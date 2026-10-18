package com.bitlabbr.minhadespensa.modules.price.repository

import com.bitlabbr.minhadespensa.modules.price.entity.PriceEntryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PriceEntryRepository : JpaRepository<PriceEntryEntity, String> {

    fun findAllByProductIdAndIsDeletedFalseOrderByUpdatedAtDesc(productId: String): List<PriceEntryEntity>

    fun findByUpdatedAtGreaterThan(updatedAt: Long): List<PriceEntryEntity>
}
