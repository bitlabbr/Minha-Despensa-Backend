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
import com.bitlabbr.minhadespensa.modules.pantry.domain.MovementType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "pantry_movements")
@IdClass(HouseholdEntityId::class)
class PantryMovementEntity(
    householdId: String = "",
    id: String = UUID.randomUUID().toString(),

    @Column(name = "pantry_item_id", length = 36)
    var pantryItemId: String? = null,

    @Column(name = "product_id", nullable = false, length = 36)
    var productId: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    var type: MovementType = MovementType.ENTRY_MANUAL,

    @Column(name = "quantity", nullable = false)
    var quantity: Double = 1.0,

    @Column(name = "unit_price_in_cents")
    var unitPriceInCents: Long? = null,

    @Column(name = "total_price_in_cents")
    var totalPriceInCents: Long? = null,

    @Column(name = "store_or_reason", length = 255)
    var storeOrReason: String? = null,

    updatedAt: Long = System.currentTimeMillis(),
    serverVersion: Long = 0L,
    isDeleted: Boolean = false
) : HouseholdScopedSyncEntity(
    householdId = householdId,
    id = id,
    updatedAt = updatedAt,
    serverVersion = serverVersion,
    isDeleted = isDeleted
)
