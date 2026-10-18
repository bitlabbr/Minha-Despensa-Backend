package com.bitlabbr.minhadespensa.modules.shoppinglist.entity

import com.bitlabbr.minhadespensa.common.entity.BaseSyncEntity
import com.bitlabbr.minhadespensa.modules.shoppinglist.domain.ShoppingListStatus
import com.bitlabbr.minhadespensa.modules.shoppinglist.domain.ShoppingListType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(
    name = "shopping_lists",
    indexes = [
        Index(name = "idx_shopping_list_status", columnList = "status"),
        Index(name = "idx_shopping_list_updated_at", columnList = "updated_at"),
        Index(name = "idx_shopping_list_is_deleted", columnList = "is_deleted")
    ]
)
class ShoppingListEntity(
    id: String = UUID.randomUUID().toString(),

    @Column(name = "name", nullable = false, length = 255)
    var name: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    var type: ShoppingListType = ShoppingListType.PLANNED,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: ShoppingListStatus = ShoppingListStatus.DRAFT,

    @Column(name = "budget_in_cents", nullable = true)
    var budgetInCents: Long? = null,

    updatedAt: Long = System.currentTimeMillis(),
    isDeleted: Boolean = false
) : BaseSyncEntity(id = id, updatedAt = updatedAt, isDeleted = isDeleted)
