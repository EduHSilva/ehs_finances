package com.ehs.finances.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehs.finances.core.network.ApiException
import com.ehs.finances.core.ui.UiMessage
import com.ehs.finances.data.model.CashflowProjection
import com.ehs.finances.data.model.DashboardPending
import com.ehs.finances.data.model.InvestmentSummary
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

data class DashboardUiState(
    val days: Int = 30,
    val loading: Boolean = false,
    val error: String? = null,
    val projection: CashflowProjection? = null,
    val pending: DashboardPending? = null,
    val investments: InvestmentSummary? = null,
    val recurrences: List<Recurrence> = emptyList(),
    val confirmingId: String? = null,
)

class DashboardViewModel(
    private val financeState: FinanceState,
    private val repository: FinancesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    private val days = MutableStateFlow(30)
    private val reload = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(financeState.activeAccount, days, reload) { account, horizon, _ ->
                account to horizon
            }.collectLatest { (account, horizon) ->
                load(account?.id, horizon)
            }
        }
    }

    fun setDays(value: Int) {
        if (value !in HORIZONS) return
        _state.value = _state.value.copy(days = value)
        days.value = value
    }

    fun refresh() {
        reload.value++
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun confirmPending(item: PendingItem) {
        if (_state.value.confirmingId != null) return
        viewModelScope.launch {
            _state.value = _state.value.copy(confirmingId = item.id)
            try {
                if (item.kind.equals("TRANSACTION", ignoreCase = true)) {
                    repository.confirmTransaction(item.id)
                } else {
                    repository.confirmRecurrence(item.id)
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

    private suspend fun load(accountId: String?, horizon: Int) {
        if (accountId == null) {
            _state.value = _state.value.copy(
                loading = false,
                error = null,
                projection = null,
                pending = null,
                investments = null,
                recurrences = emptyList(),
            )
            return
        }

        _state.value = _state.value.copy(loading = true, error = null, days = horizon)
        try {
            coroutineScope {
                val projectionDeferred = async { repository.projection(accountId, horizon) }
                val pendingDeferred = async { runCatching { repository.pending(accountId, horizon) }.getOrNull() }
                val investmentsDeferred = async { runCatching { repository.investments(accountId) }.getOrNull() }
                val recurrencesDeferred = async { runCatching { repository.recurrences(accountId) }.getOrDefault(emptyList()) }

                val projection = projectionDeferred.await()
                _state.value = _state.value.copy(
                    loading = false,
                    projection = projection,
                    pending = pendingDeferred.await(),
                    investments = investmentsDeferred.await(),
                    recurrences = recurrencesDeferred.await(),
                )
            }
        } catch (e: ApiException) {
            _state.value = _state.value.copy(loading = false, error = e.message)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                loading = false,
                error = e.message ?: "Não foi possível carregar a projeção.",
            )
        }
    }

    companion object {
        val HORIZONS = listOf(30, 90, 180)
    }
}
