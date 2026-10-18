package com.bitlabbr.minhadespensa.modules.shoppinglist.repository

import com.bitlabbr.minhadespensa.modules.shoppinglist.entity.ShoppingItemEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ShoppingItemRepository : JpaRepository<ShoppingItemEntity, String> {

    fun findByIdAndIsDeletedFalse(id: String): Optional<ShoppingItemEntity>

    fun findAllByListIdAndIsDeletedFalse(listId: String): List<ShoppingItemEntity>

    fun findByUpdatedAtGreaterThan(updatedAt: Long): List<ShoppingItemEntity>
}
