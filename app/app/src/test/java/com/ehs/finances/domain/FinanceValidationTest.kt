package com.ehs.finances.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FinanceValidationTest {
    @Test
    fun `titulo obrigatorio`() {
        assertEquals("titleRequired", FinanceValidation.titleError(""))
        assertEquals("titleRequired", FinanceValidation.titleError("   "))
        assertNull(FinanceValidation.titleError("Mercado"))
    }

    @Test
    fun `valor precisa ser positivo`() {
        assertEquals("valueInvalid", FinanceValidation.amountError(""))
        assertEquals("valueInvalid", FinanceValidation.amountError("abc"))
        assertEquals("valueInvalid", FinanceValidation.amountError("0"))
        assertNull(FinanceValidation.amountError("10,50"))
    }

    @Test
    fun `categoria e data obrigatorias`() {
        assertEquals("categoryRequired", FinanceValidation.categoryError(null))
        assertEquals("categoryRequired", FinanceValidation.categoryError(" "))
        assertNull(FinanceValidation.categoryError("categoria-1"))
        assertEquals("dateInvalid", FinanceValidation.dateError(null))
        assertNull(FinanceValidation.dateError(LocalDate.of(2026, 11, 16)))
    }

    @Test
    fun `meta fica entre 0 e 100`() {
        assertEquals("goalInvalid", FinanceValidation.goalError(""))
        assertEquals("goalInvalid", FinanceValidation.goalError("-1"))
        assertEquals("goalInvalid", FinanceValidation.goalError("101"))
        assertNull(FinanceValidation.goalError("0"))
        assertNull(FinanceValidation.goalError("100"))
    }

    @Test
    fun `dia fica entre 1 e 31`() {
        assertEquals("dayInvalid", FinanceValidation.dayError("0"))
        assertEquals("dayInvalid", FinanceValidation.dayError("32"))
        assertEquals("dayInvalid", FinanceValidation.dayError("x"))
        assertNull(FinanceValidation.dayError("1"))
        assertNull(FinanceValidation.dayError("31"))
    }

    @Test
    fun `parcelas pagas nao passam do total`() {
        assertEquals("installmentsInvalid", FinanceValidation.installmentsError("", "0"))
        assertEquals("installmentsInvalid", FinanceValidation.installmentsError("0", "0"))
        assertEquals("payedInvalid", FinanceValidation.installmentsError("3", "4"))
        assertNull(FinanceValidation.installmentsError("3", "0"))
        assertNull(FinanceValidation.installmentsError("3", "3"))
    }
}
