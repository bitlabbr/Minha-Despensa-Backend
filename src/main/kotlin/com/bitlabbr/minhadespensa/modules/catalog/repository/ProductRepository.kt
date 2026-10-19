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

package com.bitlabbr.minhadespensa.modules.catalog.repository

import com.bitlabbr.minhadespensa.modules.catalog.entity.ProductEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ProductRepository : JpaRepository<ProductEntity, String> {

    fun findByIdAndIsDeletedFalse(id: String): Optional<ProductEntity>

    fun findByEanAndIsDeletedFalse(ean: String): Optional<ProductEntity>

    fun findAllByIsDeletedFalse(): List<ProductEntity>

    fun findByUpdatedAtGreaterThan(updatedAt: Long): List<ProductEntity>

    @Query(
        """
        SELECT p FROM ProductEntity p 
        WHERE p.isDeleted = false 
          AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) 
               OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%'))
               OR p.ean = :query)
        ORDER BY p.name ASC
        """
    )
    fun searchProducts(@Param("query") query: String): List<ProductEntity>

    fun existsByEanAndIdNotAndIsDeletedFalse(ean: String, id: String): Boolean
}

typealias ProdutoRepository = ProductRepository
