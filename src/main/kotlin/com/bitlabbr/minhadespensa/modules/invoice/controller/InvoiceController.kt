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
        logger.info("Iniciando extração de nota fiscal. Tamanho do payload do QR Code: {} caracteres", request.qrCodeContent.length)
        val result = invoiceParseService.parseInvoiceFromQrCode(request.qrCodeContent)
        logger.info("Extração da nota fiscal concluída com sucesso: {} itens extraídos da loja '{}'", result.items.size, result.storeName)
        return ResponseEntity.ok(result)
    }
}
