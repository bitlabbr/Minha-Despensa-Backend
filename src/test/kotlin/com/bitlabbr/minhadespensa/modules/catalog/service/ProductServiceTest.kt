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

import com.bitlabbr.minhadespensa.common.exception.ConflictException
import com.bitlabbr.minhadespensa.common.exception.ResourceNotFoundException
import com.bitlabbr.minhadespensa.modules.catalog.domain.MeasureUnit
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductRequestDto
import com.bitlabbr.minhadespensa.modules.catalog.entity.ProductEntity
import com.bitlabbr.minhadespensa.modules.catalog.repository.ProductRepository
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

class ProductServiceTest {

    private lateinit var productRepository: ProductRepository
    private lateinit var productService: ProductService

    @BeforeEach
    fun setUp() {
        productRepository = mock()
        productService = ProductService(productRepository)
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

        whenever(productRepository.existsByEanAndIdNotAndIsDeletedFalse("7891000100103", "prod-1"))
            .thenReturn(false)
        whenever(productRepository.findById("prod-1")).thenReturn(Optional.empty())
        whenever(productRepository.save(any<ProductEntity>())).thenAnswer { it.arguments[0] }

        val result = productService.saveProduct(dto)

        assertEquals("prod-1", result.id)
        assertEquals("Café Tradicional 500g", result.name)
        assertEquals("Pilão", result.brand)
        assertFalse(result.isDeleted)
        verify(productRepository).save(any<ProductEntity>())
    }

    @Test
    fun `saveProduct should update product when incoming updatedAt is newer`() {
        val existing = ProductEntity(
            id = "prod-1",
            name = "Café Antigo",
            updatedAt = 1000L
        )

        val updateDto = ProductRequestDto(
            id = "prod-1",
            name = "Café Atualizado",
            updatedAt = 2000L
        )

        whenever(productRepository.findById("prod-1")).thenReturn(Optional.of(existing))
        whenever(productRepository.save(existing)).thenReturn(existing)

        val result = productService.saveProduct(updateDto)

        assertEquals("Café Atualizado", result.name)
        assertEquals(2000L, result.updatedAt)
    }

    @Test
    fun `saveProduct should ignore older updates according to CRDT LWW`() {
        val existing = ProductEntity(
            id = "prod-1",
            name = "Versão Mais Nova",
            updatedAt = 5000L
        )

        val olderDto = ProductRequestDto(
            id = "prod-1",
            name = "Versão Mais Velha",
            updatedAt = 3000L
        )

        whenever(productRepository.findById("prod-1")).thenReturn(Optional.of(existing))
        whenever(productRepository.save(existing)).thenReturn(existing)

        val result = productService.saveProduct(olderDto)

        // Deve manter o nome original e o timestamp mais recente
        assertEquals("Versão Mais Nova", result.name)
        assertEquals(5000L, result.updatedAt)
    }

    @Test
    fun `saveProduct should throw ConflictException if duplicate EAN exists`() {
        val dto = ProductRequestDto(
            id = "prod-2",
            ean = "7891000100103",
            name = "Outro Café"
        )

        whenever(productRepository.existsByEanAndIdNotAndIsDeletedFalse("7891000100103", "prod-2"))
            .thenReturn(true)

        assertThrows<ConflictException> {
            productService.saveProduct(dto)
        }
    }

    @Test
    fun `getProductById should throw ResourceNotFoundException when product does not exist`() {
        whenever(productRepository.findByIdAndIsDeletedFalse("non-existent"))
            .thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            productService.getProductById("non-existent")
        }
    }

    @Test
    fun `deleteProduct should mark product as deleted`() {
        val existing = ProductEntity(
            id = "prod-1",
            name = "Arroz",
            isDeleted = false
        )

        whenever(productRepository.findByIdAndIsDeletedFalse("prod-1"))
            .thenReturn(Optional.of(existing))

        productService.deleteProduct("prod-1")

        assertTrue(existing.isDeleted)
        verify(productRepository).save(existing)
    }
}
