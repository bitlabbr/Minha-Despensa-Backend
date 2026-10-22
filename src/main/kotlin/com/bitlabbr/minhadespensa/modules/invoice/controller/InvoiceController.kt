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

package com.bitlabbr.minhadespensa.modules.invoice.controller

import com.bitlabbr.minhadespensa.common.dto.ErrorResponse
import com.bitlabbr.minhadespensa.modules.invoice.dto.InvoiceParseRequestDto
import com.bitlabbr.minhadespensa.modules.invoice.dto.InvoiceResponseDto
import com.bitlabbr.minhadespensa.modules.invoice.service.InvoiceParseService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/invoice")
@Tag(name = "Notas Fiscais", description = "Endpoints para consulta, download e extração de dados de NFC-e (SEFAZ-PE)")
class InvoiceController(
    private val invoiceParseService: InvoiceParseService
) {

    private val logger = LoggerFactory.getLogger(InvoiceController::class.java)

    @Operation(
        summary = "Consultar e extrair dados da NFC-e via QR Code",
        description = "Recebe a URL lida do QR Code da Nota Fiscal Eletrônica (SEFAZ-PE), efetua o download e retorna a lista de produtos estruturada em JSON"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Nota fiscal processada com sucesso"),
            ApiResponse(
                responseCode = "400",
                description = "URL ou conteúdo de QR Code inválido",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "502",
                description = "Falha ao comunicar com o servidor da SEFAZ",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PostMapping("/parse")
    fun parseInvoice(
        @Valid @RequestBody request: InvoiceParseRequestDto
    ): ResponseEntity<InvoiceResponseDto> {
        logger.info("Starting invoice extraction. QR Code payload size: {} characters", request.qrCodeContent.length)
        val result = invoiceParseService.parseInvoiceFromQrCode(request.qrCodeContent)
        logger.info("Invoice extraction completed successfully: {} items extracted from store '{}'", result.items.size, result.storeName)
        return ResponseEntity.ok(result)
    }
}
