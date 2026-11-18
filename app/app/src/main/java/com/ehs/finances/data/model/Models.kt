package com.ehs.finances.data.model

/** Modelos de domínio consumidos pela UI (independentes dos DTOs da API). */
data class Category(
    val id: String,
    val title: String,
    val color: String,
    val goal: Int,
    val classification: String,
    val income: Boolean = false,
    val defaultKey: String? = null,
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
    val categoryKey: String? = null,
    val classification: String?,
    val accountId: String? = null,
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
    val categoryKey: String? = null,
    val installments: Int?,
    val payed: Int?,
    val status: String,
    val accountId: String? = null,
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

data class FinancialAccount(
    val id: String,
    val name: String,
    val type: String,
    val openingBalance: Double,
    val balance: Double,
    val currency: String,
    val role: String = "OWNER",
) {
    val isOwner: Boolean get() = role.equals("OWNER", ignoreCase = true)
}

data class AccountMember(
    val id: String,
    val accountId: String,
    val email: String,
    val name: String?,
    val userId: String?,
    val status: String,
    val expiresAt: String?,
    val acceptedAt: String?,
    val createdAt: String?,
) {
    val isActive: Boolean get() = status.equals("ACTIVE", ignoreCase = true)
}

data class AccountMembers(
    val accountId: String,
    val accountName: String,
    val ownerId: String,
    val ownerName: String?,
    val ownerEmail: String?,
    val members: List<AccountMember>,
)

data class IncomingAccountInvite(
    val id: String,
    val accountId: String,
    val accountName: String,
    val ownerId: String,
    val ownerName: String?,
    val ownerEmail: String?,
    val email: String,
    val expiresAt: String?,
)

data class FinancePreferences(
    val showBusinessAccount: Boolean = true,
    val currency: String = "BRL",
)

data class MonthlyGoal(
    val id: String,
    val accountId: String,
    val title: String,
    val mode: String,
    val amount: Double,
    val initialBalance: Double?,
    val objective: Double?,
    val periodMonth: String,
    val dtInicio: String?,
    val dtFim: String?,
    val recurring: Boolean,
    val active: Boolean,
)

data class MonthlyGoalSummary(
    val goalId: String,
    val accountId: String,
    val title: String,
    val periodMonth: String,
    val incomeBase: Double,
    val expected: Double,
    val realized: Double,
    val difference: Double,
    val progress: Double,
    val initialBalance: Double?,
    val objective: Double?,
    val saved: Double?,
    val objectiveProgress: Double?,
)

data class GoalContribution(
    val amount: Double,
    val date: String,
    val transactionId: String? = null,
    val adjustment: Boolean = false,
)

data class InvestmentSummary(
    val accountId: String,
    val totalContributions: Double,
    val totalWithdrawals: Double,
    val balance: Double,
    val currency: String,
)

data class DashboardCash(
    val currentBalance: Double,
    val monthIncome: Double,
    val monthExpenses: Double,
    val monthNet: Double,
    val projectedClosingBalance: Double,
)

data class DashboardInvestments(
    val total: Double,
    val monthContributions: Double,
    val monthWithdrawals: Double,
    val monthNet: Double,
    val projectedClosingBalance: Double,
)

data class DashboardNetWorth(
    val current: Double,
    val projected: Double,
)

data class LivingCost(
    val monthlyAverage: Double,
    val months: Int,
    val total: Double,
)

data class CategoryShare(
    val categoryId: String?,
    val title: String?,
    val color: String?,
    val amount: Double,
    val share: Double,
)

data class DashboardSummary(
    val accountId: String,
    val horizonDays: Int,
    val calculatedAt: String?,
    val cash: DashboardCash,
    val investments: DashboardInvestments,
    val netWorth: DashboardNetWorth,
    val livingCost: LivingCost,
    val topCategories: List<CategoryShare>,
)

data class PendingItem(
    val id: String,
    val kind: String,
    val title: String,
    val amount: Double,
    val income: Boolean,
    val date: String?,
    val status: String,
)

data class DashboardPending(
    val accountId: String,
    val horizonDays: Int,
    val items: List<PendingItem>,
)

data class ProjectionPoint(
    val date: String?,
    val balance: Double,
    val knownIncome: Double,
    val knownExpenses: Double,
    val estimatedIncome: Double,
    val estimatedExpenses: Double,
)

data class ProjectionItem(
    val date: String?,
    val title: String,
    val amount: Double,
    val income: Boolean,
    val kind: String,
    val recurrenceId: String? = null,
    val installmentNumber: Int? = null,
    val installments: Int? = null,
    val payed: Int? = null,
)

data class CashflowProjection(
    val accountId: String,
    val horizonDays: Int,
    val calculatedAt: String?,
    val currentBalance: Double,
    val closingBalance: Double,
    val confidence: String,
    val assumptions: List<String>,
    val series: List<ProjectionPoint>,
    val items: List<ProjectionItem>,
)

data class StatementImportPreviewLine(
    val row: Int,
    val key: String,
    val description: String,
    val amount: Double,
    val income: Boolean,
    val date: String?,
    val duplicate: Boolean,
)

data class StatementImportPreview(
    val lines: List<StatementImportPreviewLine>,
    val importable: Int,
    val duplicates: Int,
)

data class StatementImportResult(
    val imported: Int,
    val duplicates: Int,
)

data class TransactionImportAudit(
    val id: String,
    val transactionId: String,
    val event: String,
    val createdAt: String?,
)
