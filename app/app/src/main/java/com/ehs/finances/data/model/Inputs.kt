package com.ehs.finances.data.model

import java.time.LocalDate

data class CategoryInput(
    val title: String,
    val color: String,
    val goal: Int,
    val classification: String,
    val income: Boolean,
)

data class TransactionInput(
    val title: String,
    val value: Double,
    val income: Boolean,
    val categoryId: String?,
    val executionDate: LocalDate,
    /** `null` = compra de caixa ("conta/caixa"); preenchido = compra no cartão. */
    val creditCardId: String? = null,
)

data class RecurrenceInput(
    val title: String,
    val value: Double,
    val income: Boolean,
    val categoryId: String,
    val type: String,
    val day: Int,
    val installments: Int?,
    val payed: Int?,
    val creditCardId: String? = null,
)

data class CreditCardInput(
    val name: String,
    val cardLimit: Double?,
    val closingDay: Int,
    val dueDay: Int,
)

data class TransferInput(
    val fromAccountId: String,
    val toAccountId: String,
    val value: Double,
    val date: LocalDate,
    val title: String?,
)

data class MonthlyGoalInput(
    val title: String,
    val mode: String,
    val amount: Double,
    val initialBalance: Double,
    val objective: Double?,
    val periodMonth: String,
    val dtInicio: String?,
    val dtFim: String?,
    val reserve: Boolean = false,
)

data class FinancialAccountInput(
    val name: String,
    val type: String,
    val openingBalance: Double,
)

data class StatementImportLineInput(
    val description: String,
    val amount: Double,
    val income: Boolean,
    val date: String,
    val categoryId: String? = null,
    val sourceTransactionId: String? = null,
)
