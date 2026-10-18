package com.bitlabbr.minhadespensa.common.entity

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import java.util.UUID

@MappedSuperclass
abstract class BaseSyncEntity(
    @Id
    @Column(name = "id", nullable = false, length = 36)
    open var id: String = UUID.randomUUID().toString(),

    @Column(name = "updated_at", nullable = false)
    open var updatedAt: Long = System.currentTimeMillis(),

    @Column(name = "is_deleted", nullable = false)
    open var isDeleted: Boolean = false
)
