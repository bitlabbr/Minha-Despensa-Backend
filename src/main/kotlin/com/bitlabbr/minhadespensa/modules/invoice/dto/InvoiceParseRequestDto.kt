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
