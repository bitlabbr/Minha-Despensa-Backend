package com.bitlabbr.minhadespensa.modules.price.service

import com.bitlabbr.minhadespensa.modules.price.dto.PriceEntryRequestDto
import com.bitlabbr.minhadespensa.modules.price.dto.PriceEntryResponseDto
import com.bitlabbr.minhadespensa.modules.price.entity.PriceEntryEntity
import com.bitlabbr.minhadespensa.modules.price.repository.PriceEntryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PriceEntryService(
    private val priceEntryRepository: PriceEntryRepository
) {
    private val logger = LoggerFactory.getLogger(PriceEntryService::class.java)

    @Transactional(readOnly = true)
    fun getPricesByProductId(productId: String): List<PriceEntryResponseDto> {
        return priceEntryRepository.findAllByProductIdAndIsDeletedFalseOrderByUpdatedAtDesc(productId)
            .map { PriceEntryResponseDto.fromEntity(it) }
    }

    @Transactional
    fun savePrice(dto: PriceEntryRequestDto): PriceEntryResponseDto {
        val targetId = dto.id ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val incomingUpdatedAt = dto.updatedAt ?: now

        val existingOpt = priceEntryRepository.findById(targetId)

        val entityToSave = if (existingOpt.isPresent) {
            val existing = existingOpt.get()
            if (incomingUpdatedAt >= existing.updatedAt) {
                existing.productId = dto.productId
                existing.priceInCents = dto.priceInCents
                existing.storeName = dto.storeName
                existing.updatedAt = incomingUpdatedAt
                existing.isDeleted = false
                existing
            } else {
                logger.info("Ignorando atualização do histórico de preço $targetId devido à regra LWW")
                existing
            }
        } else {
            PriceEntryEntity(
                id = targetId,
                productId = dto.productId,
                priceInCents = dto.priceInCents,
                storeName = dto.storeName,
                updatedAt = incomingUpdatedAt,
                isDeleted = false
            )
        }

        val saved = priceEntryRepository.save(entityToSave)
        return PriceEntryResponseDto.fromEntity(saved)
    }

    @Transactional(readOnly = true)
    fun getPricesModifiedSince(since: Long): List<PriceEntryEntity> {
        return priceEntryRepository.findByUpdatedAtGreaterThan(since)
    }
}
