package com.bitlabbr.minhadespensa.modules.catalog.repository

import com.bitlabbr.minhadespensa.modules.catalog.entity.ProductEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ProductRepository : JpaRepository<ProductEntity, String> {

    fun findByIdAndIsDeletedFalse(id: String): Optional<ProductEntity>

    fun findByEanAndIsDeletedFalse(ean: String): Optional<ProductEntity>

    fun findAllByIsDeletedFalse(): List<ProductEntity>

    fun findByUpdatedAtGreaterThan(updatedAt: Long): List<ProductEntity>

    @Query(
        """
        SELECT p FROM ProductEntity p 
        WHERE p.isDeleted = false 
          AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) 
               OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%'))
               OR p.ean = :query)
        ORDER BY p.name ASC
        """
    )
    fun searchProducts(@Param("query") query: String): List<ProductEntity>

    fun existsByEanAndIdNotAndIsDeletedFalse(ean: String, id: String): Boolean
}

typealias ProdutoRepository = ProductRepository
