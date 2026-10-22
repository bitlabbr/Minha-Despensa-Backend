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

package com.bitlabbr.minhadespensa.modules.pantry.dto

import com.bitlabbr.minhadespensa.modules.catalog.domain.MeasureUnit
import com.bitlabbr.minhadespensa.modules.catalog.entity.ProductEntity
import com.bitlabbr.minhadespensa.modules.pantry.domain.PantryItemStatus
import com.bitlabbr.minhadespensa.modules.pantry.entity.PantryItemEntity
import com.fasterxml.jackson.annotation.JsonAlias
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import tools.jackson.databind.annotation.JsonDeserialize

@Schema(description = "Resumo cadastral do produto associado ao estoque na despensa")
data class ProductSummaryDto(
    @Schema(description = "ID único do produto", example = "a1b2c3d4-e5f6-7890-1234-56789abcdef0")
    val id: String,

    @Schema(description = "Nome do produto", example = "Leite Integral Ninho 1L")
    val nome: String,

    @Schema(description = "Código de barras", example = "7891000100103")
    val codigoBarras: String?,

    @Schema(description = "Marca do produto", example = "Nestlé")
    val marca: String?,

    @Schema(description = "Categoria do produto", example = "Laticínios")
    val categoria: String,

    @Schema(description = "Unidade de medida", example = "UN")
    val unidadeMedida: String
) {
    companion object {
        fun fromEntity(entity: ProductEntity): ProductSummaryDto =
            ProductSummaryDto(
                id = entity.id,
                nome = entity.name,
                codigoBarras = entity.ean,
                marca = entity.brand,
                categoria = entity.category,
                unidadeMedida = when (entity.measureUnit) {
                    MeasureUnit.UNIT -> "UN"
                    MeasureUnit.KILOGRAM -> "KG"
                    MeasureUnit.GRAM -> "G"
                    MeasureUnit.LITER -> "L"
                    MeasureUnit.MILLILITER -> "ML"
                    MeasureUnit.PACKAGE -> "PCT"
                }
            )

        fun fallback(productId: String): ProductSummaryDto =
            ProductSummaryDto(
                id = productId,
                nome = "Produto não cadastrado",
                codigoBarras = null,
                marca = null,
                categoria = "Geral",
                unidadeMedida = "UN"
            )
    }
}

@Schema(description = "Representação de uma ocorrência ou lote de compra de um produto na despensa")
data class PantryOccurrenceResponseDto(
    @Schema(description = "ID único do lote / item de despensa", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    val id: String,

    @Schema(description = "Quantidade disponível neste lote", example = "2.0")
    val quantidade: Double,

    @Schema(description = "Preço unitário pago pelo item na compra", example = "5.50")
    val precoUnitario: Double?,

    @Schema(description = "Data da compra em formato ISO-8601 (yyyy-MM-dd)", example = "2026-08-10")
    val dataCompra: String?,

    @Schema(description = "Data de validade do lote em formato ISO-8601 (yyyy-MM-dd)", example = "2026-09-20")
    val dataValidade: String?,

    @Schema(description = "Local da compra (estabelecimento)", example = "Carrefour Hipermercado")
    val localCompra: String?,

    @Schema(description = "Status do lote na despensa", example = "EM_ESTOQUE")
    val status: PantryItemStatus,

    @Schema(description = "Número do lote de fabricação", example = "LOTE-2026-X")
    val batchNumber: String? = null,

    @Schema(description = "Timestamp da data de compra em milissegundos UTC", example = "1786320000000")
    val purchaseDate: Long? = null,

    @Schema(description = "Timestamp da data de validade em milissegundos UTC", example = "1789862400000")
    val expirationDate: Long? = null,

    @Schema(description = "Timestamp da modificação em milissegundos UTC", example = "1727280000000")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromEntity(entity: PantryItemEntity): PantryOccurrenceResponseDto =
            PantryOccurrenceResponseDto(
                id = entity.id,
                quantidade = entity.quantity,
                precoUnitario = entity.unitPrice,
                dataCompra = null,
                dataValidade = entity.expirationDate.toIsoDateString(),
                localCompra = null,
                status = entity.status,
                batchNumber = entity.batchNumber,
                purchaseDate = null,
                expirationDate = entity.expirationDate,
                updatedAt = entity.updatedAt
            )
    }
}

typealias OcorrenciaResponse = PantryOccurrenceResponseDto

@Schema(description = "Resumo do estoque agregado de um produto na despensa")
data class PantryAggregatedResponseDto(
    @Schema(description = "Dados do produto catalogado")
    val produto: ProductSummaryDto,

    @Schema(description = "Quantidade total disponível somando todas as ocorrências ativas", example = "3.0")
    val quantidadeTotal: Double,

    @Schema(description = "Lista de ocorrências e lotes ativos deste produto")
    val ocorrencias: List<PantryOccurrenceResponseDto>
)

