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

import com.bitlabbr.minhadespensa.common.exception.InvalidRequestException
import com.bitlabbr.minhadespensa.modules.catalog.entity.ProductEntity
import com.bitlabbr.minhadespensa.modules.catalog.entity.ProductMediaEntity
import com.bitlabbr.minhadespensa.modules.catalog.repository.ProductMediaRepository
import com.bitlabbr.minhadespensa.modules.catalog.repository.ProductRepository
import com.bitlabbr.minhadespensa.modules.sync.repository.EntityIdAliasRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class ProductMediaServiceTest {

    private val productMediaRepository: ProductMediaRepository = mock()
    private val productRepository: ProductRepository = mock()
    private val aliasRepository: EntityIdAliasRepository = mock()

    private lateinit var mediaService: ProductMediaService

    @BeforeEach
    fun setUp() {
        mediaService = ProductMediaService(productMediaRepository, productRepository, aliasRepository)
    }

    @Test
    fun `saveMedia should store image and compute sha256 when size is within 200KB`() {
        val bytes = "fake-webp-image-bytes".toByteArray()
        whenever(aliasRepository.findCanonicalId("house-1", "PRODUCT", "prod-1")).thenReturn(null)
        whenever(productMediaRepository.findByHouseholdIdAndProductId("house-1", "prod-1"))
            .thenReturn(Optional.empty())
        whenever(productMediaRepository.save(any<ProductMediaEntity>())).thenAnswer { it.arguments[0] }
        whenever(productRepository.findByHouseholdIdAndId("house-1", "prod-1")).thenReturn(Optional.empty())

        val result = mediaService.saveMedia("house-1", "prod-1", bytes, "image/webp", 1L)

        assertNotNull(result)
        assertEquals(64, result.sha256Hash.length)
        assertEquals("image/webp", result.contentType)
        verify(productMediaRepository).save(any<ProductMediaEntity>())
    }

    @Test
    fun `saveMedia should resolve canonical id if alias exists and update product serverVersion`() {
        val bytes = "fake-webp-image-bytes".toByteArray()
        val canonicalProduct = ProductEntity(
            id = "prod-canonical",
            householdId = "house-1",
            name = "Leite",
            normalizedName = "leite"
        )
        whenever(aliasRepository.findCanonicalId("house-1", "PRODUCT", "prod-alias")).thenReturn("prod-canonical")
        whenever(productMediaRepository.findByHouseholdIdAndProductId("house-1", "prod-canonical"))
            .thenReturn(Optional.empty())
        whenever(productMediaRepository.save(any<ProductMediaEntity>())).thenAnswer { it.arguments[0] }
        whenever(productRepository.findByHouseholdIdAndId("house-1", "prod-canonical"))
            .thenReturn(Optional.of(canonicalProduct))

        val result = mediaService.saveMedia("house-1", "prod-alias", bytes, "image/webp", 5L)

        assertNotNull(result)
        assertEquals("prod-canonical", result.productId)
        assertEquals(5L, canonicalProduct.serverVersion)
        assertEquals(result.sha256Hash, canonicalProduct.imageHash)
        verify(productRepository).save(canonicalProduct)
    }

    @Test
    fun `saveMedia should reject images larger than 200KB`() {
        val oversizedBytes = ByteArray(201 * 1024)

        assertThrows<InvalidRequestException> {
            mediaService.saveMedia("house-1", "prod-1", oversizedBytes, "image/webp", 1L)
        }
    }

    @Test
    fun `getMedia should resolve canonical id`() {
        val media = ProductMediaEntity(
            householdId = "house-1",
            productId = "prod-canonical",
            contentType = "image/webp",
            sha256Hash = "hash123",
            imageBytes = byteArrayOf(1, 2, 3)
        )
        whenever(aliasRepository.findCanonicalId("house-1", "PRODUCT", "prod-alias")).thenReturn("prod-canonical")
        whenever(productMediaRepository.findByHouseholdIdAndProductId("house-1", "prod-canonical"))
            .thenReturn(Optional.of(media))

        val result = mediaService.getMedia("house-1", "prod-alias")

        assertNotNull(result)
        assertEquals("prod-canonical", result?.productId)
    }

    @Test
    fun `deleteMedia should resolve canonical id, delete media and update product serverVersion`() {
        val canonicalProduct = ProductEntity(
            id = "prod-canonical",
            householdId = "house-1",
            name = "Leite",
            normalizedName = "leite",
            imageHash = "existing-hash"
        )
        whenever(aliasRepository.findCanonicalId("house-1", "PRODUCT", "prod-alias")).thenReturn("prod-canonical")
        whenever(productRepository.findByHouseholdIdAndId("house-1", "prod-canonical"))
            .thenReturn(Optional.of(canonicalProduct))

        mediaService.deleteMedia("house-1", "prod-alias", 7L)

        verify(productMediaRepository).deleteByHouseholdIdAndProductId("house-1", "prod-canonical")
        assertNull(canonicalProduct.imageHash)
        assertEquals(7L, canonicalProduct.serverVersion)
        verify(productRepository).save(canonicalProduct)
    }
}
