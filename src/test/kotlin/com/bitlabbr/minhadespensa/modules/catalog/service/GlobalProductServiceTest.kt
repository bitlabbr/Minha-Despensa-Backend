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
import com.bitlabbr.minhadespensa.common.exception.ValidationException
import com.bitlabbr.minhadespensa.common.util.DeterministicIdUtils
import com.bitlabbr.minhadespensa.modules.catalog.domain.MeasureUnit
import com.bitlabbr.minhadespensa.modules.catalog.entity.GlobalProductEntity
import com.bitlabbr.minhadespensa.modules.catalog.repository.GlobalProductRepository
import com.bitlabbr.minhadespensa.modules.invoice.dto.InvoiceItemResponseDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest
import java.util.Optional

class GlobalProductServiceTest {

    private lateinit var globalProductRepository: GlobalProductRepository
    private lateinit var globalProductService: GlobalProductService

    @BeforeEach
    fun setUp() {
        globalProductRepository = mock()
        globalProductService = GlobalProductService(globalProductRepository)
    }

    @Test
    fun `getProductByEan should return DTO when product exists`() {
        val entity = GlobalProductEntity(
            id = "global-1",
            ean = "7891000100103",
            name = "Leite Condensado Moça",
            normalizedName = "leite condensado moca",
            brand = "Nestlé",
            category = "Laticínios",
            measureUnit = MeasureUnit.GRAM,
            netWeight = 395.0,
            source = "USER_SYNC",
            occurrenceCount = 5L
        )
        whenever(globalProductRepository.findByEan("7891000100103")).thenReturn(Optional.of(entity))

        val result = globalProductService.getProductByEan(" 7891000100103 ")

        assertEquals("global-1", result.id)
        assertEquals("7891000100103", result.ean)
        assertEquals("Leite Condensado Moça", result.name)
        assertEquals("Nestlé", result.brand)
        assertEquals("Laticínios", result.category)
        assertEquals(MeasureUnit.GRAM, result.measureUnit)
        assertEquals(395.0, result.netWeight)
        assertEquals(5L, result.occurrenceCount)
    }

