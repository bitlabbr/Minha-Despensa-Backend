/*
 *   Copyright (c) 2026 Willian Santos
 *
 *   This work is licensed under the Creative Commons
 *   Attribution-NonCommercial 4.0 International License (CC BY-NC 4.0).
 *
 *   You are free to:
 *     - Share  — copy and redistribute the material in any medium or format
 *     - Adapt  — remix, transform, and build upon the material
 *
 *   Under the following terms:
 *     - Attribution    — You must give appropriate credit, provide a link to
 *                        the license, and indicate if changes were made.
 *     - NonCommercial  — You may not use the material for commercial purposes.
 *
 *   Owner rights:
 *     - Willian Santos retains all commercial rights.
 *    - The copyright holder may use, sell, sublicense, or relicense this
 *       work under different terms at any time.
 *
 *   Full license: https://creativecommons.org/licenses/by-nc/4.0/legalcode
 */

package com.bitlabbr.minhadespensa.modules.sync.service

import com.bitlabbr.minhadespensa.common.exception.ResourceNotFoundException
import com.bitlabbr.minhadespensa.modules.budget.dto.MonthlyBudgetSyncDto
import com.bitlabbr.minhadespensa.modules.budget.service.MonthlyBudgetService
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductCategorySyncDto
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductResponseDto
import com.bitlabbr.minhadespensa.modules.catalog.repository.ProductRepository
import com.bitlabbr.minhadespensa.modules.catalog.service.ProductCategoryService
import com.bitlabbr.minhadespensa.modules.catalog.service.ProductService
import com.bitlabbr.minhadespensa.modules.household.repository.HouseholdRepository
import com.bitlabbr.minhadespensa.modules.household.service.HouseholdService
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryItemResponseDto
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryMovementSyncDto
import com.bitlabbr.minhadespensa.modules.pantry.service.PantryMovementService
import com.bitlabbr.minhadespensa.modules.pantry.service.PantryService
import com.bitlabbr.minhadespensa.modules.price.dto.PriceEntryResponseDto
import com.bitlabbr.minhadespensa.modules.price.service.PriceEntryService
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingItemResponseDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingListResponseDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.service.ShoppingListService
import com.bitlabbr.minhadespensa.modules.sync.dto.SyncPullResponseDto
import com.bitlabbr.minhadespensa.modules.sync.dto.SyncPushRequestDto
import com.bitlabbr.minhadespensa.modules.sync.dto.SyncPushResponseDto
import com.bitlabbr.minhadespensa.modules.sync.dto.SyncRejectedItemDto
import com.bitlabbr.minhadespensa.modules.sync.repository.EntityIdAliasRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SyncService(
    private val householdService: HouseholdService,
    private val householdRepository: HouseholdRepository,
    private val productCategoryService: ProductCategoryService,
    private val productService: ProductService,
    private val productRepository: ProductRepository,
    private val shoppingListService: ShoppingListService,
    private val pantryService: PantryService,
    private val priceEntryService: PriceEntryService,
    private val pantryMovementService: PantryMovementService,
    private val monthlyBudgetService: MonthlyBudgetService,
    private val aliasRepository: EntityIdAliasRepository
) {
    private val logger = LoggerFactory.getLogger(SyncService::class.java)

    @Transactional
    fun push(householdId: String, request: SyncPushRequestDto): SyncPushResponseDto {
        // 1. Lock pessimista da casa e geração atômica de nova versão
        val syncVersion = householdService.nextSyncVersion(householdId)
        val serverNow = System.currentTimeMillis()

        // Clamp de segurança: no máximo 5 minutos no futuro contra relógios desajustados
        fun clamp(ts: Long?): Long = minOf(ts ?: serverNow, serverNow + 300_000L)

        val categoryIdMappings = mutableMapOf<String, String>()
        val productIdMappings = mutableMapOf<String, String>()
        val rejected = mutableListOf<SyncRejectedItemDto>()
        var count = 0

        // 2. Passo 1 - Categorias de Produtos
        request.categories.forEach { catDto ->
            val clamped = catDto.copy(updatedAt = clamp(catDto.updatedAt))
            val result = productCategoryService.upsertForSync(householdId, clamped, syncVersion)
            if (result.aliasId != null) {
                categoryIdMappings[result.aliasId] = result.canonicalId
            }
            count++
        }

        // 3. Passo 2 - Produtos do Catálogo
        request.products.forEach { prodDto ->
            val clamped = prodDto.copy(updatedAt = clamp(prodDto.updatedAt))
            val result = productService.upsertForSync(householdId, clamped, syncVersion, serverNow)
            if (result.remappedFromId != null) {
                productIdMappings[result.remappedFromId] = result.canonicalId
            }
            count++
        }

        fun resolveEffectiveProductId(rawId: String?): String? {
            if (rawId == null) return null
            return productIdMappings[rawId]
                ?: aliasRepository.findCanonicalId(householdId, "PRODUCT", rawId)
                ?: rawId
        }

        fun productExists(pId: String): Boolean {
            return productRepository.findByHouseholdIdAndId(householdId, pId).isPresent
        }

        // 4. Passo 3 - Listas de Compras
        request.shoppingLists.forEach { listDto ->
            val clamped = listDto.copy(updatedAt = clamp(listDto.updatedAt))
            shoppingListService.saveList(householdId, clamped, syncVersion)
            count++
        }

        // 5. Passo 4 - Itens das Listas de Compras
        request.shoppingItems.forEach { itemDto ->
            val clamped = itemDto.copy(updatedAt = clamp(itemDto.updatedAt))
            val effectiveProductId = resolveEffectiveProductId(clamped.productId)
            val itemToSave = if (effectiveProductId != clamped.productId) {
                clamped.copy(productId = effectiveProductId)
            } else {
                clamped
            }
            shoppingListService.saveItem(householdId, itemToSave, syncVersion)
            count++
        }

        // 6. Passo 5 - Itens da Despensa (Estoque)
        request.pantryItems.forEach { pantryDto ->
            val clamped = pantryDto.copy(updatedAt = clamp(pantryDto.updatedAt))
            val effectiveProductId = resolveEffectiveProductId(clamped.productId)
            if (effectiveProductId == null || !productExists(effectiveProductId)) {
                rejected.add(
                    SyncRejectedItemDto(
                        entityType = "PANTRY_ITEM",
                        id = clamped.id ?: "unknown",
                        reason = "Produto com ID '${clamped.productId}' não encontrado na casa $householdId"
                    )
                )
            } else {
                val pantryToSave = if (effectiveProductId != clamped.productId) {
                    clamped.copy(productId = effectiveProductId)
                } else {
                    clamped
                }
                pantryService.saveItem(householdId, pantryToSave, syncVersion)
                count++
            }
        }

        // 7. Passo 6 - Histórico de Preços
        request.priceEntries.forEach { priceDto ->
            val clamped = priceDto.copy(updatedAt = clamp(priceDto.updatedAt))
            val effectiveProductId = resolveEffectiveProductId(clamped.productId)
            if (effectiveProductId == null || !productExists(effectiveProductId)) {
                rejected.add(
                    SyncRejectedItemDto(
                        entityType = "PRICE_ENTRY",
                        id = clamped.id ?: "unknown",
                        reason = "Produto com ID '${clamped.productId}' não encontrado na casa $householdId"
                    )
                )
            } else {
                val priceToSave = if (effectiveProductId != clamped.productId) {
                    clamped.copy(productId = effectiveProductId)
                } else {
                    clamped
                }
                priceEntryService.savePrice(householdId, priceToSave, syncVersion)
                count++
            }
        }

        // 8. Passo 7 - Movimentações de Estoque (Livro-Razão)
        request.pantryMovements.forEach { movDto ->
            val clamped = movDto.copy(updatedAt = clamp(movDto.updatedAt))
            val effectiveProductId = resolveEffectiveProductId(clamped.productId)
            if (effectiveProductId == null || !productExists(effectiveProductId)) {
                rejected.add(
                    SyncRejectedItemDto(
                        entityType = "PANTRY_MOVEMENT",
                        id = clamped.id,
                        reason = "Produto com ID '${clamped.productId}' não encontrado na casa $householdId"
                    )
                )
            } else {
                val movToSave = if (effectiveProductId != clamped.productId) {
                    clamped.copy(productId = effectiveProductId)
                } else {
                    clamped
                }
                pantryMovementService.upsertForSync(householdId, movToSave, syncVersion)
                count++
            }
        }

        // 9. Passo 8 - Orçamentos Mensais
        request.monthlyBudgets.forEach { budgetDto ->
            val clamped = budgetDto.copy(updatedAt = clamp(budgetDto.updatedAt))
            monthlyBudgetService.upsertForSync(householdId, clamped, syncVersion)
            count++
        }

        val serverTimestamp = System.currentTimeMillis()
        logger.info(
            "Sync PUSH concluído com sucesso para casa $householdId: $count processados, " +
                "${productIdMappings.size} produtos remapeados, ${rejected.size} rejeitados (versão=$syncVersion)"
        )

        return SyncPushResponseDto(
            serverTimestamp = serverTimestamp,
            serverVersion = syncVersion,
            totalProcessed = count,
            productIdMappings = productIdMappings,
            categoryIdMappings = categoryIdMappings,
            rejected = rejected
        )
    }

    @Transactional(readOnly = true)
    fun pull(householdId: String, sinceVersion: Long): SyncPullResponseDto {
        val currentHousehold = householdRepository.findById(householdId)
            .orElseThrow { ResourceNotFoundException("Casa com ID '$householdId' não encontrada.", "Household", householdId) }

        val categories = productCategoryService.getModifiedSince(householdId, sinceVersion)
            .map { ProductCategorySyncDto.fromEntity(it) }
        val products = productService.getModifiedSince(householdId, sinceVersion)
            .map { ProductResponseDto.fromEntity(it) }
        val shoppingLists = shoppingListService.getListsModifiedSince(householdId, sinceVersion)
            .map { ShoppingListResponseDto.fromEntity(it) }
        val shoppingItems = shoppingListService.getItemsModifiedSince(householdId, sinceVersion)
            .map { ShoppingItemResponseDto.fromEntity(it) }
        val pantryItems = pantryService.getModifiedSince(householdId, sinceVersion)
            .map { PantryItemResponseDto.fromEntity(it) }
        val priceEntries = priceEntryService.getPricesModifiedSince(householdId, sinceVersion)
            .map { PriceEntryResponseDto.fromEntity(it) }
        val movements = pantryMovementService.getModifiedSince(householdId, sinceVersion)
            .map { PantryMovementSyncDto.fromEntity(it) }
        val budgets = monthlyBudgetService.getModifiedSince(householdId, sinceVersion)
            .map { MonthlyBudgetSyncDto.fromEntity(it) }

        val total = categories.size + products.size + shoppingLists.size + shoppingItems.size +
            pantryItems.size + priceEntries.size + movements.size + budgets.size

        logger.info(
            "Sync PULL concluído para casa $householdId: $total registros alterados desde versão $sinceVersion " +
                "(versãoAtual=${currentHousehold.syncVersion})"
        )

        return SyncPullResponseDto(
            serverTimestamp = System.currentTimeMillis(),
            serverVersion = currentHousehold.syncVersion,
            hasMore = false,
            categories = categories,
            products = products,
            shoppingLists = shoppingLists,
            shoppingItems = shoppingItems,
            pantryItems = pantryItems,
            priceEntries = priceEntries,
            pantryMovements = movements,
            monthlyBudgets = budgets
        )
    }
}
