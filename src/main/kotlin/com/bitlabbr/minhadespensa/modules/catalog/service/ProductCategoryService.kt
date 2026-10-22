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

import com.bitlabbr.minhadespensa.common.util.DeterministicIdUtils
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductCategorySyncDto
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductCategoryUpsertResult
import com.bitlabbr.minhadespensa.modules.catalog.entity.ProductCategoryEntity
import com.bitlabbr.minhadespensa.modules.catalog.repository.ProductCategoryRepository
import com.bitlabbr.minhadespensa.modules.sync.entity.EntityIdAliasEntity
import com.bitlabbr.minhadespensa.modules.sync.repository.EntityIdAliasRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductCategoryService(
    private val categoryRepository: ProductCategoryRepository,
    private val aliasRepository: EntityIdAliasRepository
) {

    @Transactional
    fun upsertForSync(householdId: String, dto: ProductCategorySyncDto, syncVersion: Long): ProductCategoryUpsertResult {
        val normalizedName = DeterministicIdUtils.normalizeText(dto.name)

        // 1. Verifica se já existe um alias registrado para este ID
        val aliasOpt = aliasRepository.findByHouseholdIdAndEntityTypeAndAliasId(householdId, "CATEGORY", dto.id)
        val targetId = if (aliasOpt.isPresent) aliasOpt.get().canonicalId else dto.id

        // 2. Busca por ID direto
        val byIdOpt = categoryRepository.findByHouseholdIdAndId(householdId, targetId)
        if (byIdOpt.isPresent) {
            val existing = byIdOpt.get()
            if (dto.updatedAt >= existing.updatedAt) {
                existing.name = dto.name.trim()
                existing.normalizedName = normalizedName
                existing.isDefault = dto.isDefault
                existing.updatedAt = dto.updatedAt
                existing.serverVersion = syncVersion
                existing.isDeleted = dto.isDeleted
                categoryRepository.save(existing)
            }
            return ProductCategoryUpsertResult(entity = existing, canonicalId = existing.id)
        }

        // 3. Busca por nome normalizado (deduplicação canônica)
        val byNameOpt = categoryRepository.findByHouseholdIdAndNormalizedName(householdId, normalizedName)
        if (byNameOpt.isPresent) {
            val existing = byNameOpt.get()
            if (existing.id != dto.id) {
                aliasRepository.save(
                    EntityIdAliasEntity(
                        householdId = householdId,
                        entityType = "CATEGORY",
                        aliasId = dto.id,
                        canonicalId = existing.id
                    )
                )
            }
            if (dto.updatedAt >= existing.updatedAt) {
                existing.name = dto.name.trim()
                existing.isDefault = dto.isDefault
                existing.updatedAt = dto.updatedAt
                existing.serverVersion = syncVersion
                existing.isDeleted = dto.isDeleted
                categoryRepository.save(existing)
            }
            return ProductCategoryUpsertResult(
                entity = existing,
                aliasId = if (existing.id != dto.id) dto.id else null,
                canonicalId = existing.id
            )
        }

        // 4. Cria nova categoria
        val newCategory = ProductCategoryEntity(
            householdId = householdId,
            id = targetId,
            name = dto.name.trim(),
            normalizedName = normalizedName,
            isDefault = dto.isDefault,
            updatedAt = dto.updatedAt,
            serverVersion = syncVersion,
            isDeleted = dto.isDeleted
        )
        val saved = categoryRepository.save(newCategory)
        return ProductCategoryUpsertResult(entity = saved, canonicalId = saved.id)
    }

    @Transactional(readOnly = true)
    fun getModifiedSince(householdId: String, sinceVersion: Long): List<ProductCategoryEntity> {
        return categoryRepository.findByHouseholdIdAndServerVersionGreaterThanOrderByServerVersionAsc(householdId, sinceVersion)
    }
}
