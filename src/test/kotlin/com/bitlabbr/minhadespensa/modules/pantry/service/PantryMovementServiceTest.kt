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

import com.bitlabbr.minhadespensa.modules.pantry.domain.MovementType
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryMovementSyncDto
import com.bitlabbr.minhadespensa.modules.pantry.entity.PantryMovementEntity
import com.bitlabbr.minhadespensa.modules.pantry.repository.PantryMovementRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class PantryMovementServiceTest {

    private val movementRepository: PantryMovementRepository = mock()
    private lateinit var movementService: PantryMovementService

    @BeforeEach
    fun setUp() {
        movementService = PantryMovementService(movementRepository)
    }

    @Test
    fun `upsertForSync should create movement when absent`() {
        val dto = PantryMovementSyncDto(
            id = "mov-1",
            productId = "prod-1",
            type = MovementType.ENTRY_PURCHASE,
            quantity = 2.0,
            unitPriceInCents = 500L,
            totalPriceInCents = 1000L,
            updatedAt = 1000L
        )

        whenever(movementRepository.findByHouseholdIdAndId("house-1", "mov-1")).thenReturn(Optional.empty())
        whenever(movementRepository.save(any<PantryMovementEntity>())).thenAnswer { it.arguments[0] }

        val result = movementService.upsertForSync("house-1", dto, 1L)

        assertNotNull(result)
        assertEquals("mov-1", result.id)
        assertEquals(MovementType.ENTRY_PURCHASE, result.type)
        assertEquals(1000L, result.totalPriceInCents)
        verify(movementRepository).save(any<PantryMovementEntity>())
    }

    @Test
    fun `upsertForSync should update movement when incoming is newer`() {
        val existing = PantryMovementEntity(
            householdId = "house-1",
            id = "mov-1",
            productId = "prod-1",
            type = MovementType.ENTRY_MANUAL,
            quantity = 1.0,
            updatedAt = 500L
        )

        val dto = PantryMovementSyncDto(
            id = "mov-1",
            productId = "prod-1",
            type = MovementType.ENTRY_PURCHASE,
            quantity = 3.0,
            updatedAt = 1000L
        )

        whenever(movementRepository.findByHouseholdIdAndId("house-1", "mov-1")).thenReturn(Optional.of(existing))
        whenever(movementRepository.save(any<PantryMovementEntity>())).thenAnswer { it.arguments[0] }

        val result = movementService.upsertForSync("house-1", dto, 2L)

        assertEquals(MovementType.ENTRY_PURCHASE, result.type)
        assertEquals(3.0, result.quantity)
    }
}
