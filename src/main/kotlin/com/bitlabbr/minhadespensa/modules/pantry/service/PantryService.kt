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

import com.bitlabbr.minhadespensa.common.exception.ResourceNotFoundException
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryItemRequestDto
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryItemResponseDto
import com.bitlabbr.minhadespensa.modules.pantry.entity.PantryItemEntity
import com.bitlabbr.minhadespensa.modules.pantry.repository.PantryItemRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PantryService(
    private val pantryItemRepository: PantryItemRepository
) {
    private val logger = LoggerFactory.getLogger(PantryService::class.java)

    @Transactional(readOnly = true)
    fun getAllItems(householdId: String): List<PantryItemResponseDto> {
        return pantryItemRepository.findByHouseholdIdAndIsDeletedFalse(householdId)
            .map { PantryItemResponseDto.fromEntity(it) }
    }

    @Transactional(readOnly = true)
    fun getItemById(householdId: String, id: String): PantryItemResponseDto {
        val entity = pantryItemRepository.findByHouseholdIdAndIdAndIsDeletedFalse(householdId, id)
            .orElseThrow { ResourceNotFoundException("Item de despensa com ID '$id' não encontrado.", "PantryItem", id) }
        return PantryItemResponseDto.fromEntity(entity)
    }

    @Transactional(readOnly = true)
    fun getItemsByProductId(householdId: String, productId: String): List<PantryItemResponseDto> {
        return pantryItemRepository.findByHouseholdIdAndProductIdAndIsDeletedFalse(householdId, productId)
            .map { PantryItemResponseDto.fromEntity(it) }
    }

    @Transactional
    fun saveItem(householdId: String, dto: PantryItemRequestDto, syncVersion: Long = 0L): PantryItemResponseDto {
        val targetId = dto.id ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val incomingUpdatedAt = dto.updatedAt ?: now

        val priceInCents = dto.unitPriceInCents ?: dto.unitPrice?.let { (it * 100).toLong() }

        val existingOpt = pantryItemRepository.findByHouseholdIdAndId(householdId, targetId)

        val entityToSave = if (existingOpt.isPresent) {
            val existing = existingOpt.get()
            if (incomingUpdatedAt >= existing.updatedAt) {
                existing.productId = dto.productId
                existing.quantity = dto.quantity
                existing.expirationDate = dto.expirationDate
                existing.batchNumber = dto.batchNumber
                existing.unitPriceInCents = priceInCents
                existing.updatedAt = incomingUpdatedAt
                existing.serverVersion = syncVersion
                existing.isDeleted = dto.isDeleted
                pantryItemRepository.save(existing)
            } else {
                logger.debug("LWW ignorou atualização de item $targetId: atual=${existing.updatedAt} > recebido=$incomingUpdatedAt")
                existing
            }
        } else {
            val newEntity = PantryItemEntity(
                householdId = householdId,
                id = targetId,
                productId = dto.productId,
                quantity = dto.quantity,
                expirationDate = dto.expirationDate,
                batchNumber = dto.batchNumber,
                unitPriceInCents = priceInCents,
                updatedAt = incomingUpdatedAt,
                serverVersion = syncVersion,
                isDeleted = dto.isDeleted
            )
            pantryItemRepository.save(newEntity)
        }

        return PantryItemResponseDto.fromEntity(entityToSave)
    }

    @Transactional
    fun deleteItem(householdId: String, id: String, syncVersion: Long = 0L) {
        val entity = pantryItemRepository.findByHouseholdIdAndIdAndIsDeletedFalse(householdId, id)
            .orElseThrow { ResourceNotFoundException("Item de despensa com ID '$id' não encontrado para exclusão.", "PantryItem", id) }

        entity.isDeleted = true
        entity.updatedAt = System.currentTimeMillis()
        entity.serverVersion = syncVersion
        pantryItemRepository.save(entity)
        logger.info("Item de despensa $id marcado como excluído na casa $householdId.")
    }

    @Transactional(readOnly = true)
    fun getModifiedSince(householdId: String, sinceVersion: Long): List<PantryItemEntity> {
        return pantryItemRepository.findByHouseholdIdAndServerVersionGreaterThanOrderByServerVersionAsc(householdId, sinceVersion)
    }
}

typealias DespensaService = PantryService
