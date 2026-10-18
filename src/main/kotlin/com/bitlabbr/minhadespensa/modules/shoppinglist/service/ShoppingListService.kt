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
    fun getAllLists(): List<ShoppingListResponseDto> {
        val lists = shoppingListRepository.findAllByIsDeletedFalse()
        return lists.map { list ->
            val items = shoppingItemRepository.findAllByListIdAndIsDeletedFalse(list.id)
            ShoppingListResponseDto.fromEntity(list, items)
        }
    }

    @Transactional(readOnly = true)
    fun getListById(id: String): ShoppingListResponseDto {
        val list = shoppingListRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow { ResourceNotFoundException("Lista de compras com ID '$id' não encontrada.", "ShoppingList", id) }
        val items = shoppingItemRepository.findAllByListIdAndIsDeletedFalse(list.id)
        return ShoppingListResponseDto.fromEntity(list, items)
    }

    @Transactional
    fun saveList(dto: ShoppingListRequestDto): ShoppingListResponseDto {
        val targetId = dto.id ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val incomingUpdatedAt = dto.updatedAt ?: now

        val existingOpt = shoppingListRepository.findById(targetId)

        val entityToSave = if (existingOpt.isPresent) {
            val existing = existingOpt.get()
            if (incomingUpdatedAt >= existing.updatedAt) {
                existing.name = dto.name
                existing.type = dto.type
                existing.status = dto.status
                existing.budgetInCents = dto.budgetInCents
                existing.updatedAt = incomingUpdatedAt
                existing.isDeleted = false
                existing
            } else {
                logger.info("Ignorando atualização da lista $targetId devido à regra LWW")
                existing
            }
        } else {
            ShoppingListEntity(
                id = targetId,
                name = dto.name,
                type = dto.type,
                status = dto.status,
                budgetInCents = dto.budgetInCents,
                updatedAt = incomingUpdatedAt,
                isDeleted = false
            )
        }

        val saved = shoppingListRepository.save(entityToSave)
        val items = shoppingItemRepository.findAllByListIdAndIsDeletedFalse(saved.id)
        return ShoppingListResponseDto.fromEntity(saved, items)
    }

    @Transactional
    fun deleteList(id: String) {
        val list = shoppingListRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow { ResourceNotFoundException("Lista de compras com ID '$id' não encontrada para exclusão.", "ShoppingList", id) }

        val now = System.currentTimeMillis()
        list.isDeleted = true
        list.updatedAt = now
        shoppingListRepository.save(list)

        // Soft delete em cascata nos itens da lista
        val items = shoppingItemRepository.findAllByListIdAndIsDeletedFalse(id)
        items.forEach {
            it.isDeleted = true
            it.updatedAt = now
        }
        shoppingItemRepository.saveAll(items)
        logger.info("Lista de compras $id e seus ${items.size} itens foram marcados como excluídos (soft-delete).")
    }

    @Transactional
    fun saveItem(dto: ShoppingItemRequestDto): ShoppingItemResponseDto {
        val targetId = dto.id ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val incomingUpdatedAt = dto.updatedAt ?: now

        val existingOpt = shoppingItemRepository.findById(targetId)

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
                existing.isDeleted = false
                existing
            } else {
                logger.info("Ignorando atualização do item de lista $targetId devido à regra LWW")
                existing
            }
        } else {
            ShoppingItemEntity(
                id = targetId,
                listId = dto.listId,
                productId = dto.productId,
                rawText = dto.rawText,
                quantity = dto.quantity,
                priceAtTime = dto.priceAtTime,
                isChecked = dto.isChecked,
                updatedAt = incomingUpdatedAt,
                isDeleted = false
            )
        }

        val saved = shoppingItemRepository.save(entityToSave)
        return ShoppingItemResponseDto.fromEntity(saved)
    }

    @Transactional
    fun deleteItem(id: String) {
        val item = shoppingItemRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow { ResourceNotFoundException("Item de lista com ID '$id' não encontrado para exclusão.", "ShoppingItem", id) }

        item.isDeleted = true
        item.updatedAt = System.currentTimeMillis()
        shoppingItemRepository.save(item)
    }

    @Transactional(readOnly = true)
    fun getItemsByListId(listId: String): List<ShoppingItemResponseDto> {
        return shoppingItemRepository.findAllByListIdAndIsDeletedFalse(listId)
            .map { ShoppingItemResponseDto.fromEntity(it) }
    }

    @Transactional(readOnly = true)
    fun getListsModifiedSince(since: Long): List<ShoppingListEntity> {
        return shoppingListRepository.findByUpdatedAtGreaterThan(since)
    }

    @Transactional(readOnly = true)
    fun getItemsModifiedSince(since: Long): List<ShoppingItemEntity> {
        return shoppingItemRepository.findByUpdatedAtGreaterThan(since)
    }
}
