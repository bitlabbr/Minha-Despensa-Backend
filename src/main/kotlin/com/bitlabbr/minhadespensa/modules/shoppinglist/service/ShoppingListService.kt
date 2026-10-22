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

package com.bitlabbr.minhadespensa.modules.shoppinglist.service

import com.bitlabbr.minhadespensa.common.exception.ResourceNotFoundException
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingItemRequestDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingItemResponseDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingListRequestDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingListResponseDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.entity.ShoppingItemEntity
import com.bitlabbr.minhadespensa.modules.shoppinglist.entity.ShoppingListEntity
import com.bitlabbr.minhadespensa.modules.shoppinglist.repository.ShoppingItemRepository
import com.bitlabbr.minhadespensa.modules.shoppinglist.repository.ShoppingListRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ShoppingListService(
    private val shoppingListRepository: ShoppingListRepository,
    private val shoppingItemRepository: ShoppingItemRepository
) {
    private val logger = LoggerFactory.getLogger(ShoppingListService::class.java)

    @Transactional(readOnly = true)
    fun getAllLists(householdId: String): List<ShoppingListResponseDto> {
        val lists = shoppingListRepository.findByHouseholdIdAndIsDeletedFalse(householdId)
        return lists.map { list ->
            val items = shoppingItemRepository.findByHouseholdIdAndListIdAndIsDeletedFalse(householdId, list.id)
            ShoppingListResponseDto.fromEntity(list, items)
        }
    }

    @Transactional(readOnly = true)
    fun getListById(householdId: String, id: String): ShoppingListResponseDto {
        val list = shoppingListRepository.findByHouseholdIdAndIdAndIsDeletedFalse(householdId, id)
            .orElseThrow { ResourceNotFoundException("Lista de compras com ID '$id' não encontrada.", "ShoppingList", id) }
        val items = shoppingItemRepository.findByHouseholdIdAndListIdAndIsDeletedFalse(householdId, list.id)
        return ShoppingListResponseDto.fromEntity(list, items)
    }

    @Transactional
    fun saveList(householdId: String, dto: ShoppingListRequestDto, syncVersion: Long = 0L): ShoppingListResponseDto {
        val targetId = dto.id ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val incomingUpdatedAt = dto.updatedAt ?: now

        val existingOpt = shoppingListRepository.findByHouseholdIdAndId(householdId, targetId)

        val entityToSave = if (existingOpt.isPresent) {
            val existing = existingOpt.get()
            if (incomingUpdatedAt >= existing.updatedAt) {
                existing.name = dto.name
                existing.type = dto.type
                existing.status = dto.status
                existing.budgetInCents = dto.budgetInCents
                existing.updatedAt = incomingUpdatedAt
                existing.serverVersion = syncVersion
                existing.isDeleted = dto.isDeleted
                existing
            } else {
                logger.debug("LWW ignorou atualização de lista $targetId: atual=${existing.updatedAt} > recebido=$incomingUpdatedAt")
                existing
            }
        } else {
            ShoppingListEntity(
                householdId = householdId,
                id = targetId,
                name = dto.name,
                type = dto.type,
                status = dto.status,
                budgetInCents = dto.budgetInCents,
                updatedAt = incomingUpdatedAt,
                serverVersion = syncVersion,
                isDeleted = dto.isDeleted
            )
        }

        val saved = shoppingListRepository.save(entityToSave)

        // Cascade Tombstone: se a lista foi excluída, todos os seus itens tornam-se excluídos
        if (saved.isDeleted) {
            val items = shoppingItemRepository.findByHouseholdIdAndListId(householdId, saved.id)
            val modifiedItems = items.filter { !it.isDeleted }
            if (modifiedItems.isNotEmpty()) {
                modifiedItems.forEach { item ->
                    item.isDeleted = true
                    item.updatedAt = maxOf(item.updatedAt + 1, incomingUpdatedAt)
                    item.serverVersion = syncVersion
                }
                shoppingItemRepository.saveAll(modifiedItems)
                logger.info("Cascade tombstone propagado para ${modifiedItems.size} itens da lista ${saved.id} na casa $householdId")
            }
        }

        val activeItems = shoppingItemRepository.findByHouseholdIdAndListIdAndIsDeletedFalse(householdId, saved.id)
        return ShoppingListResponseDto.fromEntity(saved, activeItems)
    }

    @Transactional
    fun deleteList(householdId: String, id: String, syncVersion: Long = 0L) {
        val list = shoppingListRepository.findByHouseholdIdAndIdAndIsDeletedFalse(householdId, id)
            .orElseThrow { ResourceNotFoundException("Lista de compras com ID '$id' não encontrada para exclusão.", "ShoppingList", id) }

        val now = System.currentTimeMillis()
        list.isDeleted = true
        list.updatedAt = now
        list.serverVersion = syncVersion
        shoppingListRepository.save(list)

        // Soft delete em cascata nos itens da lista
        val items = shoppingItemRepository.findByHouseholdIdAndListIdAndIsDeletedFalse(householdId, id)
        items.forEach {
            it.isDeleted = true
            it.updatedAt = maxOf(it.updatedAt + 1, now)
            it.serverVersion = syncVersion
        }
        shoppingItemRepository.saveAll(items)
        logger.info("Lista de compras $id e seus ${items.size} itens foram marcados como excluídos na casa $householdId.")
    }

    @Transactional
    fun saveItem(householdId: String, dto: ShoppingItemRequestDto, syncVersion: Long = 0L): ShoppingItemResponseDto {
        val targetId = dto.id ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val incomingUpdatedAt = dto.updatedAt ?: now

        // Regra "Tombstone do pai vence o filho": se a lista pai está excluída e é mais recente, o item deve ser excluído
        val parentOpt = shoppingListRepository.findByHouseholdIdAndId(householdId, dto.listId)
        val forcedDeletedByParent = parentOpt.isPresent && parentOpt.get().isDeleted && parentOpt.get().updatedAt >= incomingUpdatedAt
        val effectiveIsDeleted = if (forcedDeletedByParent) true else dto.isDeleted

        val existingOpt = shoppingItemRepository.findByHouseholdIdAndId(householdId, targetId)

        val entityToSave = if (existingOpt.isPresent) {
            val existing = existingOpt.get()
            if (incomingUpdatedAt >= existing.updatedAt) {
                existing.listId = dto.listId
                existing.productId = dto.productId
                existing.rawText = dto.rawText
                existing.quantity = dto.quantity
                existing.priceAtTime = dto.priceAtTime
                existing.isChecked = dto.isChecked
                existing.updatedAt = incomingUpdatedAt
                existing.serverVersion = syncVersion
                existing.isDeleted = effectiveIsDeleted
                existing
            } else {
                logger.debug("LWW ignorou atualização de item $targetId: atual=${existing.updatedAt} > recebido=$incomingUpdatedAt")
                existing
            }
        } else {
            ShoppingItemEntity(
                householdId = householdId,
                id = targetId,
                listId = dto.listId,
                productId = dto.productId,
                rawText = dto.rawText,
                quantity = dto.quantity,
                priceAtTime = dto.priceAtTime,
                isChecked = dto.isChecked,
                updatedAt = incomingUpdatedAt,
                serverVersion = syncVersion,
                isDeleted = effectiveIsDeleted
            )
        }

        val saved = shoppingItemRepository.save(entityToSave)
        return ShoppingItemResponseDto.fromEntity(saved)
    }

    @Transactional
    fun deleteItem(householdId: String, id: String, syncVersion: Long = 0L) {
        val item = shoppingItemRepository.findByHouseholdIdAndIdAndIsDeletedFalse(householdId, id)
            .orElseThrow { ResourceNotFoundException("Item de lista com ID '$id' não encontrado para exclusão.", "ShoppingItem", id) }

        item.isDeleted = true
        item.updatedAt = System.currentTimeMillis()
        item.serverVersion = syncVersion
        shoppingItemRepository.save(item)
    }

    @Transactional(readOnly = true)
    fun getItemsByListId(householdId: String, listId: String): List<ShoppingItemResponseDto> {
        return shoppingItemRepository.findByHouseholdIdAndListIdAndIsDeletedFalse(householdId, listId)
            .map { ShoppingItemResponseDto.fromEntity(it) }
    }

    @Transactional(readOnly = true)
    fun getListsModifiedSince(householdId: String, sinceVersion: Long): List<ShoppingListEntity> {
        return shoppingListRepository.findByHouseholdIdAndServerVersionGreaterThanOrderByServerVersionAsc(householdId, sinceVersion)
    }

    @Transactional(readOnly = true)
    fun getItemsModifiedSince(householdId: String, sinceVersion: Long): List<ShoppingItemEntity> {
        return shoppingItemRepository.findByHouseholdIdAndServerVersionGreaterThanOrderByServerVersionAsc(householdId, sinceVersion)
    }
}
