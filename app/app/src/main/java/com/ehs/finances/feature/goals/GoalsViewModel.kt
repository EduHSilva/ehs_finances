package com.ehs.finances.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehs.finances.core.network.ApiException
import com.ehs.finances.core.ui.UiMessage
import com.ehs.finances.data.model.Category
import com.ehs.finances.data.model.GoalContribution
import com.ehs.finances.data.model.MonthlyGoal
import com.ehs.finances.data.model.MonthlyGoalInput
import com.ehs.finances.data.model.MonthlyGoalSummary
import com.ehs.finances.data.model.Transaction
import com.ehs.finances.data.repository.FinanceState
import com.ehs.finances.data.repository.FinancesRepository
import com.ehs.finances.domain.ApiDates
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class CategoryGoalRow(
    val id: String,
    val title: String,
    val color: String,
    val goal: Int,
    val expected: Double,
    val realized: Double,
    val progress: Double,
    val achieved: Double?,
    val share: Double?,
)

data class GoalsUiState(
    val month: YearMonth = YearMonth.now(),
    val loading: Boolean = false,
    val error: String? = null,
    val goals: List<MonthlyGoal> = emptyList(),
    val summaries: Map<String, MonthlyGoalSummary> = emptyMap(),
    val monthlyIncome: Double = 0.0,
    val categories: List<Category> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val saving: Boolean = false,
    val contributing: Boolean = false,
    val deletingId: String? = null,
) {
    val expectedTotal: Double get() = goals.sumOf { summaries[it.id]?.expected ?: 0.0 }
    val realizedTotal: Double get() = goals.sumOf { summaries[it.id]?.realized ?: 0.0 }

    fun expected(goal: MonthlyGoal): Double = summaries[goal.id]?.expected ?: 0.0
    fun realized(goal: MonthlyGoal): Double = summaries[goal.id]?.realized ?: 0.0
    fun initialBalance(goal: MonthlyGoal): Double =
        summaries[goal.id]?.initialBalance ?: goal.initialBalance ?: 0.0

    fun saved(goal: MonthlyGoal): Double =
        summaries[goal.id]?.saved ?: goal.initialBalance ?: 0.0

    fun objective(goal: MonthlyGoal): Double? = summaries[goal.id]?.objective ?: goal.objective

    fun objectiveProgress(goal: MonthlyGoal): Double =
        summaries[goal.id]?.objectiveProgress ?: 0.0

    fun categoryReport(): List<CategoryGoalRow> {
        val monthKey = month.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val realizedByCategory = mutableMapOf<String, Double>()
        for (transaction in transactions) {
            if (transaction.income || !transaction.status.equals("OK", ignoreCase = true)) continue
            val categoryId = transaction.categoryId ?: continue
            val date = ApiDates.parse(transaction.executionDate) ?: continue
            if (YearMonth.from(date).format(DateTimeFormatter.ofPattern("yyyy-MM")) != monthKey) continue
            realizedByCategory[categoryId] = (realizedByCategory[categoryId] ?: 0.0) + transaction.value
        }
        val income = monthlyIncome
        return categories
            .filter { category -> !category.income && realizedByCategory.containsKey(category.id) }
            .map { category ->
                val realized = realizedByCategory[category.id] ?: 0.0
                val expected = if (category.goal > 0) income * category.goal / 100.0 else 0.0
                CategoryGoalRow(
                    id = category.id,
                    title = category.title,
                    color = category.color,
                    goal = category.goal,
                    expected = expected,
                    realized = realized,
                    progress = if (expected > 0) (realized / expected * 100.0).coerceAtMost(100.0) else 0.0,
                    achieved = if (expected > 0) realized / expected * 100.0 else null,
                    share = if (income > 0) realized / income * 100.0 else null,
                )
            }
            .sortedByDescending { it.realized }
    }
}

