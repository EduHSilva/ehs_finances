package com.ehs.finances.data.remote

import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// Auth (/users)
// ---------------------------------------------------------------------------

@Serializable
data class LoginRequest(
    val email: String? = null,
    val password: String? = null,
    val tokenGmail: String? = null,
    // O users exige `app` no login; sem ele a API responde 403.
    // Sem default de propósito: o Json usa encodeDefaults=false e o campo sumiria do corpo.
    val app: String,
)

@Serializable
data class LoginDto(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val lastLogin: String? = null,
    val token: String? = null,
    val roles: List<String> = emptyList(),
    val apps: List<String> = emptyList(),
)

@Serializable
data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String,
    val role: String,
)

@Serializable
data class UserDto(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val createdDate: String? = null,
    val status: String? = null,
    val roles: List<String> = emptyList(),
    val companyName: String? = null,
)

@Serializable
data class UpdateUserRequest(val username: String)

// ---------------------------------------------------------------------------
// Contas e preferências (/finances)
// ---------------------------------------------------------------------------

@Serializable
data class FinanceAccountDto(
    val id: String = "",
    val name: String = "",
    val type: String = "PF",
    val openingBalance: Double = 0.0,
    val balance: Double = 0.0,
    val currency: String = "BRL",
)

@Serializable
data class CreateFinanceAccountRequest(
    val name: String,
    val type: String,
    val openingBalance: Double,
)

@Serializable
data class FinancePreferencesDto(
    val showBusinessAccount: Boolean = true,
    val currency: String = "BRL",
)

@Serializable
data class UpdateFinancePreferencesRequest(
    val showBusinessAccount: Boolean,
    val currency: String,
)

// ---------------------------------------------------------------------------
// Categorias
// ---------------------------------------------------------------------------

@Serializable
data class CategoryDto(
    val id: String = "",
    val title: String = "",
    val color: String = "#0F766E",
    val goal: Int = 0,
    val classification: String = "ESSENTIAL",
    val income: Boolean = false,
    val defaultKey: String? = null,
)

@Serializable
data class AddCategoryRequest(
    val title: String,
    val color: String,
    val goal: Int,
    val classification: String,
    val income: Boolean,
)

@Serializable
data class UpdateCategoryRequest(
    val title: String,
    val goal: Int,
    val color: String,
    val classification: String,
    val income: Boolean,
)

// ---------------------------------------------------------------------------
// Transações
// ---------------------------------------------------------------------------

@Serializable
data class TransactionDto(
    val id: String = "",
    val income: Boolean = false,
    val value: Double = 0.0,
    val category: String = "",
    val categoryID: String? = null,
    val categoryKey: String? = null,
    val classification: String? = null,
    val recurrence: String? = null,
    val executionDate: String? = null,
    val title: String = "",
    val status: String = "OK",
    val accountId: String? = null,
)

@Serializable
data class AddTransactionRequest(
    val income: Boolean,
    val value: Double,
    val categoryID: String? = null,
    val title: String,
    val recurrencyID: String? = null,
    val executionDate: String,
    val accountId: String? = null,
    val status: String? = null,
)

@Serializable
data class UpdateTransactionRequest(
    val value: Double,
    val title: String,
    val executionDate: String? = null,
    val categoryID: String? = null,
    val income: Boolean? = null,
    val status: String? = null,
)

@Serializable
data class ConfirmValueRequest(val value: Double)

// ---------------------------------------------------------------------------
// Recorrências
// ---------------------------------------------------------------------------

@Serializable
data class RecurrenceDto(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val categoryID: String? = null,
    val categoryKey: String? = null,
    val day: Int = 1,
    val type: String = "MONTH",
    val value: Double = 0.0,
    val income: Boolean = false,
    val installments: Int? = null,
    val payed: Int? = null,
    val status: String = "ACTIVE",
    val accountId: String? = null,
)

@Serializable
data class AddRecurrenceRequest(
    val day: Int,
    val income: Boolean,
    val categoryID: String,
    val type: String,
    val installments: Int? = null,
    val payed: Int? = null,
    val value: Double,
    val title: String,
    val accountId: String? = null,
    val status: String? = null,
)

@Serializable
data class UpdateRecurrenceRequest(
    val day: Int,
    val title: String,
    val installments: Int? = null,
    val payed: Int? = null,
    val value: Double,
    val categoryID: String? = null,
    val income: Boolean? = null,
    val status: String? = null,
)

// ---------------------------------------------------------------------------
// Metas mensais
// ---------------------------------------------------------------------------

