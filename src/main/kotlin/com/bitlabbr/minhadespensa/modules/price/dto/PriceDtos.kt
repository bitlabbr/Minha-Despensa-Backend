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

package com.bitlabbr.minhadespensa.modules.price.dto

import com.bitlabbr.minhadespensa.modules.price.entity.PriceEntryEntity
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

@Schema(description = "Dados para cadastro de registro de preço")
data class PriceEntryRequestDto(
    @field:Schema(description = "ID do registro (UUID)", example = "6fa85f64-5717-4562-b3fc-2c963f66afa9")
    val id: String? = null,

    @field:Schema(description = "ID do produto associado", example = "a1b2c3d4-e5f6-7890-1234-56789abcdef0")
    @field:NotBlank(message = "O ID do produto é obrigatório")
    val productId: String,

    @field:Schema(description = "Preço unitário em centavos", example = "1290")
    @field:PositiveOrZero(message = "O preço deve ser zero ou positivo")
    val priceInCents: Long,

    @field:Schema(description = "Nome do estabelecimento / supermercado", example = "Supermercado Pão de Açúcar")
    @field:Size(max = 150, message = "O nome do estabelecimento não pode exceder 150 caracteres")
    val storeName: String? = null,

    @field:Schema(description = "Timestamp da última modificação")
    val updatedAt: Long? = null
)

@Schema(description = "Dados do registro de preço retornado pela API")
data class PriceEntryResponseDto(
    @Schema(description = "ID do registro", example = "6fa85f64-5717-4562-b3fc-2c963f66afa9")
    val id: String,

    @Schema(description = "ID do produto", example = "a1b2c3d4-e5f6-7890-1234-56789abcdef0")
    val productId: String,

    @Schema(description = "Preço em centavos", example = "1290")
    val priceInCents: Long,

    @Schema(description = "Nome do estabelecimento", example = "Supermercado Pão de Açúcar")
    val storeName: String?,

    @Schema(description = "Timestamp da última modificação", example = "1727280000000")
    val updatedAt: Long,

    @Schema(description = "Indica se foi excluído", example = "false")
    val isDeleted: Boolean
) {
    companion object {
        fun fromEntity(entity: PriceEntryEntity): PriceEntryResponseDto =
            PriceEntryResponseDto(
                id = entity.id,
                productId = entity.productId,
                priceInCents = entity.priceInCents,
                storeName = entity.storeName,
                updatedAt = entity.updatedAt,
                isDeleted = entity.isDeleted
            )
    }
}
