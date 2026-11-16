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
