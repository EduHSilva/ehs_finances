package com.ehs.finances.data.model

import java.time.LocalDate

data class CategoryInput(
    val title: String,
    val color: String,
    val goal: Int,
    val classification: String,
)

data class TransactionInput(
    val title: String,
    val value: Double,
    val income: Boolean,
    val categoryId: String,
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
