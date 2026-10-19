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

package com.bitlabbr.minhadespensa.modules.pantry.entity

import com.bitlabbr.minhadespensa.common.entity.BaseSyncEntity
import com.bitlabbr.minhadespensa.modules.pantry.domain.PantryItemStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(
    name = "pantry_items",
    indexes = [
        Index(name = "idx_pantry_product_id", columnList = "product_id"),
        Index(name = "idx_pantry_status", columnList = "status"),
        Index(name = "idx_pantry_updated_at", columnList = "updated_at"),
        Index(name = "idx_pantry_is_deleted", columnList = "is_deleted")
    ]
)
class PantryItemEntity(
    id: String = UUID.randomUUID().toString(),

    @Column(name = "product_id", nullable = false, length = 36)
    var productId: String = "",

    @Column(name = "quantity", nullable = false)
    var quantity: Double = 1.0,

    @Column(name = "unit_price", nullable = true)
    var unitPrice: Double? = null,

    @Column(name = "purchase_date", nullable = true)
    var purchaseDate: Long? = null,

    @Column(name = "expiration_date", nullable = true)
    var expirationDate: Long? = null,

    @Column(name = "purchase_location", length = 255, nullable = true)
    var purchaseLocation: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: PantryItemStatus = PantryItemStatus.EM_ESTOQUE,

    @Column(name = "batch_number", length = 100, nullable = true)
    var batchNumber: String? = null,

    updatedAt: Long = System.currentTimeMillis(),
    isDeleted: Boolean = false
) : BaseSyncEntity(id = id, updatedAt = updatedAt, isDeleted = isDeleted)

typealias ItemDespensa = PantryItemEntity
