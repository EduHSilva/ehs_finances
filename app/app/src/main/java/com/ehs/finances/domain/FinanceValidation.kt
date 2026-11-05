package com.ehs.finances.domain

import java.time.LocalDate

/** Validações de formulário; devolvem chaves de tradução (ou `null` quando está válido). */
object FinanceValidation {
    fun titleError(title: String): String? =
        if (title.isBlank()) "titleRequired" else null

    fun amountError(text: String): String? {
        val value = Money.parse(text) ?: return "valueInvalid"
        return if (value <= 0.0) "valueInvalid" else null
    }

    fun categoryError(categoryId: String?): String? =
        if (categoryId.isNullOrBlank()) "categoryRequired" else null

    fun dateError(date: LocalDate?): String? = if (date == null) "dateInvalid" else null

    fun goalError(goalText: String): String? {
        val goal = goalText.trim().toIntOrNull() ?: return "goalInvalid"
        return if (goal in 0..100) null else "goalInvalid"
    }

    fun dayError(dayText: String): String? {
        val day = dayText.trim().toIntOrNull() ?: return "dayInvalid"
        return if (day in 1..31) null else "dayInvalid"
    }

    fun installmentsError(installmentsText: String, payedText: String): String? {
        val installments = installmentsText.trim().toIntOrNull()
            ?: return "installmentsInvalid"
        if (installments < 1) return "installmentsInvalid"
        val payed = payedText.trim().toIntOrNull() ?: 0
        if (payed < 0 || payed > installments) return "payedInvalid"
        return null
    }
}
