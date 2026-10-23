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

        // 2. Busca por nome normalizado (chave natural canônica) e por ID
        val byNameOpt = categoryRepository.findByHouseholdIdAndNormalizedName(householdId, normalizedName)
        val byIdOpt = categoryRepository.findByHouseholdIdAndId(householdId, targetId)

        val canonical = if (byNameOpt.isPresent) {
            byNameOpt.get()
        } else if (byIdOpt.isPresent) {
            byIdOpt.get()
        } else {
            null
        }

        if (canonical != null) {
            val isRemapped = canonical.id != dto.id
            if (isRemapped) {
                aliasRepository.save(
                    EntityIdAliasEntity(
                        householdId = householdId,
                        entityType = "CATEGORY",
                        aliasId = dto.id,
                        canonicalId = canonical.id
                    )
                )
                if (targetId != canonical.id && targetId != dto.id) {
                    aliasRepository.save(
                        EntityIdAliasEntity(
                            householdId = householdId,
                            entityType = "CATEGORY",
                            aliasId = targetId,
                            canonicalId = canonical.id
                        )
                    )
                }

                // Se existia uma linha distinta sob targetId, marca como excluída e desambigua o nome
                if (byIdOpt.isPresent && byIdOpt.get().id != canonical.id) {
                    val obsolete = byIdOpt.get()
                    obsolete.isDeleted = true
                    obsolete.normalizedName = "${obsolete.normalizedName}#deleted#${obsolete.id}"
                    obsolete.updatedAt = System.currentTimeMillis()
                    obsolete.serverVersion = syncVersion
                    categoryRepository.save(obsolete)
                }
            }

            if (dto.updatedAt >= canonical.updatedAt) {
                canonical.name = dto.name.trim()
                canonical.normalizedName = normalizedName
                canonical.isDefault = dto.isDefault
                canonical.updatedAt = dto.updatedAt
                canonical.serverVersion = syncVersion
                canonical.isDeleted = dto.isDeleted
                categoryRepository.save(canonical)
            }

            return ProductCategoryUpsertResult(
                entity = canonical,
                aliasId = if (isRemapped) dto.id else null,
                canonicalId = canonical.id
            )
        }

        // 3. Cria nova categoria
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
        return ProductCategoryUpsertResult(
            entity = saved,
            aliasId = if (dto.id != saved.id) dto.id else null,
            canonicalId = saved.id
        )
    }

    @Transactional(readOnly = true)
    fun getModifiedSince(householdId: String, sinceVersion: Long): List<ProductCategoryEntity> {
        return categoryRepository.findByHouseholdIdAndServerVersionGreaterThanOrderByServerVersionAsc(householdId, sinceVersion)
    }

    @Transactional
    fun seedDefaultCategories(householdId: String): List<ProductCategoryEntity> {
        val existing = categoryRepository.findByHouseholdIdAndIsDeletedFalse(householdId)
        if (existing.isNotEmpty()) return existing

        val entities = DEFAULT_CATEGORIES.mapIndexed { index, name ->
            ProductCategoryEntity(
                householdId = householdId,
                id = "default_category_$index",
                name = name,
                normalizedName = DeterministicIdUtils.normalizeText(name),
                isDefault = true,
                updatedAt = 1000L,
                serverVersion = 1L,
                isDeleted = false
            )
        }
        return categoryRepository.saveAll(entities)
    }

    companion object {
        val DEFAULT_CATEGORIES = listOf(
            "Cereais e Grãos",
            "Carnes e Proteínas",
            "Laticínios e Ovos",
            "Hortifrúti",
            "Bebidas",
            "Produtos de Limpeza",
            "Higiene Pessoal",
            "Temperos e Condimentos",
            "Outros"
        )
    }
}
