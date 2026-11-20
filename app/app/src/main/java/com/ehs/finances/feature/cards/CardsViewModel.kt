package com.ehs.finances.feature.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehs.finances.core.network.ApiException
import com.ehs.finances.core.ui.UiMessage
import com.ehs.finances.data.model.CreditCard
import com.ehs.finances.data.model.CreditCardInput
import com.ehs.finances.data.repository.FinanceState
import com.ehs.finances.data.repository.FinancesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class CardsUiState(
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
    val cards: List<CreditCard> = emptyList(),
    val saving: Boolean = false,
    val deletingId: String? = null,
) {
    val openInvoicesTotal: Double get() = cards.sumOf { it.openInvoiceTotal }
}

class CardsViewModel(
    private val financeState: FinanceState,
    private val repository: FinancesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CardsUiState())
    val state: StateFlow<CardsUiState> = _state.asStateFlow()

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    private val reload = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(financeState.activeAccount, reload) { account, _ -> account }
                .collectLatest { account -> load(account?.id) }
        }
    }

    fun refresh() {
        _state.value = _state.value.copy(refreshing = true)
        reload.value++
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun save(id: String?, input: CreditCardInput) {
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
                    repository.createCard(accountId!!, input)
                    _message.value = UiMessage.Res("cardCreated")
                } else {
                    repository.updateCard(id, input)
                    _message.value = UiMessage.Res("cardUpdated")
                }
                reload.value++
            } catch (e: ApiException) {
                _message.value = UiMessage.Text(e.message)
            } catch (e: Exception) {
                _message.value = UiMessage.Res("cardSaveFailed")
            } finally {
                _state.value = _state.value.copy(saving = false)
            }
        }
    }

    fun delete(id: String) {
        if (_state.value.deletingId != null) return
        viewModelScope.launch {
            _state.value = _state.value.copy(deletingId = id)
            try {
                repository.deleteCard(id)
                _message.value = UiMessage.Res("cardDeleted")
                reload.value++
            } catch (e: ApiException) {
                _message.value = UiMessage.Text(e.message)
            } catch (e: Exception) {
                _message.value = UiMessage.Res("cardDeleteFailed")
            } finally {
                _state.value = _state.value.copy(deletingId = null)
            }
        }
    }

    private suspend fun load(accountId: String?) {
        if (accountId == null) {
            _state.value = _state.value.copy(
                loading = false,
                refreshing = false,
                error = null,
                cards = emptyList(),
            )
            return
        }

        _state.value = _state.value.copy(loading = !_state.value.refreshing, error = null)
        try {
            val cards = repository.cards(accountId)
            _state.value = _state.value.copy(
                loading = false,
                refreshing = false,
                cards = cards,
            )
        } catch (e: ApiException) {
            _state.value = _state.value.copy(loading = false, refreshing = false, error = e.message)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                loading = false,
                refreshing = false,
                error = e.message ?: "Não foi possível carregar os cartões.",
            )
        }
    }
}
