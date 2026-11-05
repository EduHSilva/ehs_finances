package com.ehs.finances.domain

import com.ehs.finances.data.model.Category
import com.ehs.finances.data.model.Recurrence
import com.ehs.finances.data.model.Transaction
import java.time.YearMonth

data class MonthlySummary(
    val income: Double = 0.0,
    val expense: Double = 0.0,
) {
    val balance: Double get() = income - expense
    val savingsRate: Double get() = if (income > 0) ((income - expense) / income) * 100.0 else 0.0
}

data class CategorySpending(
    val title: String,
    val value: Double,
    val color: String,
    val goal: Int,
    val expected: Double,
)

/** Cálculos do painel, derivados das transações do mês selecionado. */
object FinanceMath {
    fun summary(transactions: List<Transaction>, month: YearMonth): MonthlySummary {
        val inMonth = transactionsIn(transactions, month)
        return MonthlySummary(
            income = inMonth.filter { it.income }.sumOf { it.value },
            expense = inMonth.filterNot { it.income }.sumOf { it.value },
        )
    }

    fun transactionsIn(transactions: List<Transaction>, month: YearMonth): List<Transaction> =
        transactions.filter { transaction ->
            val date = ApiDates.parse(transaction.executionDate) ?: return@filter false
            YearMonth.from(date) == month
        }

    fun spendingByCategory(
        transactions: List<Transaction>,
        categories: List<Category>,
        month: YearMonth,
    ): List<CategorySpending> {
        val summary = summary(transactions, month)
        val expenses = transactionsIn(transactions, month).filterNot { it.income }
        return categories.mapNotNull { category ->
            val value = expenses.filter { it.category.equals(category.title, ignoreCase = true) }
                .sumOf { it.value }
            if (value <= 0.0 && category.goal <= 0) return@mapNotNull null
            val expected = if (summary.expense > 0) summary.expense * (category.goal / 100.0) else 0.0
            CategorySpending(
                title = category.title,
                value = value,
                color = category.color,
                goal = category.goal,
                expected = expected,
            )
        }.sortedByDescending { it.value }
    }

    /** Recorrências que ainda podem ser lançadas neste mês (ativas e não quitadas). */
    fun pendingRecurrences(recurrences: List<Recurrence>, month: YearMonth): List<Recurrence> {
        val today = java.time.LocalDate.now()
        return recurrences.filter { recurrence ->
            val done = recurrence.status.equals("PAYED", true) || recurrence.status.equals("INACTIVE", true)
            if (done) return@filter false
            val remaining = recurrence.installments?.let { (it - (recurrence.payed ?: 0)) > 0 } ?: true
            remaining && (YearMonth.from(today) == month || recurrence.day >= 1)
        }.sortedBy { it.day }
    }
}