typealias DespensaAgrupadaResponse = PantryAggregatedResponseDto

@Schema(description = "Dados para criação ou atualização de um item da despensa")
data class PantryItemRequestDto(
    @Schema(description = "Identificador único do item (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    val id: String? = null,

    @Schema(description = "ID do produto associado no catálogo", example = "a1b2c3d4-e5f6-7890-1234-56789abcdef0")
    @NotBlank(message = "Product ID is required")
    @JsonAlias("produtoId", "produto_id")
    val productId: String,

    @Schema(description = "Quantidade disponível na despensa", example = "2.0")
    @PositiveOrZero(message = "Quantity must be zero or positive")
    @JsonAlias("quantidade")
    val quantity: Double = 1.0,

    @Schema(description = "Preço unitário em centavos", example = "550")
    @JsonAlias("unitPriceInCents", "unit_price_in_cents")
    val unitPriceInCents: Long? = null,

    @Schema(description = "Preço unitário pago pelo item na compra", example = "5.50")
    @JsonAlias("precoUnitario", "preco_unitario", "unit_price")
    val unitPrice: Double? = null,

    @Schema(description = "Data de compra (timestamp em millis ou yyyy-MM-dd)", example = "2026-08-10")
    @JsonDeserialize(using = EpochMillisOrDateDeserializer::class)
    @JsonAlias("dataCompra", "data_compra", "purchase_date")
    val purchaseDate: Long? = null,

    @Schema(description = "Data de validade do item (timestamp em millis ou yyyy-MM-dd)", example = "2026-09-20")
    @JsonDeserialize(using = EpochMillisOrDateDeserializer::class)
    @JsonAlias("dataValidade", "data_validade", "expiration_date")
    val expirationDate: Long? = null,

    @Schema(description = "Local da compra (supermercado ou loja)", example = "Carrefour Hipermercado")
    @JsonAlias("localCompra", "local_compra", "purchase_location")
    val purchaseLocation: String? = null,

    @Schema(description = "Status do item na despensa", example = "EM_ESTOQUE")
    val status: PantryItemStatus = PantryItemStatus.EM_ESTOQUE,

    @Schema(description = "Número do lote de fabricação", example = "LOTE-2026-X")
    val batchNumber: String? = null,

    @Schema(description = "Timestamp da modificação em milissegundos UTC")
    val updatedAt: Long? = null,

    @Schema(description = "Indica se o item foi marcado como excluído", example = "false")
    val isDeleted: Boolean = false
)

typealias ItemDespensaRequest = PantryItemRequestDto

@Schema(description = "Dados de um item da despensa retornado pela API")
data class PantryItemResponseDto(
    @Schema(description = "ID único do item (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    val id: String,

    @Schema(description = "ID do produto associado no catálogo", example = "a1b2c3d4-e5f6-7890-1234-56789abcdef0")
    val productId: String,

    @Schema(description = "Quantidade disponível na despensa", example = "2.0")
    val quantity: Double,

    @Schema(description = "Preço unitário em centavos", example = "550")
    val unitPriceInCents: Long? = null,

    @Schema(description = "Preço unitário pago pelo item na compra", example = "5.50")
    val unitPrice: Double? = null,

    @Schema(description = "Data de compra em milissegundos UTC", example = "1786320000000")
    val purchaseDate: Long? = null,

    @Schema(description = "Data de validade em milissegundos UTC", example = "1767139200000")
    val expirationDate: Long?,

    @Schema(description = "Local da compra", example = "Carrefour Hipermercado")
    val purchaseLocation: String? = null,

    @Schema(description = "Status do item na despensa", example = "EM_ESTOQUE")
    val status: PantryItemStatus = PantryItemStatus.EM_ESTOQUE,

    @Schema(description = "Número do lote", example = "LOTE-2026-X")
    val batchNumber: String?,

    @Schema(description = "Timestamp da modificação em milissegundos UTC", example = "1727280000000")
    val updatedAt: Long,

    @Schema(description = "Versão monotônica de sincronização da casa", example = "42")
    val serverVersion: Long = 0L,

    @Schema(description = "Indica se o item foi marcado como excluído", example = "false")
    val isDeleted: Boolean
) {
    companion object {
        fun fromEntity(entity: PantryItemEntity): PantryItemResponseDto =
            PantryItemResponseDto(
                id = entity.id,
                productId = entity.productId,
                quantity = entity.quantity,
                unitPriceInCents = entity.unitPriceInCents,
                unitPrice = entity.unitPrice,
                purchaseDate = null,
                expirationDate = entity.expirationDate,
                purchaseLocation = null,
                status = entity.status,
                batchNumber = entity.batchNumber,
                updatedAt = entity.updatedAt,
                serverVersion = entity.serverVersion,
                isDeleted = entity.isDeleted
            )
    }
}

typealias ItemDespensaResponse = PantryItemResponseDto
