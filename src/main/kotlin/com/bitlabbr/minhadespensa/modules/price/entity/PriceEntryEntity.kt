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

package com.bitlabbr.minhadespensa.modules.price.entity

import com.bitlabbr.minhadespensa.common.entity.BaseSyncEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(
    name = "price_entries",
    indexes = [
        Index(name = "idx_price_product_id", columnList = "product_id"),
        Index(name = "idx_price_updated_at", columnList = "updated_at"),
        Index(name = "idx_price_is_deleted", columnList = "is_deleted")
    ]
)
class PriceEntryEntity(
    id: String = UUID.randomUUID().toString(),

    @Column(name = "product_id", nullable = false, length = 36)
    var productId: String = "",

    @Column(name = "price_in_cents", nullable = false)
    var priceInCents: Long = 0L,

    @Column(name = "store_name", length = 150, nullable = true)
    var storeName: String? = null,

    updatedAt: Long = System.currentTimeMillis(),
    isDeleted: Boolean = false
) : BaseSyncEntity(id = id, updatedAt = updatedAt, isDeleted = isDeleted)
