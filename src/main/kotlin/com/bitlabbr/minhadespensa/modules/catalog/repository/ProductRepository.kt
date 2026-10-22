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

import com.bitlabbr.minhadespensa.common.entity.HouseholdEntityId
import com.bitlabbr.minhadespensa.modules.catalog.domain.MeasureUnit
import com.bitlabbr.minhadespensa.modules.catalog.entity.ProductEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ProductRepository : JpaRepository<ProductEntity, HouseholdEntityId> {

    fun findByHouseholdIdAndId(householdId: String, id: String): Optional<ProductEntity>

    fun findByHouseholdIdAndIdAndIsDeletedFalse(householdId: String, id: String): Optional<ProductEntity>

    fun findByHouseholdIdAndEan(householdId: String, ean: String): Optional<ProductEntity>

    fun findByHouseholdIdAndEanAndIsDeletedFalse(householdId: String, ean: String): Optional<ProductEntity>

    fun findByHouseholdIdAndNormalizedNameAndMeasureUnitAndEanIsNull(
        householdId: String,
        normalizedName: String,
        measureUnit: MeasureUnit
    ): Optional<ProductEntity>

    fun findByHouseholdIdAndNormalizedNameAndMeasureUnitAndEanIsNullAndIsDeletedFalse(
        householdId: String,
        normalizedName: String,
        measureUnit: MeasureUnit
    ): Optional<ProductEntity>

    fun findByHouseholdIdAndIsDeletedFalse(householdId: String): List<ProductEntity>

    fun findByHouseholdIdAndServerVersionGreaterThanOrderByServerVersionAsc(
        householdId: String,
        serverVersion: Long
    ): List<ProductEntity>

    fun existsByHouseholdIdAndEanAndIdNotAndIsDeletedFalse(
        householdId: String,
        ean: String,
        id: String
    ): Boolean

    @Query(
        """
        SELECT p FROM ProductEntity p 
        WHERE p.householdId = :householdId
          AND p.isDeleted = false 
          AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) 
               OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%'))
               OR p.ean = :query)
        ORDER BY p.name ASC
        """
    )
    fun searchProducts(
        @Param("householdId") householdId: String,
        @Param("query") query: String
    ): List<ProductEntity>
}

typealias ProdutoRepository = ProductRepository
