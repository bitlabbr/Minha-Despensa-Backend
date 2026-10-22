/*
 *   Copyright (c) 2026 Willian Santos
 *
 *   This work is licensed under the Creative Commons
 *   Attribution-NonCommercial 4.0 International License (CC BY-NC 4.0).
 *
 *   You are free to:
 *     - Share  — copy and redistribute the material in any medium or format
 *     - Adapt  — remix, transform, and build upon the material
 *
 *   Under the following terms:
 *     - Attribution    — You must give appropriate credit, provide a link to
 *                        the license, and indicate if changes were made.
 *     - NonCommercial  — You may not use the material for commercial purposes.
 *
 *   Owner rights:
 *     - Willian Santos retains all commercial rights.
 *    - The copyright holder may use, sell, sublicense, or relicense this
 *       work under different terms at any time.
 *
 *   Full license: https://creativecommons.org/licenses/by-nc/4.0/legalcode
 */

package com.bitlabbr.minhadespensa.modules.catalog.service

import com.bitlabbr.minhadespensa.common.exception.InvalidRequestException
import com.bitlabbr.minhadespensa.modules.catalog.entity.ProductMediaEntity
import com.bitlabbr.minhadespensa.modules.catalog.repository.ProductMediaRepository
import com.bitlabbr.minhadespensa.modules.catalog.repository.ProductRepository
import com.bitlabbr.minhadespensa.modules.sync.repository.EntityIdAliasRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest

@Service
class ProductMediaService(
    private val productMediaRepository: ProductMediaRepository,
    private val productRepository: ProductRepository,
    private val aliasRepository: EntityIdAliasRepository
) {

    private val logger = LoggerFactory.getLogger(ProductMediaService::class.java)
    private val maxSizeBytes = 200 * 1024 // 200 KB

    @Transactional
    fun saveMedia(
        householdId: String,
        productId: String,
        bytes: ByteArray,
        contentType: String,
        syncVersion: Long
    ): ProductMediaEntity {
        if (bytes.size > maxSizeBytes) {
            throw InvalidRequestException("Image exceeds maximum allowed size of 200KB (received: ${bytes.size / 1024}KB)")
        }

        val canonicalProductId = aliasRepository.findCanonicalId(householdId, "PRODUCT", productId) ?: productId
        val sha256Hash = computeSha256(bytes)
        val now = System.currentTimeMillis()

        val existingOpt = productMediaRepository.findByHouseholdIdAndProductId(householdId, canonicalProductId)
        val entity = if (existingOpt.isPresent) {
            val media = existingOpt.get()
            media.contentType = contentType
            media.sha256Hash = sha256Hash
            media.imageBytes = bytes
            media.updatedAt = now
            media.serverVersion = syncVersion
            media
        } else {
            ProductMediaEntity(
                householdId = householdId,
                productId = canonicalProductId,
                contentType = contentType,
                sha256Hash = sha256Hash,
                imageBytes = bytes,
                updatedAt = now,
                serverVersion = syncVersion
            )
        }

        val saved = productMediaRepository.save(entity)

        // Atualiza o imageHash no produto correspondente se ele existir
        val productOpt = productRepository.findByHouseholdIdAndId(householdId, canonicalProductId)
        if (productOpt.isPresent) {
            val product = productOpt.get()
            product.imageHash = sha256Hash
            product.serverVersion = syncVersion
            product.updatedAt = now
            productRepository.save(product)
        }

        logger.info("Media saved for product $productId (canonical=$canonicalProductId) in household $householdId (hash=$sha256Hash, size=${bytes.size} bytes)")
        return saved
    }

    @Transactional(readOnly = true)
    fun getMedia(householdId: String, productId: String): ProductMediaEntity? {
        val canonicalProductId = aliasRepository.findCanonicalId(householdId, "PRODUCT", productId) ?: productId
        return productMediaRepository.findByHouseholdIdAndProductId(householdId, canonicalProductId).orElse(null)
    }

    @Transactional
    fun deleteMedia(householdId: String, productId: String, syncVersion: Long = 0L) {
        val canonicalProductId = aliasRepository.findCanonicalId(householdId, "PRODUCT", productId) ?: productId
        productMediaRepository.deleteByHouseholdIdAndProductId(householdId, canonicalProductId)
        val productOpt = productRepository.findByHouseholdIdAndId(householdId, canonicalProductId)
        if (productOpt.isPresent) {
            val product = productOpt.get()
            product.imageHash = null
            product.serverVersion = syncVersion
            product.updatedAt = System.currentTimeMillis()
            productRepository.save(product)
        }
        logger.info("Media removed for product $productId (canonical=$canonicalProductId) in household $householdId (version=$syncVersion)")
    }

    private fun computeSha256(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
