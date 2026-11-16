package com.ehs.finances.domain

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Datas da API. O back serializa `java.util.Date` como ISO-8601 (ou epoch millis, dependendo do
 * mapper); [parse] aceita as duas formas. Datas enviadas vão como ISO-8601.
 */
object ApiDates {
    private val dateOnly = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    fun parse(value: JsonElement?): LocalDate? {
        val primitive = value as? JsonPrimitive ?: return null
        if (primitive.isString) return parse(primitive.content)
        val millis = primitive.content.toLongOrNull() ?: return null
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    fun parse(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        if (dateOnly.matches(value)) return runCatching { LocalDate.parse(value) }.getOrNull()
        val instant = runCatching { Instant.parse(value) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
            ?: runCatching {
                LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant()
            }.getOrNull()
        return instant?.atZone(ZoneId.systemDefault())?.toLocalDate()
    }

    /**
     * Envia o dia escolhido como meio-dia local sem fuso, igual ao web
     * (`yyyy-MM-ddT12:00:00`). O meio-dia evita que o dia mude quando o servidor
     * interpreta o valor em outro fuso.
     */
    fun toApi(value: LocalDate): String = value.atTime(12, 0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    fun format(value: LocalDate?, locale: Locale): String =
        value?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)) ?: "—"

    fun formatMonth(value: YearMonth, locale: Locale): String =
        value.format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy", locale))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}
