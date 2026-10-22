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

package com.bitlabbr.minhadespensa.modules.budget.entity

import com.bitlabbr.minhadespensa.common.entity.HouseholdEntityId
import com.bitlabbr.minhadespensa.common.entity.HouseholdScopedSyncEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.IdClass
import jakarta.persistence.Table

@Entity
@Table(name = "monthly_budgets")
@IdClass(HouseholdEntityId::class)
class MonthlyBudgetEntity(
    householdId: String = "",
    id: String = "",

    @Column(name = "year", nullable = false)
    var year: Int = 0,

    @Column(name = "month", nullable = false)
    var month: Int = 0,

    @Column(name = "budget_in_cents", nullable = false)
    var budgetInCents: Long = 0L,

    updatedAt: Long = System.currentTimeMillis(),
    serverVersion: Long = 0L,
    isDeleted: Boolean = false
) : HouseholdScopedSyncEntity(
    householdId = householdId,
    id = id,
    updatedAt = updatedAt,
    serverVersion = serverVersion,
    isDeleted = isDeleted
)
