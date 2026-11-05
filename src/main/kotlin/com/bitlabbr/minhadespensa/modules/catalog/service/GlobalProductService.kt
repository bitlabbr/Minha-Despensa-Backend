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

import com.bitlabbr.minhadespensa.common.exception.ResourceNotFoundException
import com.bitlabbr.minhadespensa.common.exception.ValidationException
import com.bitlabbr.minhadespensa.common.util.DeterministicIdUtils
import com.bitlabbr.minhadespensa.modules.catalog.domain.MeasureUnit
import com.bitlabbr.minhadespensa.modules.catalog.dto.GlobalProductResponseDto
import com.bitlabbr.minhadespensa.modules.catalog.entity.GlobalProductEntity
import com.bitlabbr.minhadespensa.modules.catalog.repository.GlobalProductRepository
import com.bitlabbr.minhadespensa.modules.invoice.dto.InvoiceItemResponseDto
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class GlobalProductService(
    private val globalProductRepository: GlobalProductRepository
) {
    private val logger = LoggerFactory.getLogger(GlobalProductService::class.java)

    companion object {
        private val VALID_EAN_LENGTHS = setOf(8, 12, 13, 14)
        private val IN_STORE_RESTRICTED_PREFIXES = (20..29).map { it.toString() }.toSet()
    }

    fun isValidEan(ean: String?): Boolean {
        if (ean.isNullOrBlank()) return false
        val clean = ean.trim()
        return clean.all { it.isDigit() } &&
            clean.length in VALID_EAN_LENGTHS &&
            !clean.all { it == '0' }
    }

    fun isIngestibleGlobalEan(ean: String?): Boolean {
        if (!isValidEan(ean)) return false
        val clean = ean!!.trim()
        // Ignora EAN-13 de circulação restrita interna de supermercado (etiquetas de balança/peso variável prefixo 20..29)
        if (clean.length == 13 && IN_STORE_RESTRICTED_PREFIXES.any { clean.startsWith(it) }) {
            return false
        }
        return true
    }

    internal fun mapInvoiceMeasureUnit(rawUnit: String?): MeasureUnit {
        val normalized = rawUnit?.trim()?.uppercase() ?: return MeasureUnit.UNIT
        return when (normalized) {
            "KG", "KILO", "QUILO", "KILOGRAM", "KILOGRAMA" -> MeasureUnit.KILOGRAM
            "G", "GR", "GRAM", "GRAMA", "GRAMAS" -> MeasureUnit.GRAM
            "L", "LT", "LITRO", "LITROS", "LITER" -> MeasureUnit.LITER
            "ML", "MILILITRO", "MILLILITER" -> MeasureUnit.MILLILITER
            "PCT", "PC", "PACOTE", "CX", "CAIXA", "FD", "FARDO", "PACKAGE", "EMB" -> MeasureUnit.PACKAGE
            "UN", "UND", "UNID", "UNIDADE", "UNIT" -> MeasureUnit.UNIT
            else -> runCatching { MeasureUnit.valueOf(normalized) }.getOrDefault(MeasureUnit.UNIT)
        }
    }

    @Transactional(readOnly = true)
    fun getProductByEan(ean: String): GlobalProductResponseDto {
        val cleanEan = ean.trim()
        if (!isValidEan(cleanEan)) {
            throw ValidationException("Código EAN inválido: '$ean'. Deve conter 8, 12, 13 ou 14 dígitos numéricos válidos.")
        }
        val entity = globalProductRepository.findByEan(cleanEan)
            .orElseThrow { ResourceNotFoundException("Produto global com EAN '$cleanEan' não encontrado no catálogo centralizado.") }
        return GlobalProductResponseDto.fromEntity(entity)
    }

    @Transactional(readOnly = true)
    fun searchProducts(query: String, limit: Int = 20): List<GlobalProductResponseDto> {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            return emptyList()
        }
        val pageable = PageRequest.of(0, limit.coerceIn(1, 50))
        val normalized = DeterministicIdUtils.normalizeText(trimmed)
        return globalProductRepository.searchProducts(normalized, pageable)
            .map { GlobalProductResponseDto.fromEntity(it) }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun ingestFromInvoiceItem(item: InvoiceItemResponseDto): GlobalProductEntity? {
        val rawEan = item.ean?.trim()
        if (!isIngestibleGlobalEan(rawEan)) {
            return null
        }
        val cleanEan = rawEan!!
        val name = item.name.trim()
        if (name.isBlank()) return null

        val unit = mapInvoiceMeasureUnit(item.measureUnit)

        return upsertGlobalProduct(
            ean = cleanEan,
            name = name,
            brand = null,
            category = "Geral",
            measureUnit = unit,
            netWeight = 1.0,
            source = "SEFAZ_INVOICE"
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun ingestFromUserProduct(
        ean: String?,
        name: String,
        brand: String?,
        category: String?,
        measureUnit: MeasureUnit,
        netWeight: Double
    ): GlobalProductEntity? {
        val cleanEan = ean?.trim()
        if (!isIngestibleGlobalEan(cleanEan)) return null
        val cleanName = name.trim()
        if (cleanName.isBlank()) return null

        return upsertGlobalProduct(
            ean = cleanEan!!,
            name = cleanName,
            brand = brand?.trim()?.takeIf { it.isNotEmpty() },
            category = category?.trim()?.takeIf { it.isNotEmpty() } ?: "Geral",
            measureUnit = measureUnit,
            netWeight = if (netWeight > 0.0) netWeight else 1.0,
            source = "USER_SYNC"
        )
    }

    @Transactional
    internal fun upsertGlobalProduct(
        ean: String,
        name: String,
        brand: String?,
        category: String,
        measureUnit: MeasureUnit,
        netWeight: Double,
        source: String
    ): GlobalProductEntity {
        val existingOpt = globalProductRepository.findByEan(ean)
        val now = System.currentTimeMillis()

        return if (existingOpt.isPresent) {
            val existing = existingOpt.get()
            existing.occurrenceCount += 1
            existing.updatedAt = now

            if (existing.brand.isNullOrBlank() && !brand.isNullOrBlank()) {
                existing.brand = brand
            }

            if ((existing.category == "Geral" || existing.category == "Outros") && category != "Geral" && category != "Outros") {
                existing.category = category
            }

            if (source == "USER_SYNC" && existing.source == "SEFAZ_INVOICE") {
                existing.name = name
                existing.normalizedName = DeterministicIdUtils.normalizeText(name)
                existing.source = "USER_SYNC"
            } else if (source == existing.source && name.length > existing.name.length && !name.contains("*")) {
                existing.name = name
                existing.normalizedName = DeterministicIdUtils.normalizeText(name)
            }

            if (existing.measureUnit == MeasureUnit.UNIT && measureUnit != MeasureUnit.UNIT) {
                existing.measureUnit = measureUnit
                existing.netWeight = netWeight
            }

            logger.info("[Catalog:Global] Enriched global product EAN={} ('{}', occurrences={}) from source={}", ean, existing.name, existing.occurrenceCount, source)
            globalProductRepository.save(existing)
        } else {
            val entity = GlobalProductEntity(
                ean = ean,
                name = name,
                normalizedName = DeterministicIdUtils.normalizeText(name),
                brand = brand,
                category = category,
                measureUnit = measureUnit,
                netWeight = netWeight,
                source = source,
                occurrenceCount = 1L,
                createdAt = now,
                updatedAt = now
            )
            val saved = globalProductRepository.save(entity)
            logger.info("[Catalog:Global] Ingested new global product EAN={} ('{}') from source={}", ean, name, source)
            saved
        }
    }
}
