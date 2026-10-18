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

import com.bitlabbr.minhadespensa.common.exception.BusinessException
import com.bitlabbr.minhadespensa.common.exception.InvalidRequestException
import com.bitlabbr.minhadespensa.common.exception.ResourceNotFoundException
import com.bitlabbr.minhadespensa.modules.catalog.domain.MeasureUnit
import com.bitlabbr.minhadespensa.modules.catalog.entity.ProductEntity
import com.bitlabbr.minhadespensa.modules.catalog.repository.ProductRepository
import com.bitlabbr.minhadespensa.modules.pantry.domain.PantryItemStatus
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
    private lateinit var productRepository: ProductRepository
    private lateinit var pantryService: PantryService

    @BeforeEach
    fun setUp() {
        pantryItemRepository = mock()
        productRepository = mock()
        pantryService = PantryService(pantryItemRepository, productRepository)
    }

    @Test
    fun `saveItem should create pantry item when it does not exist`() {
        val dto = PantryItemRequestDto(
            id = "item-1",
            productId = "prod-1",
            quantity = 2.5,
            unitPrice = 5.50,
            purchaseDate = 1786320000000L,
            expirationDate = 1789862400000L,
            purchaseLocation = "Carrefour",
            status = PantryItemStatus.EM_ESTOQUE,
            batchNumber = "LOTE-1",
            updatedAt = 1000L
        )

        whenever(pantryItemRepository.findById("item-1")).thenReturn(Optional.empty())
        whenever(pantryItemRepository.save(any<PantryItemEntity>())).thenAnswer { it.arguments[0] }

        val result = pantryService.saveItem(dto)

        assertEquals("item-1", result.id)
        assertEquals("prod-1", result.productId)
        assertEquals(2.5, result.quantity)
        assertEquals(5.50, result.unitPrice)
        assertEquals("Carrefour", result.purchaseLocation)
        assertEquals(PantryItemStatus.EM_ESTOQUE, result.status)
        assertFalse(result.isDeleted)
    }

    @Test
    fun `saveItem should update existing item when updatedAt is newer`() {
        val existing = PantryItemEntity(
            id = "item-1",
            productId = "prod-1",
            quantity = 1.0,
            updatedAt = 1000L
        )

        val dto = PantryItemRequestDto(
            id = "item-1",
            productId = "prod-1",
            quantity = 3.0,
            unitPrice = 6.20,
            purchaseLocation = "Pão de Açúcar",
            updatedAt = 2000L
        )

        whenever(pantryItemRepository.findById("item-1")).thenReturn(Optional.of(existing))
        whenever(pantryItemRepository.save(existing)).thenReturn(existing)

        val result = pantryService.saveItem(dto)

        assertEquals(3.0, result.quantity)
        assertEquals(6.20, result.unitPrice)
        assertEquals("Pão de Açúcar", result.purchaseLocation)
        assertEquals(2000L, result.updatedAt)
    }

    @Test
    fun `deleteItem should mark item as deleted`() {
        val existing = PantryItemEntity(
            id = "item-1",
            productId = "prod-1",
            quantity = 2.0,
            isDeleted = false
        )

        whenever(pantryItemRepository.findByIdAndIsDeletedFalse("item-1"))
            .thenReturn(Optional.of(existing))

        pantryService.deleteItem("item-1")

        assertTrue(existing.isDeleted)
        verify(pantryItemRepository).save(existing)
    }

    @Test
    fun `getItemById should throw ResourceNotFoundException when not found`() {
        whenever(pantryItemRepository.findByIdAndIsDeletedFalse("unknown"))
            .thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            pantryService.getItemById("unknown")
        }
    }

    @Test
    fun `getAggregatedPantry should group active stock by product`() {
        val product = ProductEntity(
            id = "prod-1",
            ean = "7891000100103",
            name = "Leite Ninho",
            category = "Laticínios",
            brand = "Nestlé",
            measureUnit = MeasureUnit.UNIT
        )

        val item1 = PantryItemEntity(
            id = "item-1",
            productId = "prod-1",
            quantity = 2.0,
            unitPrice = 5.50,
            expirationDate = 1789862400000L,
            purchaseLocation = "Carrefour",
            status = PantryItemStatus.EM_ESTOQUE
        )
        val item2 = PantryItemEntity(
            id = "item-2",
            productId = "prod-1",
            quantity = 1.0,
            unitPrice = 6.20,
            expirationDate = 1792454400000L,
            purchaseLocation = "Pão de Açúcar",
            status = PantryItemStatus.EM_ESTOQUE
        )

        whenever(pantryItemRepository.findAllByStatusAndIsDeletedFalse(PantryItemStatus.EM_ESTOQUE))
            .thenReturn(listOf(item1, item2))
        whenever(productRepository.findAllById(listOf("prod-1")))
            .thenReturn(listOf(product))

        val result = pantryService.getAggregatedPantry()

        assertEquals(1, result.size)
        val aggregated = result[0]
        assertEquals("prod-1", aggregated.produto.id)
        assertEquals("Leite Ninho", aggregated.produto.nome)
        assertEquals(3.0, aggregated.quantidadeTotal)
        assertEquals(2, aggregated.ocorrencias.size)
        assertEquals("Carrefour", aggregated.ocorrencias[0].localCompra)
        assertEquals("Pão de Açúcar", aggregated.ocorrencias[1].localCompra)
    }

    @Test
    fun `consumeItem should deduct quantity and keep in stock if remaining positive`() {
        val entity = PantryItemEntity(
            id = "item-1",
            productId = "prod-1",
            quantity = 3.0,
            status = PantryItemStatus.EM_ESTOQUE
        )

        whenever(pantryItemRepository.findByIdAndIsDeletedFalse("item-1"))
            .thenReturn(Optional.of(entity))
        whenever(pantryItemRepository.save(entity)).thenReturn(entity)

        val result = pantryService.consumeItem("item-1", 1.0)

        assertEquals(2.0, result.quantity)
        assertEquals(PantryItemStatus.EM_ESTOQUE, result.status)
    }

    @Test
    fun `consumeItem should update status to CONSUMIDO when full quantity is consumed`() {
        val entity = PantryItemEntity(
            id = "item-1",
            productId = "prod-1",
            quantity = 2.0,
            status = PantryItemStatus.EM_ESTOQUE
        )

        whenever(pantryItemRepository.findByIdAndIsDeletedFalse("item-1"))
            .thenReturn(Optional.of(entity))
        whenever(pantryItemRepository.save(entity)).thenReturn(entity)

        val result = pantryService.consumeItem("item-1", 2.0)

        assertEquals(0.0, result.quantity)
        assertEquals(PantryItemStatus.CONSUMIDO, result.status)
    }

    @Test
    fun `consumeItem should throw BusinessException when consuming more than available`() {
        val entity = PantryItemEntity(
            id = "item-1",
            productId = "prod-1",
            quantity = 1.0,
            status = PantryItemStatus.EM_ESTOQUE
        )

        whenever(pantryItemRepository.findByIdAndIsDeletedFalse("item-1"))
            .thenReturn(Optional.of(entity))

        assertThrows<BusinessException> {
            pantryService.consumeItem("item-1", 2.0)
        }
    }

    @Test
    fun `consumeItem should throw InvalidRequestException when quantity is zero or negative`() {
        assertThrows<InvalidRequestException> {
            pantryService.consumeItem("item-1", 0.0)
        }
    }

    @Test
    fun `discardItem should set status to DESCARTADO and zero quantity`() {
        val entity = PantryItemEntity(
            id = "item-1",
            productId = "prod-1",
            quantity = 2.0,
            status = PantryItemStatus.EM_ESTOQUE
        )

        whenever(pantryItemRepository.findByIdAndIsDeletedFalse("item-1"))
            .thenReturn(Optional.of(entity))
        whenever(pantryItemRepository.save(entity)).thenReturn(entity)

        val result = pantryService.discardItem("item-1")

        assertEquals(0.0, result.quantity)
        assertEquals(PantryItemStatus.DESCARTADO, result.status)
    }
}
