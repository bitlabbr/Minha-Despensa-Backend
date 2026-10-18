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
