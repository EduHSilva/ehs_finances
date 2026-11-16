package com.ehs.finances.domain

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {
    @Test
    fun `parse aceita formatos pt-BR e en-US`() {
        assertEquals(1234.56, Money.parse("1.234,56")!!, 0.001)
        assertEquals(1234.56, Money.parse("1,234.56")!!, 0.001)
        assertEquals(1234.56, Money.parse("R\$ 1234,56")!!, 0.001)
        assertEquals(1234.56, Money.parse("1234.56")!!, 0.001)
        assertEquals(10.5, Money.parse("10,5")!!, 0.001)
        assertEquals(0.0, Money.parse("0")!!, 0.001)
    }

    @Test
    fun `parse rejeita vazio, texto e negativos`() {
        assertNull(Money.parse(""))
        assertNull(Money.parse("   "))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("-10"))
        assertNull(Money.parse("1,2,3,4"))
    }

    @Test
    fun `format usa BRL independente do locale`() {
        val ptBr = Money.format(1234.56, Locale("pt", "BR"))
        assertTrue(ptBr.contains("1.234,56"))

        val enUs = Money.format(1234.56, Locale.US)
        assertTrue(enUs.contains("1,234.56"))
    }
}
