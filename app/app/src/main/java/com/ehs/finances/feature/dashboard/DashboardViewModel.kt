package com.ehs.finances.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehs.finances.core.network.ApiException
import com.ehs.finances.core.ui.UiMessage
import com.ehs.finances.data.model.CashflowProjection
import com.ehs.finances.data.model.DashboardPending
import com.ehs.finances.data.model.DashboardSummary
import com.ehs.finances.data.model.PendingItem
import com.ehs.finances.data.model.Recurrence
import com.ehs.finances.data.repository.FinanceState
import com.ehs.finances.data.repository.FinancesRepository
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

data class DashboardUiState(
    val month: YearMonth = YearMonth.now(),
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
    val projection: CashflowProjection? = null,
    val summary: DashboardSummary? = null,
    val pending: DashboardPending? = null,
    val recurrences: List<Recurrence> = emptyList(),
    val confirmingId: String? = null,
) {
    /** Há fatura/compra de cartão nas pendências: mostra o atalho para a tela de cartões. */
    val hasCardPendings: Boolean
        get() = pending?.items.orEmpty().any { it.isInvoice || !it.canConfirm }
}

class DashboardViewModel(
    private val financeState: FinanceState,
    private val repository: FinancesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    private val month = MutableStateFlow(YearMonth.now())
    private val reload = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(financeState.activeAccount, month, reload) { account, selectedMonth, _ ->
                account to selectedMonth
            }.collectLatest { (account, selectedMonth) ->
                load(account?.id, selectedMonth)
            }
        }
    }

    fun setMonth(value: YearMonth) {
        if (value.isBefore(YearMonth.now())) return
        _state.value = _state.value.copy(month = value)
        month.value = value
    }

    fun refresh() {
        _state.value = _state.value.copy(refreshing = true)
        reload.value++
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun confirmPending(item: PendingItem, value: Double) {
        // TRANSFER e INVOICE não são confirmadas por aqui (só abrem a tela correspondente).
        if (!item.canConfirm || _state.value.confirmingId != null) return
        viewModelScope.launch {
            _state.value = _state.value.copy(confirmingId = item.id)
            try {
                if (item.isTransaction) {
                    repository.confirmTransaction(item.id, value)
                } else {
                    repository.confirmRecurrence(item.id, value)
                }
                _message.value = UiMessage.Res("pendingConfirmed")
                reload.value++
            } catch (e: ApiException) {
                _message.value = UiMessage.Text(e.message)
            } catch (e: Exception) {
                _message.value = UiMessage.Res("pendingConfirmFailed")
            } finally {
                _state.value = _state.value.copy(confirmingId = null)
            }
        }
    }

    private suspend fun load(accountId: String?, selectedMonth: YearMonth) {
        if (accountId == null) {
            _state.value = _state.value.copy(
                month = selectedMonth,
                loading = false,
                refreshing = false,
                error = null,
                projection = null,
                summary = null,
                pending = null,
                recurrences = emptyList(),
            )
            return
        }

        val monthKey = selectedMonth.format(MONTH_KEY)
        _state.value = _state.value.copy(
            loading = !_state.value.refreshing,
            error = null,
            month = selectedMonth,
        )
        try {
            coroutineScope {
                val projectionDeferred = async { repository.projection(accountId, monthKey) }
                val summaryDeferred = async { runCatching { repository.summary(accountId, monthKey) }.getOrNull() }
                val pendingDeferred = async { runCatching { repository.pending(accountId, monthKey) }.getOrNull() }
                val recurrencesDeferred = async { runCatching { repository.recurrences(accountId) }.getOrDefault(emptyList()) }

                val projection = projectionDeferred.await()
                _state.value = _state.value.copy(
                    loading = false,
                    refreshing = false,
                    projection = projection,
                    summary = summaryDeferred.await(),
                    pending = pendingDeferred.await(),
                    recurrences = recurrencesDeferred.await(),
                )
            }
        } catch (e: ApiException) {
            _state.value = _state.value.copy(loading = false, refreshing = false, error = e.message)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                loading = false,
                refreshing = false,
                error = e.message ?: "Não foi possível carregar a projeção.",
            )
        }
    }

    companion object {
        private val MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM")

        /** Meses selecionáveis: o atual e os próximos (sem meses passados). */
        fun monthOptions(count: Int = 6): List<YearMonth> {
            val current = YearMonth.now()
            return (0 until count).map { current.plusMonths(it.toLong()) }
        }
    }
}
