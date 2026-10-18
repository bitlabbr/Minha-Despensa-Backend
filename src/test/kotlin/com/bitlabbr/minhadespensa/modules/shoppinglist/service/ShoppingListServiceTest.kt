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

package com.bitlabbr.minhadespensa.modules.shoppinglist.service

import com.bitlabbr.minhadespensa.common.exception.ResourceNotFoundException
import com.bitlabbr.minhadespensa.modules.shoppinglist.domain.ShoppingListStatus
import com.bitlabbr.minhadespensa.modules.shoppinglist.domain.ShoppingListType
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingItemRequestDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.dto.ShoppingListRequestDto
import com.bitlabbr.minhadespensa.modules.shoppinglist.entity.ShoppingItemEntity
import com.bitlabbr.minhadespensa.modules.shoppinglist.entity.ShoppingListEntity
import com.bitlabbr.minhadespensa.modules.shoppinglist.repository.ShoppingItemRepository
import com.bitlabbr.minhadespensa.modules.shoppinglist.repository.ShoppingListRepository
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

class ShoppingListServiceTest {

    private lateinit var shoppingListRepository: ShoppingListRepository
    private lateinit var shoppingItemRepository: ShoppingItemRepository
    private lateinit var shoppingListService: ShoppingListService

    @BeforeEach
    fun setUp() {
        shoppingListRepository = mock()
        shoppingItemRepository = mock()
        shoppingListService = ShoppingListService(shoppingListRepository, shoppingItemRepository)
    }

    @Test
    fun `saveList should create new shopping list when it does not exist`() {
        val dto = ShoppingListRequestDto(
            id = "list-1",
            name = "Compras Semanais",
            type = ShoppingListType.PLANNED,
            status = ShoppingListStatus.DRAFT,
            budgetInCents = 15000,
            updatedAt = 1000L
        )

        whenever(shoppingListRepository.findById("list-1")).thenReturn(Optional.empty())
        whenever(shoppingListRepository.save(any<ShoppingListEntity>())).thenAnswer { it.arguments[0] }
        whenever(shoppingItemRepository.findAllByListIdAndIsDeletedFalse("list-1")).thenReturn(emptyList())

        val result = shoppingListService.saveList(dto)

        assertEquals("list-1", result.id)
        assertEquals("Compras Semanais", result.name)
        assertEquals(15000L, result.budgetInCents)
        assertFalse(result.isDeleted)
    }

    @Test
    fun `deleteList should cascade soft delete to list and its items`() {
        val list = ShoppingListEntity(id = "list-1", name = "Lista Teste", isDeleted = false)
        val item1 = ShoppingItemEntity(id = "item-1", listId = "list-1", isDeleted = false)
        val item2 = ShoppingItemEntity(id = "item-2", listId = "list-1", isDeleted = false)

        whenever(shoppingListRepository.findByIdAndIsDeletedFalse("list-1")).thenReturn(Optional.of(list))
        whenever(shoppingItemRepository.findAllByListIdAndIsDeletedFalse("list-1")).thenReturn(listOf(item1, item2))

        shoppingListService.deleteList("list-1")

        assertTrue(list.isDeleted)
        assertTrue(item1.isDeleted)
        assertTrue(item2.isDeleted)
        verify(shoppingListRepository).save(list)
        verify(shoppingItemRepository).saveAll(listOf(item1, item2))
    }

    @Test
    fun `saveItem should create new shopping item`() {
        val dto = ShoppingItemRequestDto(
            id = "item-1",
            listId = "list-1",
            productId = "prod-1",
            quantity = 2.0,
            priceAtTime = 500L,
            isChecked = false,
            updatedAt = 1000L
        )

        whenever(shoppingItemRepository.findById("item-1")).thenReturn(Optional.empty())
        whenever(shoppingItemRepository.save(any<ShoppingItemEntity>())).thenAnswer { it.arguments[0] }

        val result = shoppingListService.saveItem(dto)

        assertEquals("item-1", result.id)
        assertEquals("list-1", result.listId)
        assertEquals("prod-1", result.productId)
        assertEquals(2.0, result.quantity)
    }
}
