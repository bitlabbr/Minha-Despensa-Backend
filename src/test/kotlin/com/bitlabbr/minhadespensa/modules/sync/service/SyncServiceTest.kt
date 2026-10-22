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

import com.bitlabbr.minhadespensa.modules.budget.dto.MonthlyBudgetSyncDto
import com.bitlabbr.minhadespensa.modules.budget.entity.MonthlyBudgetEntity
import com.bitlabbr.minhadespensa.modules.budget.service.MonthlyBudgetService
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductCategorySyncDto
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductCategoryUpsertResult
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductRequestDto
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductUpsertResult
import com.bitlabbr.minhadespensa.modules.catalog.entity.ProductCategoryEntity
import com.bitlabbr.minhadespensa.modules.catalog.entity.ProductEntity
import com.bitlabbr.minhadespensa.modules.catalog.repository.ProductRepository
import com.bitlabbr.minhadespensa.modules.catalog.service.ProductCategoryService
import com.bitlabbr.minhadespensa.modules.catalog.service.ProductService
import com.bitlabbr.minhadespensa.modules.household.entity.HouseholdEntity
import com.bitlabbr.minhadespensa.modules.household.repository.HouseholdRepository
import com.bitlabbr.minhadespensa.modules.household.service.HouseholdService
import com.bitlabbr.minhadespensa.modules.pantry.domain.MovementType
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryItemRequestDto
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryMovementSyncDto
import com.bitlabbr.minhadespensa.modules.pantry.entity.PantryItemEntity
import com.bitlabbr.minhadespensa.modules.pantry.entity.PantryMovementEntity
import com.bitlabbr.minhadespensa.modules.pantry.service.PantryMovementService
import com.bitlabbr.minhadespensa.modules.pantry.service.PantryService
import com.bitlabbr.minhadespensa.modules.price.dto.PriceEntryRequestDto
import com.bitlabbr.minhadespensa.modules.price.entity.PriceEntryEntity
import com.bitlabbr.minhadespensa.modules.price.service.PriceEntryService
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingItemRequestDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingListRequestDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.entity.ShoppingItemEntity
import com.bitlabbr.minhadespensa.modules.shoppinglist.entity.ShoppingListEntity
import com.bitlabbr.minhadespensa.modules.shoppinglist.service.ShoppingListService
import com.bitlabbr.minhadespensa.modules.sync.dto.SyncPushRequestDto
import com.bitlabbr.minhadespensa.modules.sync.repository.EntityIdAliasRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class SyncServiceTest {

    private lateinit var householdService: HouseholdService
    private lateinit var householdRepository: HouseholdRepository
    private lateinit var productCategoryService: ProductCategoryService
    private lateinit var productService: ProductService
    private lateinit var productRepository: ProductRepository
    private lateinit var shoppingListService: ShoppingListService
    private lateinit var pantryService: PantryService
    private lateinit var priceEntryService: PriceEntryService
    private lateinit var pantryMovementService: PantryMovementService
    private lateinit var monthlyBudgetService: MonthlyBudgetService
    private lateinit var aliasRepository: EntityIdAliasRepository

    private lateinit var syncService: SyncService

    private val householdId = "house-test-123"

    @BeforeEach
    fun setUp() {
        householdService = mock()
        householdRepository = mock()
        productCategoryService = mock()
        productService = mock()
        productRepository = mock()
        shoppingListService = mock()
        pantryService = mock()
        priceEntryService = mock()
        pantryMovementService = mock()
        monthlyBudgetService = mock()
        aliasRepository = mock()

        syncService = SyncService(
            householdService = householdService,
            householdRepository = householdRepository,
            productCategoryService = productCategoryService,
            productService = productService,
            productRepository = productRepository,
            shoppingListService = shoppingListService,
            pantryService = pantryService,
            priceEntryService = priceEntryService,
            pantryMovementService = pantryMovementService,
            monthlyBudgetService = monthlyBudgetService,
            aliasRepository = aliasRepository
        )
    }

    @Test
    fun `push should acquire household syncVersion and process all 8 collections with FK rewriting`() {
        whenever(householdService.nextSyncVersion(householdId)).thenReturn(42L)

        // Mock categoria
        val catEntity = ProductCategoryEntity(householdId = householdId, id = "cat-canonical", name = "Mercearia")
        whenever(productCategoryService.upsertForSync(eq(householdId), any(), eq(42L)))
            .thenReturn(ProductCategoryUpsertResult(entity = catEntity, aliasId = "cat-alias", canonicalId = "cat-canonical"))

        // Mock produto com remapeamento
        val prodEntity = ProductEntity(householdId = householdId, id = "prod-canonical", name = "Arroz")
        whenever(productService.upsertForSync(eq(householdId), any(), eq(42L), any()))
            .thenReturn(ProductUpsertResult(entity = prodEntity, canonicalId = "prod-canonical", remappedFromId = "prod-old"))

        // Mock verificação de existência do produto para filhos
        whenever(productRepository.findByHouseholdIdAndId(householdId, "prod-canonical"))
            .thenReturn(Optional.of(prodEntity))

        val request = SyncPushRequestDto(
            categories = listOf(ProductCategorySyncDto(id = "cat-alias", name = "Mercearia")),
            products = listOf(ProductRequestDto(id = "prod-old", name = "Arroz")),
            shoppingLists = listOf(ShoppingListRequestDto(id = "list-1", name = "Feira")),
            shoppingItems = listOf(ShoppingItemRequestDto(id = "item-1", listId = "list-1", productId = "prod-old")),
            pantryItems = listOf(PantryItemRequestDto(id = "pantry-1", productId = "prod-old", quantity = 2.0)),
            priceEntries = listOf(PriceEntryRequestDto(id = "price-1", productId = "prod-old", priceInCents = 1500L)),
            pantryMovements = listOf(
                PantryMovementSyncDto(
                    id = "mov-1",
                    productId = "prod-old",
                    type = MovementType.ENTRY_PURCHASE,
                    quantity = 2.0,
                    unitPriceInCents = 1500L,
                    totalPriceInCents = 3000L
                )
            ),
            monthlyBudgets = listOf(MonthlyBudgetSyncDto(id = "2026-10", year = 2026, month = 10, budgetInCents = 50000L))
        )

        val response = syncService.push(householdId, request)

        assertEquals(42L, response.serverVersion)
        assertEquals(8, response.totalProcessed)
        assertEquals("cat-canonical", response.categoryIdMappings["cat-alias"])
        assertEquals("prod-canonical", response.productIdMappings["prod-old"])
        assertTrue(response.rejected.isEmpty())

        verify(productCategoryService).upsertForSync(eq(householdId), any(), eq(42L))
        verify(productService).upsertForSync(eq(householdId), any(), eq(42L), any())
        verify(shoppingListService).saveList(eq(householdId), any(), eq(42L))
        verify(shoppingListService).saveItem(eq(householdId), any(), eq(42L))
        verify(pantryService).saveItem(eq(householdId), any(), eq(42L))
        verify(priceEntryService).savePrice(eq(householdId), any(), eq(42L))
        verify(pantryMovementService).upsertForSync(eq(householdId), any(), eq(42L))
        verify(monthlyBudgetService).upsertForSync(eq(householdId), any(), eq(42L))
    }

    @Test
    fun `push should record rejected items if referenced product does not exist without failing batch`() {
        whenever(householdService.nextSyncVersion(householdId)).thenReturn(10L)
        whenever(aliasRepository.findCanonicalId(householdId, "PRODUCT", "missing-prod")).thenReturn(null)
        whenever(productRepository.findByHouseholdIdAndId(householdId, "missing-prod")).thenReturn(Optional.empty())

        val request = SyncPushRequestDto(
            pantryItems = listOf(
                PantryItemRequestDto(id = "pantry-ghost", productId = "missing-prod", quantity = 1.0)
            )
        )

        val response = syncService.push(householdId, request)

        assertEquals(0, response.totalProcessed)
        assertEquals(1, response.rejected.size)
        assertEquals("PANTRY_ITEM", response.rejected[0].entityType)
        assertEquals("pantry-ghost", response.rejected[0].id)
    }

    @Test
    fun `pull should return modified entities across all 8 tables ordered by serverVersion`() {
        val household = HouseholdEntity(id = householdId, name = "Casa Principal", syncVersion = 100L)
        whenever(householdRepository.findById(householdId)).thenReturn(Optional.of(household))

        val product = ProductEntity(householdId = householdId, id = "prod-1", name = "Leite", serverVersion = 15L)
        whenever(productCategoryService.getModifiedSince(householdId, 10L)).thenReturn(emptyList())
        whenever(productService.getModifiedSince(householdId, 10L)).thenReturn(listOf(product))
        whenever(shoppingListService.getListsModifiedSince(householdId, 10L)).thenReturn(emptyList())
        whenever(shoppingListService.getItemsModifiedSince(householdId, 10L)).thenReturn(emptyList())
        whenever(pantryService.getModifiedSince(householdId, 10L)).thenReturn(emptyList())
        whenever(priceEntryService.getPricesModifiedSince(householdId, 10L)).thenReturn(emptyList())
        whenever(pantryMovementService.getModifiedSince(householdId, 10L)).thenReturn(emptyList())
        whenever(monthlyBudgetService.getModifiedSince(householdId, 10L)).thenReturn(emptyList())

        val result = syncService.pull(householdId, sinceVersion = 10L)

        assertEquals(100L, result.serverVersion)
        assertEquals(1, result.products.size)
        assertEquals("prod-1", result.products[0].id)
        assertEquals(15L, result.products[0].serverVersion)
    }
}
