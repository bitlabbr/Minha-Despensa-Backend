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

package com.bitlabbr.minhadespensa.modules.invoice.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class InvoiceParseServiceTest {

    private val service = InvoiceParseService()

    @Test
    fun `parseHtml should correctly extract storeName, accessKey, and items from sample SEFAZ NFC-e HTML`() {
        val sampleHtml = """
            <!DOCTYPE html>
            <html>
            <head><title>NFC-e</title></head>
            <body>
                <div class="txtTopo">SUPERMERCADO BOM PRECO LTDA</div>
                <div class="chave">2624 1012 3456 7800 0190 6500 1000 1234 5610 0123 4567</div>
                <div class="info">Emissão: 18/10/2026 14:35:20</div>
                <table id="tabResult">
                    <tr id="Item + 1">
                        <td class="txtTit">ARROZ BRANCO TIPO 1 1KG</td>
                        <td class="RCod">(Código: 7891234567890)</td>
                        <td class="Rqtd">Qtde.: 2,000</td>
                        <td class="RUN">UN: KG</td>
                        <td class="RvlUnit">Vl. Unit.: 6,50</td>
                        <td class="valor">13,00</td>
                    </tr>
                    <tr id="Item + 2">
                        <td class="txtTit">FEIJAO CARIOCA 1KG</td>
                        <td class="RCod">(Código: 7891234567891)</td>
                        <td class="Rqtd">Qtde.: 1,000</td>
                        <td class="RUN">UN: UN</td>
                        <td class="RvlUnit">Vl. Unit.: 8,99</td>
                        <td class="valor">8,99</td>
                    </tr>
                </table>
            </body>
            </html>
        """.trimIndent()

        val qrCodeUrl = "http://nfce.sefaz.pe.gov.br/nfce/consulta?p=26241012345678000190650010001234561001234567|2|1|1|HASH"
        val result = service.parseHtml(sampleHtml, qrCodeUrl)

        assertEquals("SUPERMERCADO BOM PRECO LTDA", result.storeName)
        assertEquals("26241012345678000190650010001234561001234567", result.accessKey)
        assertNotNull(result.emissionDate)
        assertEquals(2, result.items.size)

        val item1 = result.items[0]
        assertEquals("7891234567890", item1.ean)
        assertEquals("ARROZ BRANCO TIPO 1 1KG", item1.name)
        assertEquals(2.0, item1.quantity)
        assertEquals("KG", item1.measureUnit)
        assertEquals(650L, item1.unitPriceInCents)
        assertEquals(1300L, item1.totalPriceInCents)

        val item2 = result.items[1]
        assertEquals("7891234567891", item2.ean)
        assertEquals("FEIJAO CARIOCA 1KG", item2.name)
        assertEquals(1.0, item2.quantity)
        assertEquals("UN", item2.measureUnit)
        assertEquals(899L, item2.unitPriceInCents)
        assertEquals(899L, item2.totalPriceInCents)
    }

    @Test
    fun `parseInvoiceFromQrCode should throw IllegalArgumentException when content is blank`() {
        assertThrows<IllegalArgumentException> {
            service.parseInvoiceFromQrCode("   ")
        }
    }
}