    @Test
    fun `getProductByEan should throw ResourceNotFoundException when product does not exist`() {
        whenever(globalProductRepository.findByEan("7891000100103")).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            globalProductService.getProductByEan("7891000100103")
        }
    }

    @Test
    fun `getProductByEan should throw ValidationException when EAN is alphanumeric, invalid length, or all zeros`() {
        assertThrows<ValidationException> {
            globalProductService.getProductByEan("ABC12345")
        }
        assertThrows<ValidationException> {
            globalProductService.getProductByEan("12345")
        }
        assertThrows<ValidationException> {
            globalProductService.getProductByEan("0000000000000")
        }
    }

    @Test
    fun `ingestFromInvoiceItem should ignore invalid EAN or in-store restricted scale barcode (prefix 20-29)`() {
        val invalidEanItem = InvoiceItemResponseDto(
            ean = "SEM GTIN",
            name = "Pão Francês",
            quantity = 0.5,
            measureUnit = "KG"
        )
        val scaleBarcodeItem = InvoiceItemResponseDto(
            ean = "2001234001592",
            name = "QUEIJO MUSSARELA FATIADO",
            quantity = 0.35,
            measureUnit = "KG"
        )

        assertNull(globalProductService.ingestFromInvoiceItem(invalidEanItem))
        assertNull(globalProductService.ingestFromInvoiceItem(scaleBarcodeItem))
        verify(globalProductRepository, never()).save(any())
    }

    @Test
    fun `ingestFromInvoiceItem should create new record mapping SEFAZ measureUnit and keeping netWeight 1_0`() {
        val item = InvoiceItemResponseDto(
            ean = "7891000100103",
            name = "ARROZ TIPO 1 CAMIL",
            quantity = 3.0,
            measureUnit = "KG"
        )
        whenever(globalProductRepository.findByEan("7891000100103")).thenReturn(Optional.empty())
        whenever(globalProductRepository.save(any<GlobalProductEntity>())).thenAnswer { it.arguments[0] }

        val saved = globalProductService.ingestFromInvoiceItem(item)

        assertNotNull(saved)
        assertEquals(DeterministicIdUtils.productIdForEan("7891000100103"), saved!!.id)
        assertEquals("7891000100103", saved.ean)
        assertEquals("ARROZ TIPO 1 CAMIL", saved.name)
        assertEquals("arroz tipo 1 camil", saved.normalizedName)
        assertEquals("Outros", saved.category)
        assertEquals(MeasureUnit.KILOGRAM, saved.measureUnit)
        assertEquals(1.0, saved.netWeight)
        assertEquals("SEFAZ_INVOICE", saved.source)
        assertEquals(1L, saved.occurrenceCount)
    }

    @Test
    fun `ingestFromInvoiceItem should increment occurrenceCount and update updatedAt when EAN already exists`() {
        val existing = GlobalProductEntity(
            id = "global-1",
            ean = "7891000100103",
            name = "LEIT COND MOCA",
            normalizedName = "leit cond moca",
            source = "SEFAZ_INVOICE",
            occurrenceCount = 2L,
            updatedAt = 1000L
        )
        val item = InvoiceItemResponseDto(
            ean = "7891000100103",
            name = "LEIT COND MOCA 395G",
            quantity = 2.0,
            measureUnit = "UN"
        )
        whenever(globalProductRepository.findByEan("7891000100103")).thenReturn(Optional.of(existing))
        whenever(globalProductRepository.save(any<GlobalProductEntity>())).thenAnswer { it.arguments[0] }

        val saved = globalProductService.ingestFromInvoiceItem(item)

        assertNotNull(saved)
        assertEquals(3L, saved!!.occurrenceCount)
        assertEquals("LEIT COND MOCA 395G", saved.name)
        assertTrue(saved.updatedAt > 1000L)
    }

    @Test
    fun `ingestFromUserProduct should enrich brand, measureUnit, override SEFAZ_INVOICE name and keep fixed category Outros`() {
        val existingFromSefaz = GlobalProductEntity(
            id = DeterministicIdUtils.productIdForEan("7891000100103"),
            ean = "7891000100103",
            name = "LEIT COND MOCA NESTLE LATA",
            normalizedName = "leit cond moca nestle lata",
            brand = null,
            category = "Outros",
            measureUnit = MeasureUnit.UNIT,
            netWeight = 1.0,
            source = "SEFAZ_INVOICE",
            occurrenceCount = 4L
        )
        whenever(globalProductRepository.findByEan("7891000100103")).thenReturn(Optional.of(existingFromSefaz))
        whenever(globalProductRepository.save(any<GlobalProductEntity>())).thenAnswer { it.arguments[0] }

        val saved = globalProductService.ingestFromUserProduct(
            ean = "7891000100103",
            name = "Leite Condensado Moça",
            brand = "Nestlé",
            category = "Coisinhas Customizadas da Casa",
            measureUnit = MeasureUnit.GRAM,
            netWeight = 395.0
        )

        assertNotNull(saved)
        assertEquals("Leite Condensado Moça", saved!!.name)
        assertEquals("leite condensado moca", saved.normalizedName)
        assertEquals("Nestlé", saved.brand)
        assertEquals("Outros", saved.category)
        assertEquals(MeasureUnit.GRAM, saved.measureUnit)
        assertEquals(395.0, saved.netWeight)
        assertEquals("USER_SYNC", saved.source)
        assertEquals(5L, saved.occurrenceCount)
    }

    @Test
    fun `ingestFromInvoiceItem should NOT overwrite curated USER_SYNC name even if SEFAZ description is longer`() {
        val existingFromUser = GlobalProductEntity(
            id = "global-1",
            ean = "7891000100103",
            name = "Leite Integral Piracanjuba",
            normalizedName = "leite integral piracanjuba",
            brand = "Piracanjuba",
            category = "Laticínios",
            measureUnit = MeasureUnit.LITER,
            netWeight = 1.0,
            source = "USER_SYNC",
            occurrenceCount = 10L
        )
        val noisyInvoiceItem = InvoiceItemResponseDto(
            ean = "7891000100103",
            name = "LEITE UHT INTEGRAL PIRACANJUBA TP 1L COD INTERNO 492019",
            quantity = 12.0,
            measureUnit = "UN"
        )
        whenever(globalProductRepository.findByEan("7891000100103")).thenReturn(Optional.of(existingFromUser))
        whenever(globalProductRepository.save(any<GlobalProductEntity>())).thenAnswer { it.arguments[0] }

        val saved = globalProductService.ingestFromInvoiceItem(noisyInvoiceItem)

        assertNotNull(saved)
        assertEquals("Leite Integral Piracanjuba", saved!!.name)
        assertEquals("USER_SYNC", saved.source)
        assertEquals(11L, saved.occurrenceCount)
    }

    @Test
    fun `ingestFromUserProduct should enrich netWeight and measureUnit even when SEFAZ_INVOICE had non-UNIT measureUnit`() {
        val existingFromSefaz = GlobalProductEntity(
            id = "global-arroz",
            ean = "7891111111111",
            name = "ARROZ TIO JOAO 5KG",
            normalizedName = "arroz tio joao 5kg",
            brand = null,
            category = "Geral",
            measureUnit = MeasureUnit.KILOGRAM,
            netWeight = 1.0,
            source = "SEFAZ_INVOICE",
            occurrenceCount = 2L
        )
        whenever(globalProductRepository.findByEan("7891111111111")).thenReturn(Optional.of(existingFromSefaz))
        whenever(globalProductRepository.save(any<GlobalProductEntity>())).thenAnswer { it.arguments[0] }

        val saved = globalProductService.ingestFromUserProduct(
            ean = "7891111111111",
            name = "Arroz Tio João Tipo 1",
            brand = "Tio João",
            category = "Alimentos",
            measureUnit = MeasureUnit.KILOGRAM,
            netWeight = 5.0
        )

        assertNotNull(saved)
        assertEquals("Arroz Tio João Tipo 1", saved!!.name)
        assertEquals("Tio João", saved.brand)
        assertEquals("Outros", saved.category)
        assertEquals(MeasureUnit.KILOGRAM, saved.measureUnit)
        assertEquals(5.0, saved.netWeight)
        assertEquals("USER_SYNC", saved.source)
        assertEquals(3L, saved.occurrenceCount)
    }

    @Test
    fun `searchProducts should return empty list for short query and delegate normalized and raw queries to repository`() {
        assertTrue(globalProductService.searchProducts("a", 20).isEmpty())
        verify(globalProductRepository, never()).searchProducts(any(), any(), any())

        val entity = GlobalProductEntity(
            id = "global-1",
            ean = "7891000100103",
            name = "Café Pilão",
            normalizedName = "cafe pilao",
            brand = "Pilão"
        )
        whenever(globalProductRepository.searchProducts(eq("cafe"), eq("café"), eq(PageRequest.of(0, 50))))
            .thenReturn(listOf(entity))

        val results = globalProductService.searchProducts("Café", 100)

        assertEquals(1, results.size)
        assertEquals("Café Pilão", results.first().name)
    }
}
