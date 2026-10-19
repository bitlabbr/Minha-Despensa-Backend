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

import com.bitlabbr.minhadespensa.common.exception.GlobalExceptionHandler
import com.bitlabbr.minhadespensa.modules.invoice.dto.InvoiceItemResponseDto
import com.bitlabbr.minhadespensa.modules.invoice.dto.InvoiceResponseDto
import com.bitlabbr.minhadespensa.modules.invoice.service.InvoiceParseService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(InvoiceController::class)
@Import(GlobalExceptionHandler::class)
class InvoiceControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var invoiceParseService: InvoiceParseService

    @Test
    fun `POST invoice parse should return 200 with extracted invoice data`() {
        val payload = """
            {
                "qrCodeContent": "http://nfce.sefaz.pe.gov.br/nfce/consulta?p=26241012345678000190650010001234561001234567"
            }
        """.trimIndent()

        val mockInvoice = InvoiceResponseDto(
            accessKey = "26241012345678000190650010001234561001234567",
            storeName = "SUPERMERCADO TESTE",
            emissionDate = "2026-10-18T14:35:00",
            items = listOf(
                InvoiceItemResponseDto(
                    ean = "7891234567890",
                    name = "ARROZ 1KG",
                    quantity = 2.0,
                    measureUnit = "KG",
                    unitPriceInCents = 650,
                    totalPriceInCents = 1300
                )
            )
        )

        whenever(invoiceParseService.parseInvoiceFromQrCode(any())).thenReturn(mockInvoice)

        mockMvc.perform(
            post("/api/v1/invoice/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.storeName").value("SUPERMERCADO TESTE"))
            .andExpect(jsonPath("$.accessKey").value("26241012345678000190650010001234561001234567"))
            .andExpect(jsonPath("$.items[0].name").value("ARROZ 1KG"))
            .andExpect(jsonPath("$.items[0].ean").value("7891234567890"))
            .andExpect(jsonPath("$.items[0].quantity").value(2.0))
            .andExpect(jsonPath("$.items[0].unitPriceInCents").value(650))
            .andExpect(jsonPath("$.items[0].totalPriceInCents").value(1300))
    }

    @Test
    fun `POST invoice parse with blank qrCodeContent should return 400`() {
        val payload = """
            {
                "qrCodeContent": "   "
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/invoice/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        )
            .andExpect(status().isBadRequest)
    }
}
