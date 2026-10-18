package com.bitlabbr.minhadespensa.modules.catalog.dto

import com.bitlabbr.minhadespensa.modules.catalog.domain.MeasureUnit
import com.bitlabbr.minhadespensa.modules.catalog.entity.ProductEntity
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Representação detalhada de um produto do catálogo retornado pela API")
data class ProductResponseDto(
    @Schema(description = "ID único do produto (UUID)", example = "a1b2c3d4-e5f6-7890-1234-56789abcdef0")
    val id: String,

    @Schema(description = "Código de barras (EAN)", example = "7891000100103")
    val ean: String?,

    @Schema(description = "Nome do produto", example = "Arroz Branco Tipo 1 5kg")
    val name: String,

    @Schema(description = "Categoria do produto", example = "Mercearia")
    val category: String,

    @Schema(description = "Marca do produto", example = "Camil")
    val brand: String?,

    @Schema(description = "Unidade de medida", example = "KILOGRAM")
    val measureUnit: MeasureUnit,

    @Schema(description = "Peso líquido", example = "5.0")
    val netWeight: Double,

    @Schema(description = "URL da imagem miniatura", example = "https://cdn.despensa.app/p/arroz-camil.png")
    val thumbnailUrl: String?,

    @Schema(description = "Cadastrado manualmente", example = "true")
    val manuallyAdded: Boolean,

    @Schema(description = "Observações", example = "Preferir pacote lacrado")
    val notes: String?,

    @Schema(description = "Timestamp da última modificação (epoch millis UTC)", example = "1727280000000")
    val updatedAt: Long,

    @Schema(description = "Indica se o registro está marcado como excluído", example = "false")
    val isDeleted: Boolean
) {
    companion object {
        fun fromEntity(entity: ProductEntity): ProductResponseDto =
            ProductResponseDto(
                id = entity.id,
                ean = entity.ean,
                name = entity.name,
                category = entity.category,
                brand = entity.brand,
                measureUnit = entity.measureUnit,
                netWeight = entity.netWeight,
                thumbnailUrl = entity.thumbnailUrl,
                manuallyAdded = entity.manuallyAdded,
                notes = entity.notes,
                updatedAt = entity.updatedAt,
                isDeleted = entity.isDeleted
            )
    }
}

typealias ProdutoResponse = ProductResponseDto
