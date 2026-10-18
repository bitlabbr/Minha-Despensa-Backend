package com.bitlabbr.minhadespensa.modules.invoice.service

import com.bitlabbr.minhadespensa.modules.invoice.dto.InvoiceItemResponseDto
import com.bitlabbr.minhadespensa.modules.invoice.dto.InvoiceResponseDto
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import kotlin.math.roundToLong

@Service
class InvoiceParseService {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun parseInvoiceFromQrCode(qrCodeContent: String): InvoiceResponseDto {
        val trimmedContent = qrCodeContent.trim()
        require(trimmedContent.isNotBlank()) { "O conteúdo do QR Code da nota fiscal não pode ser vazio." }

        val targetUrl = extractUrl(trimmedContent)
        logger.info("Buscando dados da nota fiscal na URL: {}", targetUrl)

        val htmlContent = fetchHtml(targetUrl)
        return parseHtml(htmlContent, trimmedContent)
    }

    internal fun fetchHtml(url: String): String {
        return try {
            Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")
                .timeout(20000)
                .followRedirects(true)
                .ignoreContentType(true)
                .get()
                .html()
        } catch (ex: Exception) {
            logger.error("Falha ao efetuar download da página da SEFAZ na URL {}: {}", url, ex.message)
            throw IllegalStateException("Não foi possível acessar a página da SEFAZ: ${ex.message}", ex)
        }
    }

    internal fun parseHtml(html: String, qrCodeContent: String): InvoiceResponseDto {
        val doc: Document = Jsoup.parse(html)

        val accessKey = extractAccessKey(doc, qrCodeContent)
        val storeName = extractStoreName(doc)
        val emissionDate = extractEmissionDate(doc)
        val items = extractItems(doc)

        logger.info(
            "Nota fiscal processada com sucesso: Loja='{}', Chave='{}', Itens={}",
            storeName,
            accessKey,
            items.size
        )

        return InvoiceResponseDto(
            accessKey = accessKey,
            storeName = storeName,
            emissionDate = emissionDate,
            items = items
        )
    }

    private fun extractUrl(content: String): String {
        val urlMatcher = Pattern.compile("https?://[^\\s|]+").matcher(content)
        return if (urlMatcher.find()) {
            urlMatcher.group()
        } else if (content.startsWith("http://") || content.startsWith("https://")) {
            content
        } else {
            throw IllegalArgumentException("O conteúdo fornecido não possui uma URL válida da SEFAZ: $content")
        }
    }

    private fun extractAccessKey(doc: Document, qrCodeContent: String): String? {
        val chaveSpan = doc.select(".chave, span.chave, .chaveAcesso").text().trim()
        val digitsFromSpan = chaveSpan.filter { it.isDigit() }
        if (digitsFromSpan.length == 44) {
            return digitsFromSpan
        }

        // Tenta extrair da query string p=... do QR Code (ex: 262410...|2|1|1|...)
        try {
            val uri = URI(qrCodeContent)
            val query = uri.query
            if (!query.isNullOrBlank()) {
                val pParam = query.split("&")
                    .firstOrNull { it.startsWith("p=") }
                    ?.removePrefix("p=")
                if (pParam != null) {
                    val candidate = pParam.split("|").firstOrNull()?.filter { it.isDigit() }
                    if (candidate != null && candidate.length == 44) {
                        return candidate
                    }
                }
            }
        } catch (_: Exception) {
            // Ignora erro de parsing de URI e recorre a regex
        }

        // Regex para 44 dígitos no texto
        val regex44 = Pattern.compile("\\b(\\d{44})\\b").matcher(doc.text())
        if (regex44.find()) {
            return regex44.group(1)
        }

        return null
    }

