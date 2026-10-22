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

package com.bitlabbr.minhadespensa.modules.catalog.service

import com.bitlabbr.minhadespensa.common.exception.ResourceNotFoundException
import com.bitlabbr.minhadespensa.common.util.DeterministicIdUtils
import com.bitlabbr.minhadespensa.modules.catalog.domain.MeasureUnit
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductRequestDto
import com.bitlabbr.minhadespensa.modules.catalog.entity.ProductEntity
import com.bitlabbr.minhadespensa.modules.catalog.repository.ProductRepository
import com.bitlabbr.minhadespensa.modules.sync.entity.EntityIdAliasEntity
import com.bitlabbr.minhadespensa.modules.sync.repository.EntityIdAliasRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class ProductServiceTest {

    private lateinit var productRepository: ProductRepository
    private lateinit var aliasRepository: EntityIdAliasRepository
    private lateinit var productService: ProductService

    private val householdId = "house-123"

    @BeforeEach
    fun setUp() {
        productRepository = mock()
        aliasRepository = mock()
        productService = ProductService(productRepository, aliasRepository)
    }

    @Test
    fun `saveProduct should create product when it does not exist`() {
        val dto = ProductRequestDto(
            id = "prod-1",
            ean = "7891000100103",
            name = "Café Tradicional 500g",
            category = "Mercearia",
            brand = "Pilão",
            measureUnit = MeasureUnit.PACKAGE,
            netWeight = 500.0,
            updatedAt = 1000L
        )

        whenever(aliasRepository.findCanonicalId(householdId, "PRODUCT", "prod-1")).thenReturn(null)
        whenever(productRepository.findByHouseholdIdAndId(householdId, "prod-1")).thenReturn(Optional.empty())
        whenever(productRepository.findByHouseholdIdAndEan(householdId, "7891000100103")).thenReturn(Optional.empty())
        whenever(productRepository.save(any<ProductEntity>())).thenAnswer { it.arguments[0] }

        val result = productService.saveProduct(householdId, dto, syncVersion = 10L)

        assertEquals("prod-1", result.id)
        assertEquals("Café Tradicional 500g", result.name)
        assertEquals("Pilão", result.brand)
        assertEquals(10L, result.serverVersion)
        assertFalse(result.isDeleted)
        verify(productRepository).save(any<ProductEntity>())
    }

    @Test
    fun `saveProduct should update product when incoming updatedAt is newer`() {
        val existing = ProductEntity(
            householdId = householdId,
            id = "prod-1",
            name = "Café Antigo",
            updatedAt = 1000L,
            serverVersion = 5L
        )

        val updateDto = ProductRequestDto(
            id = "prod-1",
            name = "Café Atualizado",
            updatedAt = 2000L
        )

        whenever(aliasRepository.findCanonicalId(householdId, "PRODUCT", "prod-1")).thenReturn(null)
        whenever(productRepository.findByHouseholdIdAndId(householdId, "prod-1")).thenReturn(Optional.of(existing))
        whenever(productRepository.save(existing)).thenReturn(existing)

        val result = productService.saveProduct(householdId, updateDto, syncVersion = 15L)

        assertEquals("Café Atualizado", result.name)
        assertEquals(2000L, result.updatedAt)
        assertEquals(15L, result.serverVersion)
    }

    @Test
    fun `saveProduct should ignore older updates according to CRDT LWW`() {
        val existing = ProductEntity(
            householdId = householdId,
            id = "prod-1",
            name = "Versão Mais Nova",
            updatedAt = 5000L,
            serverVersion = 8L
        )

        val olderDto = ProductRequestDto(
            id = "prod-1",
            name = "Versão Mais Velha",
            updatedAt = 3000L
        )

        whenever(aliasRepository.findCanonicalId(householdId, "PRODUCT", "prod-1")).thenReturn(null)
        whenever(productRepository.findByHouseholdIdAndId(householdId, "prod-1")).thenReturn(Optional.of(existing))

        val result = productService.saveProduct(householdId, olderDto, syncVersion = 9L)

        // Deve manter o nome original e o timestamp mais recente
        assertEquals("Versão Mais Nova", result.name)
        assertEquals(5000L, result.updatedAt)
    }

    @Test
    fun `upsertForSync should deduplicate product by EAN and record alias`() {
        val canonical = ProductEntity(
            householdId = householdId,
            id = "canonical-prod-id",
            ean = "7891000100103",
            name = "Arroz Tipo 1 Camil 5kg",
            updatedAt = 2000L,
            serverVersion = 4L
        )

        val duplicateDto = ProductRequestDto(
            id = "device-prod-id",
            ean = "7891000100103",
            name = "Arroz Camil 5kg",
            updatedAt = 3000L
        )

        whenever(aliasRepository.findCanonicalId(householdId, "PRODUCT", "device-prod-id")).thenReturn(null)
        whenever(productRepository.findByHouseholdIdAndId(householdId, "device-prod-id")).thenReturn(Optional.empty())
        whenever(productRepository.findByHouseholdIdAndEan(householdId, "7891000100103")).thenReturn(Optional.of(canonical))
        whenever(productRepository.save(canonical)).thenReturn(canonical)

        val result = productService.upsertForSync(householdId, duplicateDto, syncVersion = 10L)

        assertEquals("canonical-prod-id", result.canonicalId)
        assertEquals("device-prod-id", result.remappedFromId)
        assertEquals("Arroz Camil 5kg", result.entity.name)
        assertEquals(10L, result.entity.serverVersion)

        verify(aliasRepository).save(any<EntityIdAliasEntity>())
    }

    @Test
    fun `upsertForSync should deduplicate product without EAN by normalizedName and measureUnit`() {
        val normalizedName = DeterministicIdUtils.normalizeText("Banana Prata")
        val canonical = ProductEntity(
            householdId = householdId,
            id = "canonical-banana",
            ean = null,
            name = "Banana Prata",
            normalizedName = normalizedName,
            measureUnit = MeasureUnit.KILOGRAM,
            updatedAt = 1000L
        )

        val duplicateDto = ProductRequestDto(
            id = "device-banana-id",
            ean = null,
            name = "banana  prata ",
            measureUnit = MeasureUnit.KILOGRAM,
            updatedAt = 1500L
        )

        whenever(aliasRepository.findCanonicalId(householdId, "PRODUCT", "device-banana-id")).thenReturn(null)
        whenever(productRepository.findByHouseholdIdAndId(householdId, "device-banana-id")).thenReturn(Optional.empty())
        whenever(
            productRepository.findByHouseholdIdAndNormalizedNameAndMeasureUnitAndEanIsNull(
                householdId,
                normalizedName,
                MeasureUnit.KILOGRAM
            )
        ).thenReturn(Optional.of(canonical))
        whenever(productRepository.save(canonical)).thenReturn(canonical)

        val result = productService.upsertForSync(householdId, duplicateDto, syncVersion = 20L)

        assertEquals("canonical-banana", result.canonicalId)
        assertEquals("device-banana-id", result.remappedFromId)
        verify(aliasRepository).save(any<EntityIdAliasEntity>())
    }

    @Test
    fun `getProductById should throw ResourceNotFoundException when product does not exist`() {
        whenever(productRepository.findByHouseholdIdAndIdAndIsDeletedFalse(householdId, "non-existent"))
            .thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            productService.getProductById(householdId, "non-existent")
        }
    }

    @Test
    fun `deleteProduct should mark product as deleted and set serverVersion`() {
        val existing = ProductEntity(
            householdId = householdId,
            id = "prod-1",
            name = "Arroz",
            isDeleted = false
        )

        whenever(productRepository.findByHouseholdIdAndIdAndIsDeletedFalse(householdId, "prod-1"))
            .thenReturn(Optional.of(existing))

        productService.deleteProduct(householdId, "prod-1", syncVersion = 30L)

        assertTrue(existing.isDeleted)
        assertEquals(30L, existing.serverVersion)
        verify(productRepository).save(existing)
    }
}
