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

package com.bitlabbr.minhadespensa.common.util

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.Normalizer
import java.util.UUID

object DeterministicIdUtils {

    val NAMESPACE_PRODUCT: UUID = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
    val NAMESPACE_CATEGORY: UUID = UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8")

    private val DIACRITICS_REGEX = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    private val WHITESPACE_REGEX = "\\s+".toRegex()

    fun normalizeText(input: String): String {
        val nfd = Normalizer.normalize(input, Normalizer.Form.NFD)
        return DIACRITICS_REGEX.replace(nfd, "")
            .lowercase()
            .trim()
            .replace(WHITESPACE_REGEX, " ")
    }

    fun uuid5(namespace: UUID, name: String): UUID {
        val md = MessageDigest.getInstance("SHA-1")
        val nsBuffer = ByteBuffer.wrap(ByteArray(16))
        nsBuffer.putLong(namespace.mostSignificantBits)
        nsBuffer.putLong(namespace.leastSignificantBits)
        md.update(nsBuffer.array())
        md.update(name.toByteArray(StandardCharsets.UTF_8))
        val bytes = md.digest()

        // RFC 4122 version 5 (bits 12-15 = 0101)
        bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x50).toByte()
        // RFC 4122 variant (bits 6-7 = 10)
        bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()

        val resultBuffer = ByteBuffer.wrap(bytes)
        return UUID(resultBuffer.long, resultBuffer.long)
    }

    fun productIdForEan(ean: String): String {
        require(ean.isNotBlank()) { "EAN não pode ser vazio para geração determinística de ID" }
        return uuid5(NAMESPACE_PRODUCT, "ean:${ean.trim()}").toString()
    }

    fun productIdForNameAndUnit(name: String, measureUnit: String): String {
        require(name.isNotBlank()) { "Nome do produto não pode ser vazio" }
        val normalizedName = normalizeText(name)
        val unit = measureUnit.trim().uppercase()
        return uuid5(NAMESPACE_PRODUCT, "name:$normalizedName:$unit").toString()
    }

    fun categoryIdForName(name: String): String {
        require(name.isNotBlank()) { "Nome da categoria não pode ser vazio" }
        val normalizedName = normalizeText(name)
        return uuid5(NAMESPACE_CATEGORY, normalizedName).toString()
    }
}
