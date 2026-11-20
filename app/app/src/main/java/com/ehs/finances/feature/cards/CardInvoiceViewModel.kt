package com.ehs.finances.feature.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehs.finances.core.network.ApiException
import com.ehs.finances.core.ui.UiMessage
import com.ehs.finances.data.model.CreditCard
import com.ehs.finances.data.model.CreditCardInvoice
import com.ehs.finances.data.repository.FinancesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CardInvoiceUiState(
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
    val card: CreditCard? = null,
    val invoices: List<CreditCardInvoice> = emptyList(),
    val saving: Boolean = false,
    val deletingPaymentId: String? = null,
) {
    /** Ciclos mais recentes primeiro (a API já devolve ordenado; garantimos aqui também). */
    val sortedInvoices: List<CreditCardInvoice>
        get() = invoices.sortedByDescending { it.invoiceMonth }
}

class CardInvoiceViewModel(
    private val cardId: String,
    private val repository: FinancesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CardInvoiceUiState())
    val state: StateFlow<CardInvoiceUiState> = _state.asStateFlow()

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    init {
        load(refresh = false)
    }

    fun refresh() {
        load(refresh = true)
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun pay(value: Double, date: LocalDate, invoiceMonth: String) {
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true)
            try {
                repository.payInvoice(cardId, invoiceMonth, value, date)
                _message.value = UiMessage.Res("invoicePaidSuccess")
                fetch(showLoading = false)
            } catch (e: ApiException) {
                _message.value = UiMessage.Text(e.message)
            } catch (e: Exception) {
                _message.value = UiMessage.Res("invoicePayFailed")
            } finally {
                _state.value = _state.value.copy(saving = false)
            }
        }
    }

    fun deletePayment(invoiceMonth: String, paymentId: String) {
        if (_state.value.deletingPaymentId != null) return
        viewModelScope.launch {
            _state.value = _state.value.copy(deletingPaymentId = paymentId)
            try {
                repository.deleteInvoicePayment(cardId, invoiceMonth, paymentId)
                _message.value = UiMessage.Res("invoicePaymentDeleted")
                fetch(showLoading = false)
            } catch (e: ApiException) {
                _message.value = UiMessage.Text(e.message)
            } catch (e: Exception) {
                _message.value = UiMessage.Res("invoicePaymentDeleteFailed")
            } finally {
                _state.value = _state.value.copy(deletingPaymentId = null)
            }
        }
    }

    private fun load(refresh: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                loading = !refresh || _state.value.invoices.isEmpty(),
                refreshing = refresh,
                error = null,
            )
            fetch(showLoading = false)
            _state.value = _state.value.copy(loading = false, refreshing = false)
        }
    }

    private suspend fun fetch(showLoading: Boolean) {
        if (showLoading) _state.value = _state.value.copy(loading = true)
        try {
            // O cartão é opcional para a tela: se falhar, as faturas ainda aparecem.
            val card = _state.value.card ?: runCatching { repository.card(cardId) }.getOrNull()
            val invoices = repository.cardInvoices(cardId)
            _state.value = _state.value.copy(card = card, invoices = invoices, error = null)
        } catch (e: ApiException) {
            _state.value = _state.value.copy(error = e.message)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                error = e.message ?: "Não foi possível carregar a fatura.",
            )
        }
    }
}
