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

package com.bitlabbr.minhadespensa.modules.budget.repository

import com.bitlabbr.minhadespensa.common.entity.HouseholdEntityId
import com.bitlabbr.minhadespensa.modules.budget.entity.MonthlyBudgetEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface MonthlyBudgetRepository : JpaRepository<MonthlyBudgetEntity, HouseholdEntityId> {
    fun findByHouseholdIdAndId(householdId: String, id: String): Optional<MonthlyBudgetEntity>
    fun findByHouseholdIdAndYearAndMonth(householdId: String, year: Int, month: Int): Optional<MonthlyBudgetEntity>
    fun findByHouseholdIdAndServerVersionGreaterThanOrderByServerVersionAsc(householdId: String, serverVersion: Long): List<MonthlyBudgetEntity>
}
