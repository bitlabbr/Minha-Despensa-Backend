package com.bitlabbr.minhadespensa.modules.invoice.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Dados consolidados extraídos da Nota Fiscal Eletrônica")
data class InvoiceResponseDto(
    @field:Schema(description = "Chave de acesso de 44 dígitos da NFC-e", example = "26241012345678000190650010001234561001234567")
    val accessKey: String? = null,

    @field:Schema(description = "Razão social ou nome do estabelecimento emitente", example = "Supermercado Exemplo Ltda")
    val storeName: String,

    @field:Schema(description = "Data e hora de emissão da nota fiscal", example = "2026-10-18T14:30:00")
    val emissionDate: String? = null,

    @field:Schema(description = "Lista de itens e produtos extraídos da nota fiscal")
    val items: List<InvoiceItemResponseDto> = emptyList()
)
