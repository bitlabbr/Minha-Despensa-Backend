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
