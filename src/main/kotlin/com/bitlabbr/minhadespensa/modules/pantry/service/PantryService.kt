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

package com.bitlabbr.minhadespensa.modules.pantry.service

import com.bitlabbr.minhadespensa.common.exception.BusinessException
import com.bitlabbr.minhadespensa.common.exception.InvalidRequestException
import com.bitlabbr.minhadespensa.common.exception.ResourceNotFoundException
import com.bitlabbr.minhadespensa.modules.catalog.repository.ProductRepository
import com.bitlabbr.minhadespensa.modules.pantry.domain.PantryItemStatus
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryAggregatedResponseDto
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryItemRequestDto
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryItemResponseDto
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryOccurrenceResponseDto
import com.bitlabbr.minhadespensa.modules.pantry.dto.ProductSummaryDto
import com.bitlabbr.minhadespensa.modules.pantry.entity.PantryItemEntity
import com.bitlabbr.minhadespensa.modules.pantry.repository.PantryItemRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PantryService(
    private val pantryItemRepository: PantryItemRepository,
    private val productRepository: ProductRepository
) {
    private val logger = LoggerFactory.getLogger(PantryService::class.java)

    @Transactional(readOnly = true)
    fun getAllItems(): List<PantryItemResponseDto> {
        return pantryItemRepository.findAllByIsDeletedFalse()
            .map { PantryItemResponseDto.fromEntity(it) }
    }

    @Transactional(readOnly = true)
    fun getItemById(id: String): PantryItemResponseDto {
        val entity = pantryItemRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow { ResourceNotFoundException("Item de despensa com ID '$id' não encontrado.", "PantryItem", id) }
        return PantryItemResponseDto.fromEntity(entity)
    }

    @Transactional(readOnly = true)
    fun getItemsByProductId(productId: String): List<PantryItemResponseDto> {
        return pantryItemRepository.findAllByProductIdAndIsDeletedFalse(productId)
            .map { PantryItemResponseDto.fromEntity(it) }
    }

    @Transactional(readOnly = true)
    fun getAggregatedPantry(): List<PantryAggregatedResponseDto> {
        val activeItems = pantryItemRepository.findAllByStatusAndIsDeletedFalse(PantryItemStatus.EM_ESTOQUE)
            .filter { it.quantity > 0.0 }

        if (activeItems.isEmpty()) {
            return emptyList()
        }

        val productIds = activeItems.map { it.productId }.distinct()
        val productsMap = productRepository.findAllById(productIds)
            .filter { !it.isDeleted }
            .associateBy { it.id }

        val itemsByProduct = activeItems.groupBy { it.productId }

        return itemsByProduct.map { (productId, items) ->
            val product = productsMap[productId]
            val productSummary = if (product != null) {
                ProductSummaryDto.fromEntity(product)
            } else {
                ProductSummaryDto.fallback(productId)
            }

            val totalQuantity = items.sumOf { it.quantity }
            val occurrences = items
                .sortedWith(compareBy<PantryItemEntity, Long?>(nullsLast()) { it.expirationDate }.thenBy { it.purchaseDate })
                .map { PantryOccurrenceResponseDto.fromEntity(it) }

            PantryAggregatedResponseDto(
                produto = productSummary,
                quantidadeTotal = totalQuantity,
                ocorrencias = occurrences
            )
        }.sortedBy { it.produto.nome.lowercase() }
    }

    @Transactional(readOnly = true)
    fun getOccurrencesByProductId(productId: String): List<PantryOccurrenceResponseDto> {
        return pantryItemRepository.findAllByProductIdAndStatusAndIsDeletedFalse(productId, PantryItemStatus.EM_ESTOQUE)
            .filter { it.quantity > 0.0 }
            .sortedWith(compareBy<PantryItemEntity, Long?>(nullsLast()) { it.expirationDate }.thenBy { it.purchaseDate })
            .map { PantryOccurrenceResponseDto.fromEntity(it) }
    }

    @Transactional
    fun saveItem(dto: PantryItemRequestDto): PantryItemResponseDto {
        val targetId = dto.id ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val incomingUpdatedAt = dto.updatedAt ?: now

        val existingOpt = pantryItemRepository.findById(targetId)

        val entityToSave = if (existingOpt.isPresent) {
            val existing = existingOpt.get()
            // Regra CRDT / LWW
            if (incomingUpdatedAt >= existing.updatedAt) {
                existing.productId = dto.productId
                existing.quantity = dto.quantity
                existing.unitPrice = dto.unitPrice
                existing.purchaseDate = dto.purchaseDate
                existing.expirationDate = dto.expirationDate
                existing.purchaseLocation = dto.purchaseLocation
                existing.status = if (dto.quantity <= 0.0 && dto.status == PantryItemStatus.EM_ESTOQUE) {
                    PantryItemStatus.CONSUMIDO
                } else {
                    dto.status
                }
                existing.batchNumber = dto.batchNumber
                existing.updatedAt = incomingUpdatedAt
                existing.isDeleted = false
                existing
            } else {
                logger.info("Ignorando atualização do item de despensa $targetId devido à regra LWW (existente=${existing.updatedAt} > recebido=$incomingUpdatedAt)")
                existing
            }
        } else {
            val initialStatus = if (dto.quantity <= 0.0 && dto.status == PantryItemStatus.EM_ESTOQUE) {
                PantryItemStatus.CONSUMIDO
            } else {
                dto.status
            }
            PantryItemEntity(
                id = targetId,
                productId = dto.productId,
                quantity = dto.quantity,
                unitPrice = dto.unitPrice,
                purchaseDate = dto.purchaseDate,
                expirationDate = dto.expirationDate,
                purchaseLocation = dto.purchaseLocation,
                status = initialStatus,
                batchNumber = dto.batchNumber,
                updatedAt = incomingUpdatedAt,
                isDeleted = false
            )
        }

        val saved = pantryItemRepository.save(entityToSave)
        return PantryItemResponseDto.fromEntity(saved)
    }

    @Transactional
    fun consumeItem(id: String, quantityToConsume: Double): PantryItemResponseDto {
        if (quantityToConsume <= 0.0) {
            throw InvalidRequestException("A quantidade a ser consumida deve ser maior que zero.")
        }

        val entity = pantryItemRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow { ResourceNotFoundException("Item de despensa com ID '$id' não encontrado.", "PantryItem", id) }

        if (entity.status != PantryItemStatus.EM_ESTOQUE || entity.quantity <= 0.0) {
            throw BusinessException("Não é possível consumir um item com status '${entity.status}' ou sem estoque disponível.")
        }

        if (quantityToConsume > entity.quantity) {
            throw BusinessException("Quantidade a consumir ($quantityToConsume) é maior que o saldo em estoque (${entity.quantity}).")
        }

        val remaining = entity.quantity - quantityToConsume
        entity.quantity = if (remaining < 0.000001) 0.0 else remaining
        if (entity.quantity == 0.0) {
            entity.status = PantryItemStatus.CONSUMIDO
        }
        entity.updatedAt = System.currentTimeMillis()

        val saved = pantryItemRepository.save(entity)
        logger.info("Item $id consumido ($quantityToConsume un). Saldo restante: ${entity.quantity}, status: ${entity.status}")
        return PantryItemResponseDto.fromEntity(saved)
    }

    @Transactional
    fun discardItem(id: String): PantryItemResponseDto {
        val entity = pantryItemRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow { ResourceNotFoundException("Item de despensa com ID '$id' não encontrado.", "PantryItem", id) }

        entity.status = PantryItemStatus.DESCARTADO
        entity.quantity = 0.0
        entity.updatedAt = System.currentTimeMillis()

        val saved = pantryItemRepository.save(entity)
        logger.info("Item $id marcado como descartado.")
        return PantryItemResponseDto.fromEntity(saved)
    }

    @Transactional
    fun deleteItem(id: String) {
        val entity = pantryItemRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow { ResourceNotFoundException("Item de despensa com ID '$id' não encontrado para exclusão.", "PantryItem", id) }

        entity.isDeleted = true
        entity.updatedAt = System.currentTimeMillis()
        pantryItemRepository.save(entity)
        logger.info("Item de despensa $id marcado como excluído (soft-delete).")
    }

    @Transactional(readOnly = true)
    fun getModifiedSince(since: Long): List<PantryItemEntity> {
        return pantryItemRepository.findByUpdatedAtGreaterThan(since)
    }
}

typealias DespensaService = PantryService