class GoalsViewModel(
    private val financeState: FinanceState,
    private val repository: FinancesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(GoalsUiState())
    val state: StateFlow<GoalsUiState> = _state.asStateFlow()

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    private val month = MutableStateFlow(YearMonth.now())
    private val reload = MutableStateFlow(0)
    private var categoriesLoaded = false

    init {
        viewModelScope.launch {
            combine(financeState.activeAccount, month, reload) { account, currentMonth, _ ->
                account to currentMonth
            }.collectLatest { (account, currentMonth) ->
                load(account?.id, currentMonth)
            }
        }
    }

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun nextMonth() {
        month.value = month.value.plusMonths(1)
    }

    fun refresh() {
        reload.value++
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun save(id: String?, input: MonthlyGoalInput, active: Boolean = true) {
        if (_state.value.saving) return
        val accountId = financeState.activeAccount.value?.id
        if (id == null && accountId == null) {
            _message.value = UiMessage.Res("noAccounts")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true)
            try {
                if (id == null) {
                    repository.createGoal(accountId!!, input)
                } else {
                    repository.updateGoal(id, accountId!!, input, active)
                }
                _message.value = UiMessage.Res(if (id == null) "goalCreated" else "goalUpdated")
                _state.value = _state.value.copy(saving = false)
                reload.value++
            } catch (e: ApiException) {
                _state.value = _state.value.copy(saving = false)
                _message.value = UiMessage.Text(e.message)
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false)
                _message.value = UiMessage.Res("goalSaveFailed")
            }
        }
    }

    fun delete(id: String) {
        if (_state.value.deletingId != null) return
        viewModelScope.launch {
            _state.value = _state.value.copy(deletingId = id)
            try {
                repository.deleteGoal(id)
                _message.value = UiMessage.Res("goalDeleted")
                _state.value = _state.value.copy(deletingId = null)
                reload.value++
            } catch (e: ApiException) {
                _state.value = _state.value.copy(deletingId = null)
                _message.value = UiMessage.Text(e.message)
            } catch (e: Exception) {
                _state.value = _state.value.copy(deletingId = null)
                _message.value = UiMessage.Res("goalDeleteFailed")
            }
        }
    }

    fun contribute(goal: MonthlyGoal, amount: Double, date: String, adjustment: Boolean) {
        if (_state.value.contributing) return
        viewModelScope.launch {
            _state.value = _state.value.copy(contributing = true)
            try {
                repository.contributeToGoal(
                    goal.id,
                    GoalContribution(
                        amount = amount,
                        date = date,
                        adjustment = adjustment,
                    ),
                )
                _message.value = UiMessage.Res(
                    if (adjustment) "goalAdjustmentAdded" else "goalContributionAdded",
                )
                _state.value = _state.value.copy(contributing = false)
                reload.value++
            } catch (e: ApiException) {
                _state.value = _state.value.copy(contributing = false)
                _message.value = UiMessage.Text(e.message)
            } catch (e: Exception) {
                _state.value = _state.value.copy(contributing = false)
                _message.value = UiMessage.Res("goalContributionFailed")
            }
        }
    }

    private suspend fun load(accountId: String?, currentMonth: YearMonth) {
        if (accountId == null) {
            _state.value = _state.value.copy(
                month = currentMonth,
                loading = false,
                error = null,
                goals = emptyList(),
                summaries = emptyMap(),
                transactions = emptyList(),
                monthlyIncome = 0.0,
            )
            return
        }

        val periodMonth = currentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        _state.value = _state.value.copy(month = currentMonth, loading = true, error = null)
        try {
            coroutineScope {
                val goalsDeferred = async { repository.goals(accountId, periodMonth) }
                val transactionsDeferred = async { repository.transactions(accountId) }
                val categoriesDeferred = async {
                    if (categoriesLoaded) null else runCatching { repository.categories() }.getOrNull()
                }

                val goals = goalsDeferred.await().filter { it.active }
                val summaries = goals.associate { goal ->
                    goal.id to repository.goalSummary(goal.id, periodMonth)
                }
                val transactions = transactionsDeferred.await()
                categoriesDeferred.await()?.let { categories ->
                    categoriesLoaded = true
                    _state.value = _state.value.copy(categories = categories)
                }

                val monthlyIncome = summaries.values.firstOrNull()?.incomeBase
                    ?: transactions.filter { transaction ->
                        transaction.income && transaction.status.equals("OK", ignoreCase = true) &&
                            ApiDates.parse(transaction.executionDate)?.let { YearMonth.from(it) == currentMonth } == true
                    }.sumOf { it.value }

                _state.value = _state.value.copy(
                    loading = false,
                    goals = goals,
                    summaries = summaries,
                    transactions = transactions,
                    monthlyIncome = monthlyIncome,
                )
            }
        } catch (e: ApiException) {
            _state.value = _state.value.copy(loading = false, error = e.message)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                loading = false,
                error = e.message ?: "Não foi possível carregar as metas.",
            )
        }
    }
}
