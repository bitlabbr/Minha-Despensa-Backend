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
