package com.ehs.finances.data.repository

import com.ehs.finances.core.network.apiCall
import com.ehs.finances.core.network.unwrap
import com.ehs.finances.data.mappers.toModel
import com.ehs.finances.data.model.AccountMember
import com.ehs.finances.data.model.AccountMembers
import com.ehs.finances.data.model.CashflowProjection
import com.ehs.finances.data.model.Category
import com.ehs.finances.data.model.CategoryInput
import com.ehs.finances.data.model.DashboardPending
import com.ehs.finances.data.model.DashboardSummary
import com.ehs.finances.data.model.FinancePreferences
import com.ehs.finances.data.model.FinancialAccount
import com.ehs.finances.data.model.FinancialAccountInput
import com.ehs.finances.data.model.GoalContribution
import com.ehs.finances.data.model.IncomingAccountInvite
import com.ehs.finances.data.model.InvestmentSummary
import com.ehs.finances.data.model.MonthlyGoal
import com.ehs.finances.data.model.MonthlyGoalInput
import com.ehs.finances.data.model.MonthlyGoalSummary
import com.ehs.finances.data.model.Recurrence
import com.ehs.finances.data.model.RecurrenceInput
import com.ehs.finances.data.model.StatementImportLineInput
import com.ehs.finances.data.model.StatementImportPreview
import com.ehs.finances.data.model.StatementImportResult
import com.ehs.finances.data.model.Transaction
import com.ehs.finances.data.model.TransactionImportAudit
import com.ehs.finances.data.model.TransactionInput
import com.ehs.finances.data.remote.AddCategoryRequest
import com.ehs.finances.data.remote.AddRecurrenceRequest
import com.ehs.finances.data.remote.AddTransactionRequest
import com.ehs.finances.data.remote.ConfirmValueRequest
import com.ehs.finances.data.remote.CreateFinanceAccountRequest
import com.ehs.finances.data.remote.FinancesApi
import com.ehs.finances.data.remote.GoalContributionRequest
import com.ehs.finances.data.remote.InviteAccountMemberRequest
import com.ehs.finances.data.remote.MonthlyGoalRequest
import com.ehs.finances.data.remote.ReviewImportedTransactionRequest
import com.ehs.finances.data.remote.StatementImportLineRequest
import com.ehs.finances.data.remote.StatementImportRequest
import com.ehs.finances.data.remote.UpdateCategoryRequest
import com.ehs.finances.data.remote.UpdateFinancePreferencesRequest
import com.ehs.finances.data.remote.UpdateRecurrenceRequest
import com.ehs.finances.data.remote.UpdateTransactionRequest
import com.ehs.finances.domain.ApiDates

/** Repositório único do microserviço `finances`, com os mesmos contratos usados pelo web. */
class FinancesRepository(private val api: FinancesApi) {

    // ----- Contas e preferências -------------------------------------------

    suspend fun preferences(): FinancePreferences =
        apiCall { api.preferences().unwrap().toModel() }

    suspend fun updatePreferences(value: FinancePreferences): FinancePreferences =
        apiCall {
            api.updatePreferences(
                UpdateFinancePreferencesRequest(
                    showBusinessAccount = value.showBusinessAccount,
                    currency = value.currency,
                ),
            ).unwrap().toModel()
        }

    suspend fun accounts(): List<FinancialAccount> =
        apiCall { api.accounts().unwrap().map { it.toModel() } }

    suspend fun createAccount(input: FinancialAccountInput): FinancialAccount =
        apiCall {
            api.createAccount(
                CreateFinanceAccountRequest(
                    name = input.name.trim(),
                    type = input.type,
                    openingBalance = input.openingBalance,
                ),
            ).unwrap().toModel()
        }

    // ----- Categorias -------------------------------------------------------

    suspend fun categories(): List<Category> = apiCall {
        api.categories(page = 0, size = PAGE_SIZE).unwrap().items.map { it.toModel() }
    }

    suspend fun createCategory(input: CategoryInput): Category = apiCall {
        api.createCategory(
            AddCategoryRequest(
                title = input.title.trim(),
                color = input.color,
                goal = input.goal,
                classification = input.classification,
                income = input.income,
            ),
        ).unwrap().toModel()
    }

    suspend fun updateCategory(id: String, input: CategoryInput): Category = apiCall {
        api.updateCategory(
            id,
            UpdateCategoryRequest(
                title = input.title.trim(),
                goal = input.goal,
                color = input.color,
                classification = input.classification,
                income = input.income,
            ),
        ).unwrap().toModel()
    }