@Serializable
data class MonthlyGoalDto(
    val id: String = "",
    val accountId: String = "",
    val title: String = "",
    val mode: String = "FIXED",
    val amount: Double = 0.0,
    val initialBalance: Double? = null,
    val objective: Double? = null,
    val periodMonth: String = "",
    val dtInicio: String? = null,
    val dtFim: String? = null,
    val recurring: Boolean = false,
    val active: Boolean = true,
)

@Serializable
data class MonthlyGoalRequest(
    val title: String,
    val accountId: String,
    val mode: String,
    val amount: Double,
    val initialBalance: Double? = null,
    val objective: Double? = null,
    val periodMonth: String,
    val dtInicio: String? = null,
    val dtFim: String? = null,
    val active: Boolean,
)

@Serializable
data class GoalContributionRequest(
    val amount: Double,
    val date: String,
    val transactionId: String? = null,
    val adjustment: Boolean,
)

@Serializable
data class MonthlyGoalSummaryDto(
    val goalId: String = "",
    val accountId: String = "",
    val title: String = "",
    val periodMonth: String = "",
    val incomeBase: Double = 0.0,
    val expected: Double = 0.0,
    val realized: Double = 0.0,
    val difference: Double = 0.0,
    val progress: Double = 0.0,
    val initialBalance: Double? = null,
    val objective: Double? = null,
    val saved: Double? = null,
    val objectiveProgress: Double? = null,
)

// ---------------------------------------------------------------------------
// Painéis, projeção e investimentos
// ---------------------------------------------------------------------------

@Serializable
data class InvestmentSummaryDto(
    val accountId: String = "",
    val totalContributions: Double = 0.0,
    val totalWithdrawals: Double = 0.0,
    val balance: Double = 0.0,
    val currency: String = "BRL",
)

@Serializable
data class DashboardPendingDto(
    val accountId: String = "",
    val horizonDays: Int = 30,
    val items: List<PendingItemDto> = emptyList(),
)

@Serializable
data class PendingItemDto(
    val id: String = "",
    val kind: String = "TRANSACTION",
    val title: String = "",
    val amount: Double = 0.0,
    val income: Boolean = false,
    val date: String? = null,
    val status: String = "",
)

@Serializable
data class CashflowProjectionDto(
    val accountId: String = "",
    val horizonDays: Int = 90,
    val calculatedAt: String? = null,
    val currentBalance: Double = 0.0,
    val closingBalance: Double = 0.0,
    val confidence: String = "LOW",
    val assumptions: List<String> = emptyList(),
    val series: List<ProjectionPointDto> = emptyList(),
    val items: List<ProjectionItemDto> = emptyList(),
)

@Serializable
data class ProjectionPointDto(
    val date: String? = null,
    val balance: Double = 0.0,
    val knownIncome: Double = 0.0,
    val knownExpenses: Double = 0.0,
    val estimatedIncome: Double = 0.0,
    val estimatedExpenses: Double = 0.0,
)

@Serializable
data class ProjectionItemDto(
    val date: String? = null,
    val title: String = "",
    val amount: Double = 0.0,
    val income: Boolean = false,
    val kind: String = "",
    val recurrenceId: String? = null,
    val installmentNumber: Int? = null,
    val installments: Int? = null,
    val payed: Int? = null,
)

// ---------------------------------------------------------------------------
// Importação de extrato
// ---------------------------------------------------------------------------

@Serializable
data class StatementImportLineRequest(
    val description: String,
    val amount: Double,
    val income: Boolean,
    val date: String,
    val categoryId: String? = null,
    val sourceTransactionId: String? = null,
)

@Serializable
data class StatementImportRequest(
    val accountId: String,
    val source: String,
    val lines: List<StatementImportLineRequest>,
)

@Serializable
data class StatementImportPreviewDto(
    val lines: List<StatementImportPreviewLineDto> = emptyList(),
    val importable: Int = 0,
    val duplicates: Int = 0,
)

@Serializable
data class StatementImportPreviewLineDto(
    val row: Int = 0,
    val key: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val income: Boolean = false,
    val date: String? = null,
    val duplicate: Boolean = false,
)

@Serializable
data class StatementImportResultDto(
    val imported: Int = 0,
    val duplicates: Int = 0,
)

@Serializable
data class ReviewImportedTransactionRequest(
    val approved: Boolean,
    val categoryId: String? = null,
)

@Serializable
data class TransactionImportAuditDto(
    val id: String = "",
    val transactionId: String = "",
    val event: String = "",
    val createdAt: String? = null,
)
