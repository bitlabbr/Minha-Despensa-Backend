package com.bitlabbr.minhadespensa.modules.pantry.repository

import com.bitlabbr.minhadespensa.modules.pantry.domain.PantryItemStatus
import com.bitlabbr.minhadespensa.modules.pantry.entity.PantryItemEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface PantryItemRepository : JpaRepository<PantryItemEntity, String> {

    fun findByIdAndIsDeletedFalse(id: String): Optional<PantryItemEntity>

    fun findAllByIsDeletedFalse(): List<PantryItemEntity>

    fun findAllByProductIdAndIsDeletedFalse(productId: String): List<PantryItemEntity>

    fun findByUpdatedAtGreaterThan(updatedAt: Long): List<PantryItemEntity>

    fun findAllByStatusAndIsDeletedFalse(status: PantryItemStatus): List<PantryItemEntity>

    fun findAllByProductIdAndStatusAndIsDeletedFalse(productId: String, status: PantryItemStatus): List<PantryItemEntity>
}

typealias ItemDespensaRepository = PantryItemRepository
