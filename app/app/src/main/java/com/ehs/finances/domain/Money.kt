package com.ehs.finances.domain

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Formatação/parse de valores monetários em BRL. */
object Money {
    fun format(value: Double, locale: Locale): String =
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance("BRL")
        }.format(value)

    /** Aceita "1.234,56", "1,234.56", "R$ 1234,56" e "1234.56". */
    fun parse(text: String): Double? {
        val cleaned = text.trim()
            .replace("R$", "")
            .replace(" ", "")
            .replace("\u00A0", "")
        if (cleaned.isEmpty()) return null
        val lastComma = cleaned.lastIndexOf(',')
        val lastDot = cleaned.lastIndexOf('.')
        val normalized = when {
            lastComma >= 0 && lastDot >= 0 ->
                if (lastComma > lastDot) cleaned.replace(".", "").replace(',', '.')
                else cleaned.replace(",", "")
            lastComma >= 0 -> cleaned.replace(',', '.')
            else -> cleaned
        }
        return normalized.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
    }
}
