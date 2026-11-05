package com.ehs.finances.data.model

/** Modelos de domínio consumidos pela UI (independentes dos DTOs da API). */
data class Category(
    val id: String,
    val title: String,
    val color: String,
    val goal: Int,
    val classification: String,
) {
    val isEssential: Boolean get() = classification.equals("ESSENTIAL", ignoreCase = true)
}

data class Transaction(
    val id: String,
    val title: String,
    val value: Double,
    val income: Boolean,
    val executionDate: String?,
    val status: String,
    val categoryId: String?,
    val category: String,
    val classification: String?,
)

data class Recurrence(
    val id: String,
    val title: String,
    val value: Double,
    val income: Boolean,
    val type: String,
    val day: Int,
    val categoryId: String?,
    val category: String,
    val installments: Int?,
    val payed: Int?,
    val status: String,
) {
    val isInstallment: Boolean get() = type.equals("INSTALLMENT", ignoreCase = true)
    val remaining: Int? get() = installments?.let { total -> (total - (payed ?: 0)).coerceAtLeast(0) }
}

data class Page<T>(
    val items: List<T>,
    val total: Long,
    val page: Int,
    val size: Int,
)
