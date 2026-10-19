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

package com.bitlabbr.minhadespensa.modules.sync.service

import com.bitlabbr.minhadespensa.modules.catalog.domain.MeasureUnit
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductRequestDto
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductResponseDto
import com.bitlabbr.minhadespensa.modules.catalog.entity.ProductEntity
import com.bitlabbr.minhadespensa.modules.catalog.repository.ProductRepository
import com.bitlabbr.minhadespensa.modules.catalog.service.ProductService
import com.bitlabbr.minhadespensa.modules.pantry.dto.PantryItemRequestDto
import com.bitlabbr.minhadespensa.modules.pantry.entity.PantryItemEntity
import com.bitlabbr.minhadespensa.modules.pantry.service.PantryService
import com.bitlabbr.minhadespensa.modules.price.dto.PriceEntryRequestDto
import com.bitlabbr.minhadespensa.modules.price.entity.PriceEntryEntity
import com.bitlabbr.minhadespensa.modules.price.service.PriceEntryService
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingItemRequestDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingListRequestDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.entity.ShoppingItemEntity
import com.bitlabbr.minhadespensa.modules.shoppinglist.entity.ShoppingListEntity
import com.bitlabbr.minhadespensa.modules.shoppinglist.service.ShoppingListService
import com.bitlabbr.minhadespensa.modules.sync.dto.SyncPushRequestDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SyncServiceTest {

    private lateinit var productService: ProductService
    private lateinit var productRepository: ProductRepository
    private lateinit var pantryService: PantryService
    private lateinit var shoppingListService: ShoppingListService
    private lateinit var priceEntryService: PriceEntryService
    private lateinit var syncService: SyncService

    @BeforeEach
    fun setUp() {
        productService = mock()
        productRepository = mock()
        pantryService = mock()
        shoppingListService = mock()
        priceEntryService = mock()
        syncService = SyncService(
            productService,
            productRepository,
            pantryService,
            shoppingListService,
            priceEntryService
        )
    }

    @Test
    fun `push should process all entities and return total count`() {
        val request = SyncPushRequestDto(
            products = listOf(
                ProductRequestDto(name = "Feijão Preto 1kg", category = "Alimentos")
            ),
            pantryItems = listOf(
                PantryItemRequestDto(productId = "p-1", quantity = 3.0)
            ),
            shoppingLists = listOf(
                ShoppingListRequestDto(name = "Feira Semanal")
            ),
            shoppingItems = listOf(
                ShoppingItemRequestDto(listId = "l-1", rawText = "Tomate 1kg")
            ),
            priceEntries = listOf(
                PriceEntryRequestDto(productId = "p-1", priceInCents = 899)
            )
        )

        val response = syncService.push(request)

        assertEquals(5, response.totalProcessed)
        assertTrue(response.serverTimestamp > 0)
        verify(productService).saveProduct(any())
        verify(pantryService).saveItem(any())
        verify(shoppingListService).saveList(any())
        verify(shoppingListService).saveItem(any())
        verify(priceEntryService).savePrice(any())
    }

    @Test
    fun `pull should return modified entities since timestamp`() {
        val since = 1000L

        val productEntity = ProductEntity(
            id = "prod-1",
            name = "Leite",
            category = "Laticínios",
            updatedAt = 1500L
        )

        val pantryEntity = PantryItemEntity(
            id = "pantry-1",
            productId = "prod-1",
            quantity = 2.0,
            updatedAt = 1600L
        )

        val listEntity = ShoppingListEntity(
            id = "list-1",
            name = "Lista 1",
            updatedAt = 1700L
        )

        val itemEntity = ShoppingItemEntity(
            id = "item-1",
            listId = "list-1",
            rawText = "Queijo",
            updatedAt = 1800L
        )

        val priceEntity = PriceEntryEntity(
            id = "price-1",
            productId = "prod-1",
            priceInCents = 450,
            updatedAt = 1900L
        )

        whenever(productRepository.findByUpdatedAtGreaterThan(since)).thenReturn(listOf(productEntity))
        whenever(pantryService.getModifiedSince(since)).thenReturn(listOf(pantryEntity))
        whenever(shoppingListService.getListsModifiedSince(since)).thenReturn(listOf(listEntity))
        whenever(shoppingListService.getItemsModifiedSince(since)).thenReturn(listOf(itemEntity))
        whenever(priceEntryService.getPricesModifiedSince(since)).thenReturn(listOf(priceEntity))

        val result = syncService.pull(since)

        assertEquals(1, result.products.size)
        assertEquals("prod-1", result.products[0].id)
        assertEquals(1, result.pantryItems.size)
        assertEquals("pantry-1", result.pantryItems[0].id)
        assertEquals(1, result.shoppingLists.size)
        assertEquals("list-1", result.shoppingLists[0].id)
        assertEquals(1, result.shoppingItems.size)
        assertEquals("item-1", result.shoppingItems[0].id)
        assertEquals(1, result.priceEntries.size)
        assertEquals("price-1", result.priceEntries[0].id)
    }
}
