package com.bitlabbr.minhadespensa.modules.catalog.dto

import com.bitlabbr.minhadespensa.modules.catalog.domain.MeasureUnit
import com.fasterxml.jackson.annotation.JsonAlias
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

@Schema(description = "Payload para criação ou atualização de um produto do catálogo")
data class ProductRequestDto(
    @field:Schema(
        description = "Identificador único do produto (UUID). Se não informado, um novo ID será gerado.",
        example = "a1b2c3d4-e5f6-7890-1234-56789abcdef0"
    )
    val id: String? = null,

    @field:Schema(description = "Código de barras (EAN-8, EAN-13, EAN-14)", example = "7891000100103")
    @field:Size(max = 32, message = "O EAN não pode ter mais de 32 caracteres")
    @JsonAlias("codigoBarras", "codigo_barras")
    val ean: String? = null,

    @field:Schema(description = "Nome do produto", example = "Arroz Branco Tipo 1 5kg")
    @field:NotBlank(message = "O nome do produto é obrigatório")
    @field:Size(max = 255, message = "O nome não pode exceder 255 caracteres")
    @JsonAlias("nome")
    val name: String,

    @field:Schema(description = "Categoria do produto", example = "Mercearia")
    @field:NotBlank(message = "A categoria é obrigatória")
    @field:Size(max = 100, message = "A categoria não pode exceder 100 caracteres")
    @JsonAlias("categoria")
    val category: String = "Geral",

    @field:Schema(description = "Marca do produto", example = "Camil")
    @field:Size(max = 150, message = "A marca não pode exceder 150 caracteres")
    @JsonAlias("marca")
    val brand: String? = null,

    @field:Schema(description = "Unidade de medida", example = "KILOGRAM")
    @JsonAlias("unidadeMedida", "unidade_medida")
    val measureUnit: MeasureUnit = MeasureUnit.UNIT,

    @field:Schema(description = "Peso líquido ou quantidade nominal na embalagem", example = "5.0")
    @field:Positive(message = "O peso líquido deve ser maior que zero")
    @JsonAlias("pesoLiquido", "peso_liquido")
    val netWeight: Double = 1.0,

    @field:Schema(description = "URL da imagem miniatura do produto", example = "https://cdn.despensa.app/p/arroz-camil.png")
    @JsonAlias("thumbnail_url", "imagemUrl")
    val thumbnailUrl: String? = null,

    @field:Schema(description = "Indica se o produto foi cadastrado manualmente", example = "true")
    val manuallyAdded: Boolean = true,

    @field:Schema(description = "Notas ou observações adicionais", example = "Preferir pacote lacrado")
    @JsonAlias("descricao", "notes", "observacoes")
    val notes: String? = null,

    @field:Schema(description = "Timestamp da última modificação (epoch millis UTC)")
    val updatedAt: Long? = null
)

typealias ProdutoRequest = ProductRequestDto
