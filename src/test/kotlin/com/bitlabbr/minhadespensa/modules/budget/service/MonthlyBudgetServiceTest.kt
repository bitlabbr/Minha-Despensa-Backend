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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class MonthlyBudgetServiceTest {

    private val budgetRepository: MonthlyBudgetRepository = mock()
    private lateinit var budgetService: MonthlyBudgetService

    @BeforeEach
    fun setUp() {
        budgetService = MonthlyBudgetService(budgetRepository)
    }

    @Test
    fun `upsertForSync should create budget with natural ID YYYY-M when absent`() {
        val dto = MonthlyBudgetSyncDto(year = 2026, month = 10, budgetInCents = 150000L, updatedAt = 1000L)
        whenever(budgetRepository.findByHouseholdIdAndYearAndMonth("house-1", 2026, 10))
            .thenReturn(Optional.empty())
        whenever(budgetRepository.save(any<MonthlyBudgetEntity>())).thenAnswer { it.arguments[0] }

        val result = budgetService.upsertForSync("house-1", dto, 1L)

        assertNotNull(result)
        assertEquals("2026-10", result.id)
        assertEquals(150000L, result.budgetInCents)
        verify(budgetRepository).save(any<MonthlyBudgetEntity>())
    }

    @Test
    fun `upsertForSync should update existing budget when incoming timestamp is newer`() {
        val existing = MonthlyBudgetEntity(
            householdId = "house-1",
            id = "2026-10",
            year = 2026,
            month = 10,
            budgetInCents = 100000L,
            updatedAt = 500L
        )

        val dto = MonthlyBudgetSyncDto(year = 2026, month = 10, budgetInCents = 200000L, updatedAt = 1000L)
        whenever(budgetRepository.findByHouseholdIdAndYearAndMonth("house-1", 2026, 10))
            .thenReturn(Optional.of(existing))
        whenever(budgetRepository.save(any<MonthlyBudgetEntity>())).thenAnswer { it.arguments[0] }

        val result = budgetService.upsertForSync("house-1", dto, 2L)

        assertEquals(200000L, result.budgetInCents)
        assertEquals(1000L, result.updatedAt)
    }
}
