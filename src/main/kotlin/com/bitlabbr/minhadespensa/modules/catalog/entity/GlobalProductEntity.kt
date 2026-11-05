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

package com.bitlabbr.minhadespensa.modules.catalog.entity

import com.bitlabbr.minhadespensa.modules.catalog.domain.MeasureUnit
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(
    name = "global_products",
    indexes = [
        Index(name = "ux_global_products_ean", columnList = "ean", unique = true),
        Index(name = "idx_global_products_normalized_name", columnList = "normalized_name"),
        Index(name = "idx_global_products_brand", columnList = "brand"),
        Index(name = "idx_global_products_updated_at", columnList = "updated_at")
    ]
)
class GlobalProductEntity(
    @Id
    @Column(name = "id", length = 36, nullable = false)
    var id: String = UUID.randomUUID().toString(),

    @Column(name = "ean", length = 32, nullable = false, unique = true)
    var ean: String = "",

    @Column(name = "name", length = 255, nullable = false)
    var name: String = "",

    @Column(name = "normalized_name", length = 255, nullable = false)
    var normalizedName: String = "",

    @Column(name = "brand", length = 80, nullable = true)
    var brand: String? = null,

    @Column(name = "category", length = 100, nullable = false)
    var category: String = "Outros",

    @Enumerated(EnumType.STRING)
    @Column(name = "measure_unit", length = 30, nullable = false)
    var measureUnit: MeasureUnit = MeasureUnit.UNIT,

    @Column(name = "net_weight", nullable = false)
    var netWeight: Double = 1.0,

    @Column(name = "source", length = 50, nullable = false)
    var source: String = "USER_SYNC",

    @Column(name = "occurrence_count", nullable = false)
    var occurrenceCount: Long = 1L,

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = System.currentTimeMillis(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = System.currentTimeMillis()
)
