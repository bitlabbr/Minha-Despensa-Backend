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
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class ShoppingListServiceTest {

    private lateinit var shoppingListRepository: ShoppingListRepository
    private lateinit var shoppingItemRepository: ShoppingItemRepository
    private lateinit var shoppingListService: ShoppingListService

    private val householdId = "house-123"

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

        whenever(shoppingListRepository.findByHouseholdIdAndId(householdId, "list-1")).thenReturn(Optional.empty())
        whenever(shoppingListRepository.save(any<ShoppingListEntity>())).thenAnswer { it.arguments[0] }
        whenever(shoppingItemRepository.findByHouseholdIdAndListIdAndIsDeletedFalse(householdId, "list-1")).thenReturn(emptyList())

        val result = shoppingListService.saveList(householdId, dto, syncVersion = 10L)

        assertEquals("list-1", result.id)
        assertEquals("Compras Semanais", result.name)
        assertEquals(15000L, result.budgetInCents)
        assertEquals(10L, result.serverVersion)
        assertFalse(result.isDeleted)
    }

    @Test
    fun `deleteList should cascade soft delete to list and its items`() {
        val list = ShoppingListEntity(householdId = householdId, id = "list-1", name = "Lista Teste", isDeleted = false)
        val item1 = ShoppingItemEntity(householdId = householdId, id = "item-1", listId = "list-1", isDeleted = false)
        val item2 = ShoppingItemEntity(householdId = householdId, id = "item-2", listId = "list-1", isDeleted = false)

        whenever(shoppingListRepository.findByHouseholdIdAndIdAndIsDeletedFalse(householdId, "list-1")).thenReturn(Optional.of(list))
        whenever(shoppingItemRepository.findByHouseholdIdAndListIdAndIsDeletedFalse(householdId, "list-1")).thenReturn(listOf(item1, item2))

        shoppingListService.deleteList(householdId, "list-1", syncVersion = 25L)

        assertTrue(list.isDeleted)
        assertEquals(25L, list.serverVersion)
        assertTrue(item1.isDeleted)
        assertEquals(25L, item1.serverVersion)
        assertTrue(item2.isDeleted)
        assertEquals(25L, item2.serverVersion)
        verify(shoppingListRepository).save(list)
        verify(shoppingItemRepository).saveAll(listOf(item1, item2))
    }

    @Test
    fun `saveList should propagate cascade tombstone when saving a deleted list`() {
        val dto = ShoppingListRequestDto(
            id = "list-1",
            name = "Lista Antiga",
            updatedAt = 5000L,
            isDeleted = true
        )

        val existingList = ShoppingListEntity(householdId = householdId, id = "list-1", updatedAt = 4000L, isDeleted = false)
        val item1 = ShoppingItemEntity(householdId = householdId, id = "item-1", listId = "list-1", isDeleted = false, updatedAt = 3000L)

        whenever(shoppingListRepository.findByHouseholdIdAndId(householdId, "list-1")).thenReturn(Optional.of(existingList))
        whenever(shoppingListRepository.save(existingList)).thenReturn(existingList)
        whenever(shoppingItemRepository.findByHouseholdIdAndListId(householdId, "list-1")).thenReturn(listOf(item1))
        whenever(shoppingItemRepository.findByHouseholdIdAndListIdAndIsDeletedFalse(householdId, "list-1")).thenReturn(emptyList())

        val result = shoppingListService.saveList(householdId, dto, syncVersion = 30L)

        assertTrue(result.isDeleted)
        assertTrue(item1.isDeleted)
        assertEquals(30L, item1.serverVersion)
        assertEquals(5000L, item1.updatedAt)
        verify(shoppingItemRepository).saveAll(listOf(item1))
    }

    @Test
    fun `saveItem should enforce parent tombstone wins rule if parent list is already deleted`() {
        val parentList = ShoppingListEntity(
            householdId = householdId,
            id = "list-1",
            name = "Lista Deletada",
            updatedAt = 5000L,
            isDeleted = true
        )

        val itemDto = ShoppingItemRequestDto(
            id = "item-1",
            listId = "list-1",
            productId = "prod-1",
            quantity = 2.0,
            updatedAt = 4000L,
            isDeleted = false // Cliente acha que o item ainda está vivo
        )

        whenever(shoppingListRepository.findByHouseholdIdAndId(householdId, "list-1")).thenReturn(Optional.of(parentList))
        whenever(shoppingItemRepository.findByHouseholdIdAndId(householdId, "item-1")).thenReturn(Optional.empty())
        whenever(shoppingItemRepository.save(any<ShoppingItemEntity>())).thenAnswer { it.arguments[0] }

        val result = shoppingListService.saveItem(householdId, itemDto, syncVersion = 40L)

        // Item DEVE ser forçado como excluído porque o pai já faleceu com timestamp mais recente
        assertTrue(result.isDeleted)
        assertEquals(40L, result.serverVersion)
    }

    @Test
    fun `saveItem should create new shopping item normally when parent is active`() {
        val parentList = ShoppingListEntity(
            householdId = householdId,
            id = "list-1",
            name = "Lista Ativa",
            isDeleted = false
        )

        val dto = ShoppingItemRequestDto(
            id = "item-1",
            listId = "list-1",
            productId = "prod-1",
            quantity = 2.0,
            priceAtTime = 500L,
            isChecked = false,
            updatedAt = 1000L
        )

        whenever(shoppingListRepository.findByHouseholdIdAndId(householdId, "list-1")).thenReturn(Optional.of(parentList))
        whenever(shoppingItemRepository.findByHouseholdIdAndId(householdId, "item-1")).thenReturn(Optional.empty())
        whenever(shoppingItemRepository.save(any<ShoppingItemEntity>())).thenAnswer { it.arguments[0] }

        val result = shoppingListService.saveItem(householdId, dto, syncVersion = 10L)

        assertEquals("item-1", result.id)
        assertEquals("list-1", result.listId)
        assertEquals("prod-1", result.productId)
        assertEquals(2.0, result.quantity)
        assertEquals(10L, result.serverVersion)
        assertFalse(result.isDeleted)
    }
}
