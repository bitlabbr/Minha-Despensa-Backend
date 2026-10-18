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

package com.bitlabbr.minhadespensa.modules.pantry.dto

import tools.jackson.core.JsonParser
import tools.jackson.core.JsonToken
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

fun Long?.toIsoDateString(): String? {
    if (this == null) return null
    return try {
        Instant.ofEpochMilli(this)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .toString()
    } catch (_: Exception) {
        null
    }
}

fun parseDateToEpochMillis(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    val trimmed = value.trim()
    return try {
        trimmed.toLong()
    } catch (_: NumberFormatException) {
        try {
            LocalDate.parse(trimmed)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
            try {
                Instant.parse(trimmed).toEpochMilli()
            } catch (_: Exception) {
                null
            }
        }
    }
}

class EpochMillisOrDateDeserializer : ValueDeserializer<Long?>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Long? {
        val token = p.currentToken()
        if (token == JsonToken.VALUE_NUMBER_INT) {
            return p.longValue
        }
        val text = p.valueAsString
        return parseDateToEpochMillis(text)
    }
}
