package com.bitlabbr.minhadespensa.modules.shoppinglist.repository

import com.bitlabbr.minhadespensa.modules.shoppinglist.entity.ShoppingListEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ShoppingListRepository : JpaRepository<ShoppingListEntity, String> {

    fun findByIdAndIsDeletedFalse(id: String): Optional<ShoppingListEntity>

    fun findAllByIsDeletedFalse(): List<ShoppingListEntity>

    fun findByUpdatedAtGreaterThan(updatedAt: Long): List<ShoppingListEntity>
}
