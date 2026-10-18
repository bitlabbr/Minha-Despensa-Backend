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