    private fun extractStoreName(doc: Document): String {
        val topoSelectors = listOf(
            ".txtTopo",
            "#u20",
            ".conteudoTopo .txtTopo",
            "div.ui-header h1",
            ".proprietario",
            "#lblNomeFantasia",
            "#lblRazaoSocial"
        )
        for (selector in topoSelectors) {
            val text = doc.select(selector).firstOrNull()?.text()?.trim()
            if (!text.isNullOrBlank()) {
                return text
            }
        }

        // Busca por cabeçalhos h1, h2, h3
        val firstHeader = doc.select("h1, h2, h3").firstOrNull { it.text().isNotBlank() }?.text()?.trim()
        if (!firstHeader.isNullOrBlank() && firstHeader.length in 3..120) {
            return firstHeader
        }

        return "Estabelecimento Comercial"
    }

    private fun extractEmissionDate(doc: Document): String? {
        val text = doc.text()
        val dateMatcher = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})(?:\\s+(\\d{2}:\\d{2}(?::\\d{2})?))?").matcher(text)
        if (dateMatcher.find()) {
            val datePart = dateMatcher.group(1)
            val timePart = dateMatcher.group(2) ?: "12:00:00"
            return try {
                val formatterInput = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                val normalizedTime = if (timePart.count { it == ':' } == 1) "$timePart:00" else timePart
                val parsed = LocalDateTime.parse("$datePart $normalizedTime", formatterInput)
                parsed.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } catch (_: Exception) {
                null
            }
        }
        return null
    }

    private fun extractItems(doc: Document): List<InvoiceItemResponseDto> {
        val rows = doc.select("tr[id^=Item], #tabResult tr, table.tabelaResultado tr, .ui-body-c tr")
        val items = mutableListOf<InvoiceItemResponseDto>()

        for (row in rows) {
            val item = parseItemRow(row)
            if (item != null && item.name.isNotBlank()) {
                items.add(item)
            }
        }

        if (items.isNotEmpty()) {
            return items
        }

        // Fallback: seletor de cards/divs móveis
        val divItems = doc.select("div.item, div[id^=Item], div.linhaProduto")
        for (div in divItems) {
            val item = parseItemDiv(div)
            if (item != null && item.name.isNotBlank()) {
                items.add(item)
            }
        }

        return items
    }

    private fun parseItemRow(row: Element): InvoiceItemResponseDto? {
        val nameElement = row.select(".txtTit, .txtTit2, span.txtTit, td.txtTit").firstOrNull()
            ?: row.select("td:nth-child(1)").firstOrNull()
        val name = nameElement?.text()?.trim() ?: return null
        if (name.isBlank() || name.equals("Item", ignoreCase = true) || name.contains("Descrição", ignoreCase = true)) {
            return null
        }

        val rowText = row.text()

        // EAN / Código do produto
        val ean = extractEan(row, rowText)

        // Quantidade
        val quantity = extractQuantity(row, rowText)

        // Unidade de Medida
        val measureUnit = extractMeasureUnit(row, rowText)

        // Valor Unitário
        val unitPriceInCents = extractUnitPriceInCents(row, rowText)

        // Valor Total
        val totalPriceInCents = extractTotalPriceInCents(row, rowText)
            ?: unitPriceInCents?.let { (quantity * it).roundToLong() }

        return InvoiceItemResponseDto(
            ean = ean,
            name = name,
            quantity = quantity,
            measureUnit = measureUnit,
            unitPriceInCents = unitPriceInCents,
            totalPriceInCents = totalPriceInCents
        )
    }

    private fun parseItemDiv(div: Element): InvoiceItemResponseDto? {
        val name = div.select(".txtTit, h4, strong").firstOrNull()?.text()?.trim() ?: return null
        val text = div.text()

        val ean = extractEan(div, text)
        val quantity = extractQuantity(div, text)
        val measureUnit = extractMeasureUnit(div, text)
        val unitPriceInCents = extractUnitPriceInCents(div, text)
        val totalPriceInCents = extractTotalPriceInCents(div, text)

        return InvoiceItemResponseDto(
            ean = ean,
            name = name,
            quantity = quantity,
            measureUnit = measureUnit,
            unitPriceInCents = unitPriceInCents,
            totalPriceInCents = totalPriceInCents
        )
    }

    private fun extractEan(element: Element, text: String): String? {
        val codText = element.select(".RCod, span.RCod, .codigo").text().trim()
        val codMatcher = Pattern.compile("(?i)(?:Código|Cod|Ean)?:?\\s*\\(?(\\d{8,14})\\)?").matcher(codText)
        if (codMatcher.find()) {
            return codMatcher.group(1)
        }

        val fallbackMatcher = Pattern.compile("(?i)(?:Código|Cod|Ean):?\\s*\\(?(\\d{8,14})\\)?").matcher(text)
        if (fallbackMatcher.find()) {
            return fallbackMatcher.group(1)
        }

        return null
    }

    private fun extractQuantity(element: Element, text: String): Double {
        val qtdText = element.select(".Rqtd, span.Rqtd, .qtd, .quantidade").text().trim()
        val qtdMatcher = Pattern.compile("(?i)(?:Qtde?|Qtd)\\.?:?\\s*([\\d.,]+)").matcher(qtdText)
        if (qtdMatcher.find()) {
            val raw = qtdMatcher.group(1).replace(".", "").replace(",", ".")
            return raw.toDoubleOrNull() ?: 1.0
        }

        val fallbackMatcher = Pattern.compile("(?i)(?:Qtde?|Qtd)\\.?:?\\s*([\\d.,]+)").matcher(text)
        if (fallbackMatcher.find()) {
            val raw = fallbackMatcher.group(1).replace(".", "").replace(",", ".")
            return raw.toDoubleOrNull() ?: 1.0
        }

        return 1.0
    }

    private fun extractMeasureUnit(element: Element, text: String): String {
        val unText = element.select(".RUN, span.RUN, .unidade").text().trim()
        val unMatcher = Pattern.compile("(?i)UN:?\\s*([A-Za-z]+)").matcher(unText)
        if (unMatcher.find()) {
            return unMatcher.group(1).uppercase()
        }

        val fallbackMatcher = Pattern.compile("(?i)UN:?\\s*([A-Za-z]+)").matcher(text)
        if (fallbackMatcher.find()) {
            return fallbackMatcher.group(1).uppercase()
        }

        return "UN"
    }

    private fun extractUnitPriceInCents(element: Element, text: String): Long? {
        val vlUnitText = element.select(".RvlUnit, span.RvlUnit, .valorUnitario").text().trim()
        val matcher = Pattern.compile("(?i)(?:Vl\\.?\\s*Unit\\.?|Unit[áa]rio):?\\s*(?:R\\$\\s*)?([\\d.,]+)").matcher(vlUnitText)
        if (matcher.find()) {
            return parsePriceToCents(matcher.group(1))
        }

        val fallbackMatcher = Pattern.compile("(?i)(?:Vl\\.?\\s*Unit\\.?|Unit[áa]rio):?\\s*(?:R\\$\\s*)?([\\d.,]+)").matcher(text)
        if (fallbackMatcher.find()) {
            return parsePriceToCents(fallbackMatcher.group(1))
        }

        return null
    }

    private fun extractTotalPriceInCents(element: Element, text: String): Long? {
        val vlTotText = element.select(".valor, .RvlTot, span.valor").text().trim()
        val matcher = Pattern.compile("(?:R\\$\\s*)?([\\d.,]+)").matcher(vlTotText)
        if (matcher.find()) {
            return parsePriceToCents(matcher.group(1))
        }

        return null
    }

    private fun parsePriceToCents(priceString: String): Long? {
        return try {
            val clean = priceString.trim()
                .replace("R$", "")
                .trim()
            val numeric = if (clean.contains(",") && clean.contains(".")) {
                clean.replace(".", "").replace(",", ".")
            } else if (clean.contains(",")) {
                clean.replace(",", ".")
            } else {
                clean
            }
            val valueDouble = numeric.toDouble()
            (valueDouble * 100).roundToLong()
        } catch (_: Exception) {
            null
        }
    }
}
