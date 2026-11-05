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

package com.bitlabbr.minhadespensa.modules.catalog.dto

import com.bitlabbr.minhadespensa.modules.catalog.domain.MeasureUnit
import com.bitlabbr.minhadespensa.modules.catalog.entity.GlobalProductEntity
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Representação detalhada de um produto no catálogo global centralizado")
data class GlobalProductResponseDto(
    @Schema(description = "Identificador único do registro global", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    val id: String,

    @Schema(description = "Código de barras universal (EAN / GTIN)", example = "7891000100103")
    val ean: String,

    @Schema(description = "Nome do produto", example = "Leite Condensado Moça")
    val name: String,

    @Schema(description = "Marca do produto", example = "Nestlé")
    val brand: String?,

    @Schema(description = "Categoria do produto", example = "Laticínios")
    val category: String,

    @Schema(description = "Unidade de medida padronizada", example = "GRAM")
    val measureUnit: MeasureUnit,

    @Schema(description = "Peso líquido ou quantidade declarada", example = "395.0")
    val netWeight: Double,

    @Schema(description = "Origem da ingestão (SEFAZ_INVOICE, USER_SYNC)", example = "SEFAZ_INVOICE")
    val source: String,

    @Schema(description = "Quantidade de vezes que este produto foi catalogado ou lido", example = "12")
    val occurrenceCount: Long,

    @Schema(description = "Timestamp da última atualização (epoch millis)", example = "1730761200000")
    val updatedAt: Long
) {
    companion object {
        fun fromEntity(entity: GlobalProductEntity): GlobalProductResponseDto {
            return GlobalProductResponseDto(
                id = entity.id,
                ean = entity.ean,
                name = entity.name,
                brand = entity.brand,
                category = entity.category,
                measureUnit = entity.measureUnit,
                netWeight = entity.netWeight,
                source = entity.source,
                occurrenceCount = entity.occurrenceCount,
                updatedAt = entity.updatedAt
            )
        }
    }
}
