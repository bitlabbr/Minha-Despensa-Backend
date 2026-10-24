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

import com.bitlabbr.minhadespensa.modules.price.dto.PriceEntryRequestDto
import com.bitlabbr.minhadespensa.modules.price.entity.PriceEntryEntity
import com.bitlabbr.minhadespensa.modules.price.repository.PriceEntryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class PriceEntryServiceTest {

    private lateinit var priceEntryRepository: PriceEntryRepository
    private lateinit var priceEntryService: PriceEntryService

    private val householdId = "house-123"

    @BeforeEach
    fun setUp() {
        priceEntryRepository = mock()
        priceEntryService = PriceEntryService(priceEntryRepository)
    }

    @Test
    fun `savePrice should create new price entry when it does not exist`() {
        val dto = PriceEntryRequestDto(
            id = "price-1",
            productId = "prod-1",
            priceInCents = 1599L,
            storeName = "Carrefour",
            updatedAt = 1000L
        )

        whenever(priceEntryRepository.findByHouseholdIdAndId(householdId, "price-1")).thenReturn(Optional.empty())
        whenever(priceEntryRepository.save(any<PriceEntryEntity>())).thenAnswer { it.arguments[0] }

        val result = priceEntryService.savePrice(householdId, dto, syncVersion = 5L)

        assertEquals("price-1", result.id)
        assertEquals("prod-1", result.productId)
        assertEquals(1599L, result.priceInCents)
        assertEquals("Carrefour", result.storeName)
        assertEquals(5L, result.serverVersion)
        assertFalse(result.isDeleted)
    }

    @Test
    fun `savePrice should update existing entry when updatedAt is newer`() {
        val existing = PriceEntryEntity(
            householdId = householdId,
            id = "price-1",
            productId = "prod-1",
            priceInCents = 1200L,
            storeName = "Pão de Açúcar",
            updatedAt = 1000L,
            serverVersion = 2L
        )

        val dto = PriceEntryRequestDto(
            id = "price-1",
            productId = "prod-1",
            priceInCents = 1350L,
            storeName = "Pão de Açúcar",
            updatedAt = 2000L
        )

        whenever(priceEntryRepository.findByHouseholdIdAndId(householdId, "price-1")).thenReturn(Optional.of(existing))
        whenever(priceEntryRepository.save(existing)).thenReturn(existing)

        val result = priceEntryService.savePrice(householdId, dto, syncVersion = 10L)

        assertEquals(1350L, result.priceInCents)
        assertEquals(2000L, result.updatedAt)
        assertEquals(10L, result.serverVersion)
    }

    @Test
    fun `savePrice should ignore older update according to CRDT LWW`() {
        val existing = PriceEntryEntity(
            householdId = householdId,
            id = "price-1",
            productId = "prod-1",
            priceInCents = 1500L,
            updatedAt = 5000L
        )

        val olderDto = PriceEntryRequestDto(
            id = "price-1",
            productId = "prod-1",
            priceInCents = 1100L,
            updatedAt = 3000L
        )

        whenever(priceEntryRepository.findByHouseholdIdAndId(householdId, "price-1")).thenReturn(Optional.of(existing))

        val result = priceEntryService.savePrice(householdId, olderDto, syncVersion = 12L)

        assertEquals(1500L, result.priceInCents)
        assertEquals(5000L, result.updatedAt)
    }

    @Test
    fun `deletePrice should mark entry as deleted`() {
        val existing = PriceEntryEntity(
            householdId = householdId,
            id = "price-1",
            productId = "prod-1",
            priceInCents = 1500L,
            isDeleted = false
        )

        whenever(priceEntryRepository.findByHouseholdIdAndId(householdId, "price-1"))
            .thenReturn(Optional.of(existing))

        priceEntryService.deletePrice(householdId, "price-1", syncVersion = 15L)

        assertTrue(existing.isDeleted)
        assertEquals(15L, existing.serverVersion)
        verify(priceEntryRepository).save(existing)
    }
}
