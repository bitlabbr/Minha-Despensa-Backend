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
import com.bitlabbr.minhadespensa.modules.pantry.entity.PantryItemEntity
import com.bitlabbr.minhadespensa.modules.pantry.repository.PantryItemRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class PantryServiceTest {

    private lateinit var pantryItemRepository: PantryItemRepository
    private lateinit var pantryService: PantryService

    private val householdId = "house-123"

    @BeforeEach
    fun setUp() {
        pantryItemRepository = mock()
        pantryService = PantryService(pantryItemRepository)
    }

    @Test
    fun `saveItem should create pantry item when it does not exist`() {
        val dto = PantryItemRequestDto(
            id = "item-1",
            productId = "prod-1",
            quantity = 2.5,
            unitPriceInCents = 550L,
            expirationDate = 1789862400000L,
            batchNumber = "LOTE-1",
            updatedAt = 1000L
        )

        whenever(pantryItemRepository.findByHouseholdIdAndId(householdId, "item-1")).thenReturn(Optional.empty())
        whenever(pantryItemRepository.save(any<PantryItemEntity>())).thenAnswer { it.arguments[0] }

        val result = pantryService.saveItem(householdId, dto, syncVersion = 5L)

        assertEquals("item-1", result.id)
        assertEquals("prod-1", result.productId)
        assertEquals(2.5, result.quantity)
        assertEquals(550L, result.unitPriceInCents)
        assertEquals(5L, result.serverVersion)
        assertFalse(result.isDeleted)
    }

    @Test
    fun `saveItem should update existing item when updatedAt is newer`() {
        val existing = PantryItemEntity(
            householdId = householdId,
            id = "item-1",
            productId = "prod-1",
            quantity = 1.0,
            updatedAt = 1000L,
            serverVersion = 3L
        )

        val dto = PantryItemRequestDto(
            id = "item-1",
            productId = "prod-1",
            quantity = 3.0,
            unitPriceInCents = 620L,
            updatedAt = 2000L
        )

        whenever(pantryItemRepository.findByHouseholdIdAndId(householdId, "item-1")).thenReturn(Optional.of(existing))
        whenever(pantryItemRepository.save(existing)).thenReturn(existing)

        val result = pantryService.saveItem(householdId, dto, syncVersion = 10L)

        assertEquals(3.0, result.quantity)
        assertEquals(620L, result.unitPriceInCents)
        assertEquals(2000L, result.updatedAt)
        assertEquals(10L, result.serverVersion)
    }

    @Test
    fun `saveItem should ignore older updates according to CRDT LWW`() {
        val existing = PantryItemEntity(
            householdId = householdId,
            id = "item-1",
            productId = "prod-1",
            quantity = 5.0,
            updatedAt = 5000L,
            serverVersion = 4L
        )

        val olderDto = PantryItemRequestDto(
            id = "item-1",
            productId = "prod-1",
            quantity = 2.0,
            updatedAt = 3000L
        )

        whenever(pantryItemRepository.findByHouseholdIdAndId(householdId, "item-1")).thenReturn(Optional.of(existing))

        val result = pantryService.saveItem(householdId, olderDto, syncVersion = 5L)

        assertEquals(5.0, result.quantity)
        assertEquals(5000L, result.updatedAt)
    }

    @Test
    fun `deleteItem should mark item as deleted`() {
        val existing = PantryItemEntity(
            householdId = householdId,
            id = "item-1",
            productId = "prod-1",
            quantity = 2.0,
            isDeleted = false
        )

        whenever(pantryItemRepository.findByHouseholdIdAndIdAndIsDeletedFalse(householdId, "item-1"))
            .thenReturn(Optional.of(existing))

        pantryService.deleteItem(householdId, "item-1", syncVersion = 12L)

        assertTrue(existing.isDeleted)
        assertEquals(12L, existing.serverVersion)
        verify(pantryItemRepository).save(existing)
    }

    @Test
    fun `getItemById should throw ResourceNotFoundException when not found`() {
        whenever(pantryItemRepository.findByHouseholdIdAndIdAndIsDeletedFalse(householdId, "unknown"))
            .thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            pantryService.getItemById(householdId, "unknown")
        }
    }
}
