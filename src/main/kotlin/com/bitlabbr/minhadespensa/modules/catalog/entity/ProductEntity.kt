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

import com.bitlabbr.minhadespensa.common.entity.BaseSyncEntity
import com.bitlabbr.minhadespensa.modules.catalog.domain.MeasureUnit
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(
    name = "catalog_products",
    indexes = [
        Index(name = "idx_product_ean", columnList = "ean"),
        Index(name = "idx_product_updated_at", columnList = "updated_at"),
        Index(name = "idx_product_is_deleted", columnList = "is_deleted")
    ]
)
class ProductEntity(
    id: String = UUID.randomUUID().toString(),

    @Column(name = "ean", length = 32, nullable = true)
    var ean: String? = null,

    @Column(name = "name", nullable = false, length = 255)
    var name: String = "",

    @Column(name = "category", nullable = false, length = 100)
    var category: String = "Geral",

    @Column(name = "brand", length = 150, nullable = true)
    var brand: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "measure_unit", nullable = false, length = 30)
    var measureUnit: MeasureUnit = MeasureUnit.UNIT,

    @Column(name = "net_weight", nullable = false)
    var netWeight: Double = 1.0,

    @Column(name = "thumbnail_url", length = 1024, nullable = true)
    var thumbnailUrl: String? = null,

    @Column(name = "manually_added", nullable = false)
    var manuallyAdded: Boolean = true,

    @Column(name = "notes", length = 2000, nullable = true)
    var notes: String? = null,

    updatedAt: Long = System.currentTimeMillis(),
    isDeleted: Boolean = false
) : BaseSyncEntity(id = id, updatedAt = updatedAt, isDeleted = isDeleted)

typealias Produto = ProductEntity
