package com.ehs.finances.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehs.finances.core.network.ApiException
import com.ehs.finances.core.ui.UiMessage
import com.ehs.finances.data.model.Category
import com.ehs.finances.data.model.CreditCard
import com.ehs.finances.data.model.Transaction
import com.ehs.finances.data.model.TransactionInput
import com.ehs.finances.data.repository.FinanceState
import com.ehs.finances.data.repository.FinancesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class TransactionsUiState(
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val cards: List<CreditCard> = emptyList(),
    val search: String = "",
    val categoryFilter: String = FILTER_ALL,
    val statusFilter: String = FILTER_ALL,
    val saving: Boolean = false,
    val deletingId: String? = null,
    val confirmingId: String? = null,
) {
    val filtered: List<Transaction>
        get() {
            val term = search.trim().lowercase()
            return transactions.filter { transaction ->
                val matchesSearch = term.isEmpty() ||
                    transaction.title.lowercase().contains(term) ||
                    transaction.category.lowercase().contains(term)
                val matchesCategory = when (categoryFilter) {
                    FILTER_ALL -> true
                    FILTER_NONE -> transaction.categoryId == null
                    else -> transaction.categoryId == categoryFilter
                }
                val matchesStatus = statusFilter == FILTER_ALL ||
                    transaction.status.equals(statusFilter, ignoreCase = true)
                matchesSearch && matchesCategory && matchesStatus
            }
        }

    val reviewCount: Int get() = transactions.count { it.status.equals("REVIEW", ignoreCase = true) }

    companion object {
        const val FILTER_ALL = "__all"
        const val FILTER_NONE = "__none"
    }
}

class TransactionsViewModel(
    private val financeState: FinanceState,
    private val repository: FinancesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TransactionsUiState())
    val state: StateFlow<TransactionsUiState> = _state.asStateFlow()

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    private val reload = MutableStateFlow(0)
    private var categoriesLoaded = false
    private var cardsLoadedFor: String? = null

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

    fun setSearch(value: String) {
        _state.value = _state.value.copy(search = value)
    }

    fun setCategoryFilter(value: String) {
        _state.value = _state.value.copy(categoryFilter = value)
    }

    fun setStatusFilter(value: String) {
        _state.value = _state.value.copy(statusFilter = value)
    }

    fun save(
        id: String?,
        input: TransactionInput,
        status: String = "OK",
        currentCreditCardId: String? = null,
    ) {
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
                    repository.createTransaction(accountId!!, input, status)
                } else {
                    repository.updateTransaction(id, input, currentCreditCardId)
                }
                _message.value = UiMessage.Res("transactionSaved")
                reload.value++
            } catch (e: ApiException) {
                _message.value = UiMessage.Text(e.message)
            } catch (e: Exception) {
                _message.value = UiMessage.Res("transactionSaveFailed")
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
                repository.deleteTransaction(id)
                _message.value = UiMessage.Res("transactionDeleted")
                reload.value++
            } catch (e: ApiException) {
                _message.value = UiMessage.Text(e.message)
            } catch (e: Exception) {
                _message.value = UiMessage.Res("transactionDeleteFailed")
            } finally {
                _state.value = _state.value.copy(deletingId = null)
            }
        }
    }

    fun confirm(id: String, value: Double) {
        if (_state.value.confirmingId != null) return
        viewModelScope.launch {
            _state.value = _state.value.copy(confirmingId = id)
            try {
                repository.confirmTransaction(id, value)
                _message.value = UiMessage.Res("transactionConfirmed")
                reload.value++
            } catch (e: ApiException) {
                _message.value = UiMessage.Text(e.message)
            } catch (e: Exception) {
                _message.value = UiMessage.Res("confirmValueFailed")
            } finally {
                _state.value = _state.value.copy(confirmingId = null)
            }
        }
    }

    private suspend fun load(accountId: String?) {
        if (accountId == null) {
            _state.value = _state.value.copy(
                loading = false,
                refreshing = false,
                error = null,
                transactions = emptyList(),
                cards = emptyList(),
            )
            return
        }

        _state.value = _state.value.copy(loading = !_state.value.refreshing, error = null)
        try {
            if (!categoriesLoaded) {
                val categories = runCatching { repository.categories() }.getOrNull()
                if (categories != null) {
                    categoriesLoaded = true
                    _state.value = _state.value.copy(categories = categories)
                }
            }
            if (cardsLoadedFor != accountId || _state.value.cards.isEmpty()) {
                val cards = runCatching { repository.cards(accountId) }.getOrNull()
                if (cards != null) {
                    cardsLoadedFor = accountId
                    _state.value = _state.value.copy(cards = cards)
                }
            }
            val transactions = repository.transactions(accountId)
            _state.value = _state.value.copy(
                loading = false,
                refreshing = false,
                transactions = transactions,
            )
        } catch (e: ApiException) {
            _state.value = _state.value.copy(loading = false, refreshing = false, error = e.message)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                loading = false,
                refreshing = false,
                error = e.message ?: "Não foi possível carregar as transações.",
            )
        }
    }
}
