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

import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductCategorySyncDto
import com.bitlabbr.minhadespensa.modules.catalog.entity.ProductCategoryEntity
import com.bitlabbr.minhadespensa.modules.catalog.repository.ProductCategoryRepository
import com.bitlabbr.minhadespensa.modules.sync.entity.EntityIdAliasEntity
import com.bitlabbr.minhadespensa.modules.sync.repository.EntityIdAliasRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class ProductCategoryServiceTest {

    private val categoryRepository: ProductCategoryRepository = mock()
    private val aliasRepository: EntityIdAliasRepository = mock()

    private lateinit var categoryService: ProductCategoryService

    @BeforeEach
    fun setUp() {
        categoryService = ProductCategoryService(categoryRepository, aliasRepository)
    }

    @Test
    fun `upsertForSync should create new category when none exists`() {
        val dto = ProductCategorySyncDto(id = "cat-1", name = "Laticínios", updatedAt = 1000L)
        whenever(aliasRepository.findByHouseholdIdAndEntityTypeAndAliasId("house-1", "CATEGORY", "cat-1"))
            .thenReturn(Optional.empty())
        whenever(categoryRepository.findByHouseholdIdAndId("house-1", "cat-1"))
            .thenReturn(Optional.empty())
        whenever(categoryRepository.findByHouseholdIdAndNormalizedName("house-1", "laticinios"))
            .thenReturn(Optional.empty())
        whenever(categoryRepository.save(any<ProductCategoryEntity>())).thenAnswer { it.arguments[0] }

        val result = categoryService.upsertForSync("house-1", dto, 1L)

        assertNotNull(result)
        assertEquals("cat-1", result.canonicalId)
        assertEquals("laticinios", result.entity.normalizedName)
        verify(categoryRepository).save(any<ProductCategoryEntity>())
    }

    @Test
    fun `upsertForSync should deduplicate category by normalized name and record alias`() {
        val existing = ProductCategoryEntity(
            householdId = "house-1",
            id = "canonical-cat",
            name = "Laticínios",
            normalizedName = "laticinios",
            updatedAt = 500L
        )

        val incomingDto = ProductCategorySyncDto(id = "temp-cat-2", name = "  LATICÍNIOS  ", updatedAt = 1000L)

        whenever(aliasRepository.findByHouseholdIdAndEntityTypeAndAliasId("house-1", "CATEGORY", "temp-cat-2"))
            .thenReturn(Optional.empty())
        whenever(categoryRepository.findByHouseholdIdAndId("house-1", "temp-cat-2"))
            .thenReturn(Optional.empty())
        whenever(categoryRepository.findByHouseholdIdAndNormalizedName("house-1", "laticinios"))
            .thenReturn(Optional.of(existing))
        whenever(categoryRepository.save(any<ProductCategoryEntity>())).thenAnswer { it.arguments[0] }

        val result = categoryService.upsertForSync("house-1", incomingDto, 2L)

        assertEquals("canonical-cat", result.canonicalId)
        assertEquals("temp-cat-2", result.aliasId)
        verify(aliasRepository).save(any<EntityIdAliasEntity>())
        verify(categoryRepository).save(existing)
    }
}
