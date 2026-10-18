package com.bitlabbr.minhadespensa.modules.invoice.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Item extraído da nota fiscal")
data class InvoiceItemResponseDto(
    @field:Schema(description = "Código de barras global (EAN / GTIN), se identificado", example = "7891234567890")
    val ean: String? = null,

    @field:Schema(description = "Descrição comercial do produto", example = "ARROZ BRANCO TIPO 1 1KG")
    val name: String,

    @field:Schema(description = "Quantidade adquirida", example = "2.0")
    val quantity: Double,

    @field:Schema(description = "Unidade de medida na nota fiscal (UN, KG, G, L, ML, etc.)", example = "KG")
    val measureUnit: String = "UN",

    @field:Schema(description = "Preço unitário em centavos", example = "650")
    val unitPriceInCents: Long? = null,

    @field:Schema(description = "Preço total do item em centavos", example = "1300")
    val totalPriceInCents: Long? = null
)
