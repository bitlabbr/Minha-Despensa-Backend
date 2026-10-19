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

package com.bitlabbr.minhadespensa.modules.shoppinglist.entity

import com.bitlabbr.minhadespensa.common.entity.BaseSyncEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(
    name = "shopping_items",
    indexes = [
        Index(name = "idx_shopping_item_list_id", columnList = "list_id"),
        Index(name = "idx_shopping_item_product_id", columnList = "product_id"),
        Index(name = "idx_shopping_item_updated_at", columnList = "updated_at"),
        Index(name = "idx_shopping_item_is_deleted", columnList = "is_deleted")
    ]
)
class ShoppingItemEntity(
    id: String = UUID.randomUUID().toString(),

    @Column(name = "list_id", nullable = false, length = 36)
    var listId: String = "",

    @Column(name = "product_id", length = 36, nullable = true)
    var productId: String? = null,

    @Column(name = "raw_text", length = 500, nullable = true)
    var rawText: String? = null,

    @Column(name = "quantity", nullable = false)
    var quantity: Double = 1.0,

    @Column(name = "price_at_time", nullable = true)
    var priceAtTime: Long? = null,

    @Column(name = "is_checked", nullable = false)
    var isChecked: Boolean = false,

    updatedAt: Long = System.currentTimeMillis(),
    isDeleted: Boolean = false
) : BaseSyncEntity(id = id, updatedAt = updatedAt, isDeleted = isDeleted)
