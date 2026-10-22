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

import com.bitlabbr.minhadespensa.common.exception.ConflictException
import com.bitlabbr.minhadespensa.common.exception.ResourceNotFoundException
import com.bitlabbr.minhadespensa.common.util.DeterministicIdUtils
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductRequestDto
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductResponseDto
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductUpsertResult
import com.bitlabbr.minhadespensa.modules.catalog.entity.ProductEntity
import com.bitlabbr.minhadespensa.modules.catalog.repository.ProductRepository
import com.bitlabbr.minhadespensa.modules.sync.entity.EntityIdAliasEntity
import com.bitlabbr.minhadespensa.modules.sync.repository.EntityIdAliasRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val aliasRepository: EntityIdAliasRepository
) {
    private val logger = LoggerFactory.getLogger(ProductService::class.java)

    @Transactional(readOnly = true)
    fun getAllProducts(householdId: String): List<ProductResponseDto> {
        return productRepository.findByHouseholdIdAndIsDeletedFalse(householdId)
            .map { ProductResponseDto.fromEntity(it) }
    }

    @Transactional(readOnly = true)
    fun getProductById(householdId: String, id: String): ProductResponseDto {
        val entity = productRepository.findByHouseholdIdAndIdAndIsDeletedFalse(householdId, id)
            .orElseThrow { ResourceNotFoundException("Produto com ID '$id' não encontrado.", "Product", id) }
        return ProductResponseDto.fromEntity(entity)
    }

    @Transactional(readOnly = true)
    fun getProductByEan(householdId: String, ean: String): ProductResponseDto {
        val entity = productRepository.findByHouseholdIdAndEanAndIsDeletedFalse(householdId, ean)
            .orElseThrow { ResourceNotFoundException("Produto com código EAN '$ean' não encontrado.", "Product", ean) }
        return ProductResponseDto.fromEntity(entity)
    }

    @Transactional(readOnly = true)
    fun searchProducts(householdId: String, query: String): List<ProductResponseDto> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return getAllProducts(householdId)
        }
        return productRepository.searchProducts(householdId, trimmed)
            .map { ProductResponseDto.fromEntity(it) }
    }

    @Transactional
    fun saveProduct(householdId: String, dto: ProductRequestDto, syncVersion: Long = 0L): ProductResponseDto {
        val result = upsertForSync(householdId, dto, syncVersion)
        return ProductResponseDto.fromEntity(result.entity)
    }

    @Transactional
    fun upsertForSync(
        householdId: String,
        dto: ProductRequestDto,
        syncVersion: Long,
        serverNow: Long = System.currentTimeMillis()
    ): ProductUpsertResult {
        val cleanEan = dto.ean?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedName = DeterministicIdUtils.normalizeText(dto.name)

        // 1. Determina o ID de destino (determinístico se não fornecido)
        val rawTargetId = dto.id ?: if (cleanEan != null) {
            DeterministicIdUtils.productIdForEan(cleanEan)
        } else {
            DeterministicIdUtils.productIdForNameAndUnit(dto.name, dto.measureUnit.name)
        }

        // 2. Resolve alias durável existente
        val targetId = aliasRepository.findCanonicalId(householdId, "PRODUCT", rawTargetId) ?: rawTargetId

        // 3. Busca por chave primária
        val existingByIdOpt = productRepository.findByHouseholdIdAndId(householdId, targetId)

        // 4. Busca por chave natural única na casa (EAN ou Nome+Unidade)
        val existingByNaturalKeyOpt = if (cleanEan != null) {
            productRepository.findByHouseholdIdAndEan(householdId, cleanEan)
        } else {
            productRepository.findByHouseholdIdAndNormalizedNameAndMeasureUnitAndEanIsNull(householdId, normalizedName, dto.measureUnit)
        }

        val canonical = if (existingByNaturalKeyOpt.isPresent) {
            existingByNaturalKeyOpt.get()
        } else if (existingByIdOpt.isPresent) {
            existingByIdOpt.get()
        } else {
            null
        }

        val incomingUpdatedAt = dto.updatedAt ?: serverNow

        return if (canonical != null) {
            if (canonical.id != targetId) {
                // Colisão / Deduplicação: o ID recebido aponta para um canônico existente com outro ID
                logger.info("Deduplicating product $targetId to canonical ${canonical.id} in household $householdId")
                aliasRepository.save(
                    EntityIdAliasEntity(
                        householdId = householdId,
                        entityType = "PRODUCT",
                        aliasId = targetId,
                        canonicalId = canonical.id
                    )
                )
                if (rawTargetId != targetId) {
                    aliasRepository.save(
                        EntityIdAliasEntity(
                            householdId = householdId,
                            entityType = "PRODUCT",
                            aliasId = rawTargetId,
                            canonicalId = canonical.id
                        )
                    )
                }

                // Se havia uma linha sob targetId no banco, marca como excluída e zera EAN
                if (existingByIdOpt.isPresent && existingByIdOpt.get().id != canonical.id) {
                    val obsolete = existingByIdOpt.get()
                    obsolete.isDeleted = true
                    obsolete.ean = null
                    obsolete.updatedAt = serverNow
                    obsolete.serverVersion = syncVersion
                    productRepository.save(obsolete)
                }

                // Aplica LWW no registro canônico
                val saved = if (incomingUpdatedAt >= canonical.updatedAt) {
                    canonical.ean = cleanEan ?: canonical.ean
                    canonical.name = dto.name
                    canonical.normalizedName = normalizedName
                    canonical.category = dto.category
                    canonical.brand = dto.brand ?: canonical.brand
                    canonical.measureUnit = dto.measureUnit
                    canonical.netWeight = dto.netWeight
                    canonical.thumbnailUrl = dto.thumbnailUrl ?: canonical.thumbnailUrl
                    canonical.imageHash = dto.imageHash ?: canonical.imageHash
                    canonical.manuallyAdded = dto.manuallyAdded
                    canonical.notes = dto.notes ?: canonical.notes
                    canonical.updatedAt = incomingUpdatedAt
                    canonical.serverVersion = syncVersion
                    canonical.isDeleted = dto.isDeleted
                    productRepository.save(canonical)
                } else {
                    logger.debug("LWW discarded update on ${canonical.id}: server=${canonical.updatedAt} > incoming=$incomingUpdatedAt")
                    canonical
                }
                ProductUpsertResult(entity = saved, canonicalId = canonical.id, remappedFromId = targetId)
            } else {
                // Atualização normal do mesmo registro
                val saved = if (incomingUpdatedAt >= canonical.updatedAt) {
                    canonical.ean = cleanEan
                    canonical.name = dto.name
                    canonical.normalizedName = normalizedName
                    canonical.category = dto.category
                    canonical.brand = dto.brand
                    canonical.measureUnit = dto.measureUnit
                    canonical.netWeight = dto.netWeight
                    canonical.thumbnailUrl = dto.thumbnailUrl ?: canonical.thumbnailUrl
                    canonical.imageHash = dto.imageHash ?: canonical.imageHash
                    canonical.manuallyAdded = dto.manuallyAdded
                    canonical.notes = dto.notes
                    canonical.updatedAt = incomingUpdatedAt
                    canonical.serverVersion = syncVersion
                    canonical.isDeleted = dto.isDeleted
                    productRepository.save(canonical)
                } else {
                    logger.debug("LWW discarded update on ${canonical.id}: server=${canonical.updatedAt} > incoming=$incomingUpdatedAt")
                    canonical
                }
                ProductUpsertResult(entity = saved, canonicalId = canonical.id, remappedFromId = null)
            }
        } else {
            // Criação de novo produto no catálogo da casa
            val newProduct = ProductEntity(
                householdId = householdId,
                id = targetId,
                ean = cleanEan,
                name = dto.name,
                normalizedName = normalizedName,
                category = dto.category,
                brand = dto.brand,
                measureUnit = dto.measureUnit,
                netWeight = dto.netWeight,
                thumbnailUrl = dto.thumbnailUrl,
                imageHash = dto.imageHash,
                manuallyAdded = dto.manuallyAdded,
                notes = dto.notes,
                updatedAt = incomingUpdatedAt,
                serverVersion = syncVersion,
                isDeleted = dto.isDeleted
            )
            val saved = productRepository.save(newProduct)
            ProductUpsertResult(entity = saved, canonicalId = saved.id, remappedFromId = null)
        }
    }

    @Transactional
    fun deleteProduct(householdId: String, id: String, syncVersion: Long = 0L) {
        val entity = productRepository.findByHouseholdIdAndIdAndIsDeletedFalse(householdId, id)
            .orElseThrow { ResourceNotFoundException("Product with ID '$id' not found for deletion.", "Product", id) }

        entity.isDeleted = true
        entity.updatedAt = System.currentTimeMillis()
        entity.serverVersion = syncVersion
        productRepository.save(entity)
        logger.info("Product $id in household $householdId marked as deleted (soft-delete).")
    }

    @Transactional(readOnly = true)
    fun getModifiedSince(householdId: String, sinceVersion: Long): List<ProductEntity> {
        return productRepository.findByHouseholdIdAndServerVersionGreaterThanOrderByServerVersionAsc(householdId, sinceVersion)
    }
}

typealias ProdutoService = ProductService