    suspend fun deleteCategory(id: String) {
        apiCall { api.deleteCategory(id) }
    }

    // ----- Transações -------------------------------------------------------

    /** Carrega todas as páginas, como o web faz nas telas que precisam do histórico completo. */
    suspend fun transactions(accountId: String): List<Transaction> {
        val result = mutableListOf<Transaction>()
        var page = 0
        var total = Long.MAX_VALUE
        while (result.size < total && page < MAX_PAGES) {
            val response = apiCall {
                api.transactions(accountId = accountId, page = page, size = PAGE_SIZE).unwrap()
            }
            result += response.items.map { it.toModel() }
            total = response.total
            if (response.items.isEmpty()) break
            page++
        }
        return result
    }

    suspend fun createTransaction(
        accountId: String,
        input: TransactionInput,
        status: String = "OK",
    ): Transaction = apiCall {
        api.createTransaction(
            AddTransactionRequest(
                income = input.income,
                value = input.value,
                categoryID = input.categoryId,
                title = input.title.trim(),
                executionDate = ApiDates.toApi(input.executionDate),
                accountId = accountId,
                status = status,
            ),
        ).unwrap().toModel()
    }

    suspend fun updateTransaction(
        id: String,
        input: TransactionInput,
        updateCategory: Boolean = true,
    ): Transaction = apiCall {
        api.updateTransaction(
            id,
            UpdateTransactionRequest(
                value = input.value,
                title = input.title.trim(),
                executionDate = ApiDates.toApi(input.executionDate),
                categoryID = if (updateCategory) input.categoryId else null,
                income = if (updateCategory) input.income else null,
            ),
        ).unwrap().toModel()
    }

    suspend fun deleteTransaction(id: String) {
        apiCall { api.deleteTransaction(id) }
    }

    suspend fun confirmTransaction(id: String, value: Double? = null): Transaction =
        apiCall {
            api.confirmTransaction(id, value?.let(::ConfirmValueRequest)).unwrap().toModel()
        }

    suspend fun reviewImportedTransaction(
        id: String,
        approved: Boolean,
        categoryId: String? = null,
    ): Transaction? = apiCall {
        api.reviewImported(
            id,
            ReviewImportedTransactionRequest(approved = approved, categoryId = categoryId),
        ).data?.toModel()
    }

    suspend fun importAudit(transactionId: String): List<TransactionImportAudit> = apiCall {
        api.importAudit(transactionId).unwrap().map { it.toModel() }
    }

    suspend fun previewImport(
        accountId: String,
        source: String,
        lines: List<StatementImportLineInput>,
    ): StatementImportPreview = apiCall {
        api.previewImport(buildImportRequest(accountId, source, lines)).unwrap().toModel()
    }

    suspend fun confirmImport(
        accountId: String,
        source: String,
        lines: List<StatementImportLineInput>,
    ): StatementImportResult = apiCall {
        api.confirmImport(buildImportRequest(accountId, source, lines)).unwrap().toModel()
    }

    private fun buildImportRequest(
        accountId: String,
        source: String,
        lines: List<StatementImportLineInput>,
    ): StatementImportRequest = StatementImportRequest(
        accountId = accountId,
        source = source,
        lines = lines.map { line ->
            StatementImportLineRequest(
                description = line.description,
                amount = line.amount,
                income = line.income,
                date = line.date,
                categoryId = line.categoryId,
                sourceTransactionId = line.sourceTransactionId,
            )
        },
    )

    // ----- Recorrências -----------------------------------------------------

    suspend fun recurrences(accountId: String): List<Recurrence> = apiCall {
        api.recurrences(accountId = accountId, page = 0, size = PAGE_SIZE)
            .unwrap().items.map { it.toModel() }
    }

    suspend fun createRecurrence(
        accountId: String,
        input: RecurrenceInput,
        status: String = "ACTIVE",
    ): Recurrence = apiCall {
        api.createRecurrence(
            AddRecurrenceRequest(
                day = input.day,
                income = input.income,
                categoryID = input.categoryId,
                type = input.type,
                installments = input.installments,
                payed = input.payed,
                value = input.value,
                title = input.title.trim(),
                accountId = accountId,
                status = status,
            ),
        ).unwrap().toModel()
    }

