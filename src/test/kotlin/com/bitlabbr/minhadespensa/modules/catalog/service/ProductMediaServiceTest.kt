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
import com.bitlabbr.minhadespensa.modules.catalog.entity.ProductMediaEntity
import com.bitlabbr.minhadespensa.modules.catalog.repository.ProductMediaRepository
import com.bitlabbr.minhadespensa.modules.catalog.repository.ProductRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class ProductMediaServiceTest {

    private val productMediaRepository: ProductMediaRepository = mock()
    private val productRepository: ProductRepository = mock()

    private lateinit var mediaService: ProductMediaService

    @BeforeEach
    fun setUp() {
        mediaService = ProductMediaService(productMediaRepository, productRepository)
    }

    @Test
    fun `saveMedia should store image and compute sha256 when size is within 200KB`() {
        val bytes = "fake-webp-image-bytes".toByteArray()
        whenever(productMediaRepository.findByHouseholdIdAndProductId("house-1", "prod-1"))
            .thenReturn(Optional.empty())
        whenever(productMediaRepository.save(any<ProductMediaEntity>())).thenAnswer { it.arguments[0] }
        whenever(productRepository.findById("prod-1")).thenReturn(Optional.empty())

        val result = mediaService.saveMedia("house-1", "prod-1", bytes, "image/webp", 1L)

        assertNotNull(result)
        assertEquals(64, result.sha256Hash.length)
        assertEquals("image/webp", result.contentType)
        verify(productMediaRepository).save(any<ProductMediaEntity>())
    }

    @Test
    fun `saveMedia should reject images larger than 200KB`() {
        val oversizedBytes = ByteArray(201 * 1024)

        assertThrows<InvalidRequestException> {
            mediaService.saveMedia("house-1", "prod-1", oversizedBytes, "image/webp", 1L)
        }
    }
}
