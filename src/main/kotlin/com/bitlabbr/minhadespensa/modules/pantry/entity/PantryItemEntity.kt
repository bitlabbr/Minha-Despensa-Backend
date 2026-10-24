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

import com.bitlabbr.minhadespensa.common.entity.HouseholdEntityId
import com.bitlabbr.minhadespensa.common.entity.HouseholdScopedSyncEntity
import com.bitlabbr.minhadespensa.modules.pantry.domain.PantryItemStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.IdClass
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(
    name = "pantry_items",
    indexes = [
        Index(name = "idx_pantry_items_product", columnList = "household_id, product_id"),
        Index(name = "idx_pantry_items_server_version", columnList = "household_id, server_version")
    ]
)
@IdClass(HouseholdEntityId::class)
class PantryItemEntity(
    householdId: String = "",
    id: String = UUID.randomUUID().toString(),

    @Column(name = "product_id", nullable = false, length = 36)
    var productId: String = "",

    @Column(name = "quantity", nullable = false)
    var quantity: Double = 1.0,

    @Column(name = "expiration_date", nullable = true)
    var expirationDate: Long? = null,

    @Column(name = "batch_number", length = 100, nullable = true)
    var batchNumber: String? = null,

    @Column(name = "unit_price_in_cents", nullable = true)
    var unitPriceInCents: Long? = null,

    updatedAt: Long = System.currentTimeMillis(),
    serverVersion: Long = 0L,
    isDeleted: Boolean = false
) : HouseholdScopedSyncEntity(
    householdId = householdId,
    id = id,
    updatedAt = updatedAt,
    serverVersion = serverVersion,
    isDeleted = isDeleted
) {
    val unitPrice: Double?
        get() = unitPriceInCents?.let { it / 100.0 }

    val status: PantryItemStatus
        get() = if (quantity <= 0.0) PantryItemStatus.CONSUMIDO else PantryItemStatus.EM_ESTOQUE
}

typealias ItemDespensa = PantryItemEntity
