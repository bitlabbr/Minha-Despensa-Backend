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

package com.bitlabbr.minhadespensa.modules.budget.service

import com.bitlabbr.minhadespensa.modules.budget.dto.MonthlyBudgetSyncDto
import com.bitlabbr.minhadespensa.modules.budget.entity.MonthlyBudgetEntity
import com.bitlabbr.minhadespensa.modules.budget.repository.MonthlyBudgetRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MonthlyBudgetService(
    private val budgetRepository: MonthlyBudgetRepository
) {

    @Transactional
    fun upsertForSync(householdId: String, dto: MonthlyBudgetSyncDto, syncVersion: Long): MonthlyBudgetEntity {
        val naturalId = "${dto.year}-${dto.month}"
        val existingOpt = budgetRepository.findByHouseholdIdAndYearAndMonth(householdId, dto.year, dto.month)

        return if (existingOpt.isPresent) {
            val existing = existingOpt.get()
            if (dto.updatedAt >= existing.updatedAt) {
                existing.budgetInCents = dto.budgetInCents
                existing.updatedAt = dto.updatedAt
                existing.serverVersion = syncVersion
                existing.isDeleted = dto.isDeleted
                budgetRepository.save(existing)
            } else {
                existing
            }
        } else {
            val newBudget = MonthlyBudgetEntity(
                householdId = householdId,
                id = naturalId,
                year = dto.year,
                month = dto.month,
                budgetInCents = dto.budgetInCents,
                updatedAt = dto.updatedAt,
                serverVersion = syncVersion,
                isDeleted = dto.isDeleted
            )
            budgetRepository.save(newBudget)
        }
    }

    @Transactional(readOnly = true)
    fun getModifiedSince(householdId: String, sinceVersion: Long): List<MonthlyBudgetEntity> {
        return budgetRepository.findByHouseholdIdAndServerVersionGreaterThanOrderByServerVersionAsc(householdId, sinceVersion)
    }
}
