package com.ehs.finances.domain

import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApiDatesTest {
    @Test
    fun `toApi envia meio-dia local sem fuso`() {
        assertEquals(
            "2026-11-16T12:00:00",
            ApiDates.toApi(LocalDate.of(2026, 11, 16)),
        )
    }

    @Test
    fun `parse aceita data pura e data-hora local`() {
        assertEquals(LocalDate.of(2026, 11, 16), ApiDates.parse("2026-11-16"))
        assertEquals(LocalDate.of(2026, 11, 16), ApiDates.parse("2026-11-16T12:00:00"))
        assertNull(ApiDates.parse(""))
        assertNull(ApiDates.parse("ontem"))
    }

    @Test
    fun `format mostra dd MM yyyy`() {
        assertEquals(
            "16/11/2026",
            ApiDates.format(LocalDate.of(2026, 11, 16), Locale("pt", "BR")),
        )
        assertEquals("—", ApiDates.format(null, Locale("pt", "BR")))
    }

    @Test
    fun `formatMonth mostra mes e ano`() {
        assertEquals(
            "Novembro de 2026",
            ApiDates.formatMonth(YearMonth.of(2026, 11), Locale("pt", "BR")),
        )
    }
}
