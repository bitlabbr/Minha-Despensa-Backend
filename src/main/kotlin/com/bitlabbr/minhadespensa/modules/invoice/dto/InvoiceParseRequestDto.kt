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
import jakarta.validation.constraints.NotBlank

@Schema(description = "Payload contendo a URL ou conteúdo do QR Code da NFC-e")
data class InvoiceParseRequestDto(
    @field:Schema(
        description = "URL completa lida do QR Code da NFC-e (ex: SEFAZ Pernambuco)",
        example = "http://nfce.sefaz.pe.gov.br/nfce/consulta?p=262410...|2|1|1|..."
    )
    @field:NotBlank(message = "O conteúdo do QR Code é obrigatório")
    val qrCodeContent: String
)
