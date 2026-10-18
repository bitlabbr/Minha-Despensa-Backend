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
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductRequestDto
import com.bitlabbr.minhadespensa.modules.catalog.dto.ProductResponseDto
import com.bitlabbr.minhadespensa.modules.catalog.entity.ProductEntity
import com.bitlabbr.minhadespensa.modules.catalog.repository.ProductRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProductService(
    private val productRepository: ProductRepository
) {
    private val logger = LoggerFactory.getLogger(ProductService::class.java)

    @Transactional(readOnly = true)
    fun getAllProducts(): List<ProductResponseDto> {
        return productRepository.findAllByIsDeletedFalse()
            .map { ProductResponseDto.fromEntity(it) }
    }

    @Transactional(readOnly = true)
    fun getProductById(id: String): ProductResponseDto {
        val entity = productRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow { ResourceNotFoundException("Produto com ID '$id' não encontrado.", "Product", id) }
        return ProductResponseDto.fromEntity(entity)
    }

    @Transactional(readOnly = true)
    fun getProductByEan(ean: String): ProductResponseDto {
        val entity = productRepository.findByEanAndIsDeletedFalse(ean)
            .orElseThrow { ResourceNotFoundException("Produto com código EAN '$ean' não encontrado.", "Product", ean) }
        return ProductResponseDto.fromEntity(entity)
    }

    @Transactional(readOnly = true)
    fun searchProducts(query: String): List<ProductResponseDto> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return getAllProducts()
        }
        return productRepository.searchProducts(trimmed)
            .map { ProductResponseDto.fromEntity(it) }
    }

    @Transactional
    fun saveProduct(dto: ProductRequestDto): ProductResponseDto {
        val targetId = dto.id ?: UUID.randomUUID().toString()

        // Verifica unicidade de EAN caso fornecido
        dto.ean?.let { ean ->
            if (ean.isNotBlank() && productRepository.existsByEanAndIdNotAndIsDeletedFalse(ean, targetId)) {
                throw ConflictException("Já existe um produto ativo cadastrado com o código EAN '$ean'.")
            }
        }

        val existingOpt = productRepository.findById(targetId)
        val now = System.currentTimeMillis()
        val incomingUpdatedAt = dto.updatedAt ?: now

        val entityToSave = if (existingOpt.isPresent) {
            val existing = existingOpt.get()
            // Regra CRDT / LWW: Atualiza somente se o incoming for mais recente ou igual
            if (incomingUpdatedAt >= existing.updatedAt) {
                existing.ean = dto.ean
                existing.name = dto.name
                existing.category = dto.category
                existing.brand = dto.brand
                existing.measureUnit = dto.measureUnit
                existing.netWeight = dto.netWeight
                existing.thumbnailUrl = dto.thumbnailUrl
                existing.manuallyAdded = dto.manuallyAdded
                existing.notes = dto.notes
                existing.updatedAt = incomingUpdatedAt
                existing.isDeleted = false
                existing
            } else {
                logger.info("Ignorando atualização do produto $targetId devido à regra LWW (existente=${existing.updatedAt} > recebido=$incomingUpdatedAt)")
                existing
            }
        } else {
            ProductEntity(
                id = targetId,
                ean = dto.ean,
                name = dto.name,
                category = dto.category,
                brand = dto.brand,
                measureUnit = dto.measureUnit,
                netWeight = dto.netWeight,
                thumbnailUrl = dto.thumbnailUrl,
                manuallyAdded = dto.manuallyAdded,
                notes = dto.notes,
                updatedAt = incomingUpdatedAt,
                isDeleted = false
            )
        }

        val saved = productRepository.save(entityToSave)
        return ProductResponseDto.fromEntity(saved)
    }

    @Transactional
    fun deleteProduct(id: String) {
        val entity = productRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow { ResourceNotFoundException("Produto com ID '$id' não encontrado para exclusão.", "Product", id) }

        // Soft delete para manter coerência com o sync CRDT
        entity.isDeleted = true
        entity.updatedAt = System.currentTimeMillis()
        productRepository.save(entity)
        logger.info("Produto $id marcado como excluído (soft-delete).")
    }
}

typealias ProdutoService = ProductService
