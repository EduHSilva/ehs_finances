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
    val recurrence: String? = null,
    val accountId: String? = null,
    val entryType: String = ENTRY_NORMAL,
    val creditCardId: String? = null,
    val creditCardName: String? = null,
    val transferId: String? = null,
    val counterpartAccountId: String? = null,
    val invoiceDueDate: String? = null,
) {
    val isCard: Boolean get() = entryType.equals(ENTRY_CARD, ignoreCase = true)
    val isTransfer: Boolean get() = entryType.equals(ENTRY_TRANSFER, ignoreCase = true)
    val isInvoicePayment: Boolean get() = entryType.equals(ENTRY_INVOICE_PAYMENT, ignoreCase = true)

    /**
     * `TRANSFER` e `INVOICE_PAYMENT` vivem só nos endpoints de transferência/fatura:
     * o `PUT`/`DELETE` de lançamentos os rejeita.
     */
    val isManagedByService: Boolean get() = isTransfer || isInvoicePayment

    companion object {
        const val ENTRY_NORMAL = "NORMAL"
        const val ENTRY_CARD = "CARD"
        const val ENTRY_TRANSFER = "TRANSFER"
        const val ENTRY_INVOICE_PAYMENT = "INVOICE_PAYMENT"
    }
}

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
    val creditCardId: String? = null,
    val creditCardName: String? = null,
) {
    val isInstallment: Boolean get() = type.equals("INSTALLMENT", ignoreCase = true)
    val remaining: Int? get() = installments?.let { total -> (total - (payed ?: 0)).coerceAtLeast(0) }
}

data class CreditCard(
    val id: String,
    val accountId: String,
    val name: String,
    val cardLimit: Double?,
    val closingDay: Int,
    val dueDay: Int,
    val status: String,
    val openInvoiceTotal: Double,
) {
    val isActive: Boolean get() = status.equals("ACTIVE", ignoreCase = true)

    /** Uso do limite (informativo: não bloqueia compras). Sem limite, não há uso. */
    val limitUsage: Double? get() = cardLimit?.takeIf { it > 0.0 }?.let { openInvoiceTotal / it }
}

data class CreditCardInvoice(
    val cardId: String,
    val invoiceMonth: String,
    val closingDate: String?,
    val dueDate: String?,
    val total: Double,
    val paid: Double,
    val remaining: Double,
    val status: String,
    val payments: List<CreditCardInvoicePayment> = emptyList(),
) {
    val isPaid: Boolean get() = status.equals("PAID", ignoreCase = true)
    val isPartial: Boolean get() = status.equals("PARTIAL", ignoreCase = true)
    val isOpen: Boolean get() = status.equals("OPEN", ignoreCase = true)

    /** Fatura com saldo em aberto e sem compra pendente. O backend também valida. */
    val canPay: Boolean get() = !isPaid && remaining > 0.0
}

data class CreditCardInvoicePayment(
    val id: String,
    val value: Double,
    val date: String?,
    val transactionId: String? = null,
)

data class Transfer(
    val transferId: String,
    val fromAccountId: String,
    val toAccountId: String,
    val value: Double,
    val date: String?,
    val title: String?,
    val status: String,
    val outTransactionId: String? = null,
    val inTransactionId: String? = null,
)

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
    val reserve: Boolean = false,
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
    val monthlyAverage: Double?,
    val months: Int,
    val total: Double,
    val reserve: Reserve? = null,
)

data class Reserve(
    val goalId: String?,
    val title: String?,
    val total: Double,
    val months: Double?,
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
    val cards: DashboardCards? = null,
)

data class DashboardCards(
    val openInvoicesTotal: Double,
    val cards: List<DashboardCardTotal>,
)

data class DashboardCardTotal(
    val cardId: String,
    val name: String,
    val total: Double,
)

data class PendingItem(
    val id: String,
    val kind: String,
    val title: String,
    val amount: Double,
    val income: Boolean,
    val date: String?,
    val status: String,
    val cardId: String? = null,
    val cardName: String? = null,
    val invoiceMonth: String? = null,
) {
    val isTransaction: Boolean get() = kind.equals("TRANSACTION", ignoreCase = true)
    val isRecurrence: Boolean get() = kind.equals("RECURRENCE", ignoreCase = true)
    val isTransfer: Boolean get() = kind.equals("TRANSFER", ignoreCase = true)
    val isInvoice: Boolean get() = kind.equals("INVOICE", ignoreCase = true)

    /** Só transação e recorrência podem ser confirmadas por aqui. */
    val canConfirm: Boolean get() = isTransaction || isRecurrence
}

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
