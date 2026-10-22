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

import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryMovementSyncDto
import com.bitlabbr.minhadespensa.modules.pantry.entity.PantryMovementEntity
import com.bitlabbr.minhadespensa.modules.pantry.repository.PantryMovementRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PantryMovementService(
    private val movementRepository: PantryMovementRepository
) {

    @Transactional
    fun upsertForSync(householdId: String, dto: PantryMovementSyncDto, syncVersion: Long): PantryMovementEntity {
        val existingOpt = movementRepository.findByHouseholdIdAndId(householdId, dto.id)

        return if (existingOpt.isPresent) {
            val existing = existingOpt.get()
            if (dto.updatedAt >= existing.updatedAt) {
                existing.pantryItemId = dto.pantryItemId
                existing.productId = dto.productId
                existing.type = dto.type
                existing.quantity = dto.quantity
                existing.unitPriceInCents = dto.unitPriceInCents
                existing.totalPriceInCents = dto.totalPriceInCents
                existing.storeOrReason = dto.storeOrReason
                existing.updatedAt = dto.updatedAt
                existing.serverVersion = syncVersion
                existing.isDeleted = dto.isDeleted
                movementRepository.save(existing)
            } else {
                existing
            }
        } else {
            val newMovement = PantryMovementEntity(
                householdId = householdId,
                id = dto.id,
                pantryItemId = dto.pantryItemId,
                productId = dto.productId,
                type = dto.type,
                quantity = dto.quantity,
                unitPriceInCents = dto.unitPriceInCents,
                totalPriceInCents = dto.totalPriceInCents,
                storeOrReason = dto.storeOrReason,
                updatedAt = dto.updatedAt,
                serverVersion = syncVersion,
                isDeleted = dto.isDeleted
            )
            movementRepository.save(newMovement)
        }
    }

    @Transactional(readOnly = true)
    fun getModifiedSince(householdId: String, sinceVersion: Long): List<PantryMovementEntity> {
        return movementRepository.findByHouseholdIdAndServerVersionGreaterThanOrderByServerVersionAsc(householdId, sinceVersion)
    }
}
