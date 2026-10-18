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
