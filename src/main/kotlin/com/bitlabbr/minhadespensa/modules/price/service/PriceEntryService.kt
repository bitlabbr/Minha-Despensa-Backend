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

package com.bitlabbr.minhadespensa.modules.price.service

import com.bitlabbr.minhadespensa.common.exception.ResourceNotFoundException
import com.bitlabbr.minhadespensa.modules.price.dto.PriceEntryRequestDto
import com.bitlabbr.minhadespensa.modules.price.dto.PriceEntryResponseDto
import com.bitlabbr.minhadespensa.modules.price.entity.PriceEntryEntity
import com.bitlabbr.minhadespensa.modules.price.repository.PriceEntryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PriceEntryService(
    private val priceEntryRepository: PriceEntryRepository
) {
    private val logger = LoggerFactory.getLogger(PriceEntryService::class.java)

    @Transactional(readOnly = true)
    fun getPricesByProductId(householdId: String, productId: String): List<PriceEntryResponseDto> {
        return priceEntryRepository.findByHouseholdIdAndProductIdAndIsDeletedFalseOrderByUpdatedAtDesc(householdId, productId)
            .map { PriceEntryResponseDto.fromEntity(it) }
    }

    @Transactional
    fun savePrice(householdId: String, dto: PriceEntryRequestDto, syncVersion: Long = 0L): PriceEntryResponseDto {
        val targetId = dto.id ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val incomingUpdatedAt = dto.updatedAt ?: now

        val existingOpt = priceEntryRepository.findByHouseholdIdAndId(householdId, targetId)

        val entityToSave = if (existingOpt.isPresent) {
            val existing = existingOpt.get()
            if (incomingUpdatedAt >= existing.updatedAt) {
                existing.productId = dto.productId
                existing.priceInCents = dto.priceInCents
                existing.storeName = dto.storeName ?: existing.storeName
                existing.updatedAt = incomingUpdatedAt
                existing.serverVersion = syncVersion
                existing.isDeleted = dto.isDeleted
                priceEntryRepository.save(existing)
            } else {
                logger.debug("LWW ignored update for price entry $targetId: current=${existing.updatedAt} > incoming=$incomingUpdatedAt")
                existing
            }
        } else {
            val newEntity = PriceEntryEntity(
                householdId = householdId,
                id = targetId,
                productId = dto.productId,
                priceInCents = dto.priceInCents,
                storeName = dto.storeName ?: "Supermercado",
                updatedAt = incomingUpdatedAt,
                serverVersion = syncVersion,
                isDeleted = dto.isDeleted
            )
            priceEntryRepository.save(newEntity)
        }

        return PriceEntryResponseDto.fromEntity(entityToSave)
    }

    @Transactional
    fun deletePrice(householdId: String, id: String, syncVersion: Long = 0L) {
        val existing = priceEntryRepository.findByHouseholdIdAndId(householdId, id)
            .orElseThrow { ResourceNotFoundException("Price entry with ID '$id' not found.", "PriceEntry", id) }

        existing.isDeleted = true
        existing.updatedAt = System.currentTimeMillis()
        existing.serverVersion = syncVersion
        priceEntryRepository.save(existing)
    }

    @Transactional(readOnly = true)
    fun getPricesModifiedSince(householdId: String, sinceVersion: Long): List<PriceEntryEntity> {
        return priceEntryRepository.findByHouseholdIdAndServerVersionGreaterThanOrderByServerVersionAsc(householdId, sinceVersion)
    }
}
