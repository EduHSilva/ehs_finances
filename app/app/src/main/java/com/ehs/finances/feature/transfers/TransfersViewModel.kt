package com.ehs.finances.feature.transfers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehs.finances.core.network.ApiException
import com.ehs.finances.core.ui.UiMessage
import com.ehs.finances.data.model.FinancialAccount
import com.ehs.finances.data.model.Transaction
import com.ehs.finances.data.model.Transfer
import com.ehs.finances.data.model.TransferInput
import com.ehs.finances.data.repository.FinanceState
import com.ehs.finances.data.repository.FinancesRepository
import com.ehs.finances.domain.ApiDates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class TransfersUiState(
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
    val accountId: String? = null,
    /** Contas de destino possíveis (visíveis no seletor, como no topo do app). */
    val accounts: List<FinancialAccount> = emptyList(),
    val transfers: List<Transfer> = emptyList(),
    val month: YearMonth = YearMonth.now(),
    val saving: Boolean = false,
    val deletingId: String? = null,
) {
    val accountNames: Map<String, String>
        get() = accounts.associate { it.id to it.name }

    val filtered: List<Transfer>
        get() = transfers.filter { transfer ->
            val date = transferDate(transfer) ?: return@filter false
            YearMonth.from(date) == month
        }

    fun accountName(id: String?): String? = id?.let { accountNames[it] }
}

class TransfersViewModel(
    private val financeState: FinanceState,
    private val repository: FinancesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TransfersUiState())
    val state: StateFlow<TransfersUiState> = _state.asStateFlow()

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    private val reload = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            // A conta ativa define o extrato consultado; as duas pernas levam o mesmo transferId,
            // então basta uma conta para listar as transferências dela.
            combine(financeState.activeAccount, financeState.visibleAccounts, reload) { account, visible, _ ->
                account to visible
            }.collectLatest { (account, visible) ->
                load(account?.id, visible)
            }
        }
    }

    fun refresh() {
        _state.value = _state.value.copy(refreshing = true)
        reload.value++
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun setMonth(month: YearMonth) {
        _state.value = _state.value.copy(month = month)
    }

    fun save(
        transferId: String?,
        input: TransferInput,
    ) {
        if (_state.value.saving) return
        if (input.fromAccountId == input.toAccountId) {
            _message.value = UiMessage.Res("transferSameAccount")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true)
            try {
                if (transferId == null) {
                    repository.createTransfer(input)
                    _message.value = UiMessage.Res("transferCreated")
                } else {
                    repository.updateTransfer(
                        transferId = transferId,
                        value = input.value,
                        date = input.date,
                        title = input.title,
                    )
                    _message.value = UiMessage.Res("transferUpdated")
                }
                // Servidor e cliente não mantêm saldo local: recarrega as contas para refletir o novo saldo.
                runCatching { financeState.refresh() }
                reload.value++
            } catch (e: ApiException) {
                _message.value = UiMessage.Text(e.message)
            } catch (e: Exception) {
                _message.value = UiMessage.Res("transferSaveFailed")
            } finally {
                _state.value = _state.value.copy(saving = false)
            }
        }
    }

    fun delete(transferId: String) {
        if (_state.value.deletingId != null) return
        viewModelScope.launch {
            _state.value = _state.value.copy(deletingId = transferId)
            try {
                repository.deleteTransfer(transferId)
                _message.value = UiMessage.Res("transferDeleted")
                runCatching { financeState.refresh() }
                reload.value++
            } catch (e: ApiException) {
                _message.value = UiMessage.Text(e.message)
            } catch (e: Exception) {
                _message.value = UiMessage.Res("transferDeleteFailed")
            } finally {
                _state.value = _state.value.copy(deletingId = null)
            }
        }
    }

    private suspend fun load(accountId: String?, visible: List<FinancialAccount>) {
        if (accountId == null) {
            _state.value = _state.value.copy(
                loading = false,
                refreshing = false,
                error = null,
                accountId = null,
                accounts = visible,
                transfers = emptyList(),
            )
            return
        }

        _state.value = _state.value.copy(
            loading = !_state.value.refreshing,
            error = null,
            accountId = accountId,
            accounts = visible,
        )
        try {
            val transactions = repository.transactions(accountId)
            val transfers = transactions.toTransfers()
            _state.value = _state.value.copy(
                loading = false,
                refreshing = false,
                transfers = transfers,
            )
        } catch (e: ApiException) {
            _state.value = _state.value.copy(loading = false, refreshing = false, error = e.message)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                loading = false,
                refreshing = false,
                error = e.message ?: "Não foi possível carregar as transferências.",
            )
        }
    }

    /**
     * Reconstrói as transferências a partir das pernas `TRANSFER` do extrato: a saída da conta
     * ativa revela a conta de destino; a entrada (transferência criada na conta de destino)
     * revela a origem.
     */
    private fun List<Transaction>.toTransfers(): List<Transfer> {
        val active = _state.value.accountId
        return filter { it.isTransfer && !it.transferId.isNullOrBlank() }
            .groupBy { it.transferId!! }
            .mapNotNull { (transferId, legs) ->
                val out = legs.firstOrNull { !it.income }
                val income = legs.firstOrNull { it.income }
                val from = when {
                    out != null -> active
                    else -> income?.counterpartAccountId
                }
                val to = when {
                    out != null -> out.counterpartAccountId
                    else -> active
                }
                if (from.isNullOrBlank() || to.isNullOrBlank()) return@mapNotNull null
                Transfer(
                    transferId = transferId,
                    fromAccountId = from,
                    toAccountId = to,
                    value = out?.value ?: income?.value ?: 0.0,
                    date = out?.executionDate ?: income?.executionDate,
                    title = (out ?: income)?.title?.takeIf { it.isNotBlank() },
                    status = (out ?: income)?.status.orEmpty(),
                    outTransactionId = out?.id,
                    inTransactionId = income?.id,
                )
            }
    }
}

/** Data da transferência, para agrupar por mês. */
fun transferDate(transfer: Transfer): LocalDate? = ApiDates.parse(transfer.date)