    suspend fun updateRecurrence(
        id: String,
        input: RecurrenceInput,
        status: String,
        updateCategory: Boolean = true,
    ): Recurrence = apiCall {
        api.updateRecurrence(
            id,
            UpdateRecurrenceRequest(
                day = input.day,
                title = input.title.trim(),
                installments = input.installments,
                payed = input.payed,
                value = input.value,
                categoryID = if (updateCategory) input.categoryId else null,
                income = if (updateCategory) input.income else null,
                status = status,
            ),
        ).unwrap().toModel()
    }

    suspend fun deleteRecurrence(id: String) {
        apiCall { api.deleteRecurrence(id) }
    }

    suspend fun confirmRecurrence(id: String, value: Double? = null): Recurrence =
        apiCall {
            api.confirmRecurrence(id, value?.let(::ConfirmValueRequest)).unwrap().toModel()
        }

    // ----- Metas mensais ----------------------------------------------------

    suspend fun goals(accountId: String, periodMonth: String): List<MonthlyGoal> = apiCall {
        api.goals(
            accountId = accountId,
            periodMonth = periodMonth,
            page = 0,
            size = PAGE_SIZE,
        ).unwrap().items.map { it.toModel() }
    }

    suspend fun goalSummary(goalId: String, periodMonth: String): MonthlyGoalSummary =
        apiCall { api.goalSummary(goalId, periodMonth).unwrap().toModel() }

    suspend fun createGoal(
        accountId: String,
        input: MonthlyGoalInput,
    ): MonthlyGoal = apiCall {
        api.createGoal(input.toRequest(accountId, active = true)).unwrap().toModel()
    }

    suspend fun updateGoal(
        id: String,
        accountId: String,
        input: MonthlyGoalInput,
        active: Boolean,
    ): MonthlyGoal = apiCall {
        api.updateGoal(id, input.toRequest(accountId, active)).unwrap().toModel()
    }

    suspend fun deleteGoal(id: String) {
        apiCall { api.deleteGoal(id) }
    }

    suspend fun contributeToGoal(goalId: String, contribution: GoalContribution) {
        apiCall {
            api.contributeToGoal(
                goalId,
                GoalContributionRequest(
                    amount = contribution.amount,
                    date = contribution.date,
                    transactionId = contribution.transactionId,
                    adjustment = contribution.adjustment,
                ),
            )
        }
    }

    private fun MonthlyGoalInput.toRequest(accountId: String, active: Boolean): MonthlyGoalRequest =
        MonthlyGoalRequest(
            title = title.trim(),
            accountId = accountId,
            mode = mode,
            amount = amount,
            initialBalance = initialBalance,
            objective = objective,
            periodMonth = periodMonth,
            dtInicio = dtInicio,
            dtFim = dtFim,
            active = active,
            reserve = reserve,
        )

    // ----- Painel -----------------------------------------------------------

    suspend fun projection(accountId: String, days: Int): CashflowProjection = apiCall {
        api.projection(accountId, days).unwrap().toModel()
    }

    suspend fun pending(accountId: String, days: Int): DashboardPending = apiCall {
        api.pending(accountId, days).unwrap().toModel()
    }

    suspend fun investments(accountId: String): InvestmentSummary = apiCall {
        api.investments(accountId).unwrap().toModel()
    }

    suspend fun summary(accountId: String, days: Int): DashboardSummary = apiCall {
        api.summary(accountId, days).unwrap().toModel()
    }

    // ----- Membros e convites da conta ---------------------------------------

    suspend fun accountMembers(accountId: String): AccountMembers = apiCall {
        api.accountMembers(accountId).unwrap().toModel()
    }

    suspend fun inviteAccountMember(accountId: String, email: String): AccountMember = apiCall {
        api.inviteAccountMember(accountId, InviteAccountMemberRequest(email.trim()))
            .unwrap().toModel()
    }

    suspend fun resendAccountInvite(accountId: String, memberId: String): AccountMember = apiCall {
        api.resendAccountInvite(accountId, memberId).unwrap().toModel()
    }

    suspend fun revokeAccountMember(accountId: String, memberId: String) {
        apiCall { api.revokeAccountMember(accountId, memberId) }
    }

    suspend fun accountInvites(): List<IncomingAccountInvite> = apiCall {
        api.accountInvites().unwrap().map { it.toModel() }
    }

    suspend fun acceptAccountInvite(inviteId: String) {
        apiCall { api.acceptAccountInvite(inviteId) }
    }

    suspend fun declineAccountInvite(inviteId: String) {
        apiCall { api.declineAccountInvite(inviteId) }
    }

    private companion object {
        const val PAGE_SIZE = 200
        const val MAX_PAGES = 50
    }
}
