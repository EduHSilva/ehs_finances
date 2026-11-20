package com.ehs.finances.feature.recurrences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehs.finances.core.network.ApiException
import com.ehs.finances.core.ui.UiMessage
import com.ehs.finances.data.model.Category
import com.ehs.finances.data.model.Recurrence
import com.ehs.finances.data.model.RecurrenceInput
import com.ehs.finances.data.repository.FinanceState
import com.ehs.finances.data.repository.FinancesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class RecurrencesUiState(
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
    val recurrences: List<Recurrence> = emptyList(),
    val categories: List<Category> = emptyList(),
    val saving: Boolean = false,
    val deletingId: String? = null,
) {
    val monthly: List<Recurrence>
        get() = recurrences.filter { it.type.equals("MONTH", ignoreCase = true) }

    val installments: List<Recurrence>
        get() = recurrences.filterNot { it.type.equals("MONTH", ignoreCase = true) }
}

class RecurrencesViewModel(
    private val financeState: FinanceState,
    private val repository: FinancesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RecurrencesUiState())
    val state: StateFlow<RecurrencesUiState> = _state.asStateFlow()

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    private val reload = MutableStateFlow(0)
    private var categoriesLoaded = false

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

    fun save(
        id: String?,
        input: RecurrenceInput,
        status: String,
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
                    repository.createRecurrence(accountId!!, input, status)
                } else {
                    repository.updateRecurrence(id, input, status)
                }
                _message.value = UiMessage.Res("recurrenceSaved")
                _state.value = _state.value.copy(saving = false)
                reload.value++
            } catch (e: ApiException) {
                _state.value = _state.value.copy(saving = false)
                _message.value = UiMessage.Text(e.message)
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false)
                _message.value = UiMessage.Res("recurrenceSaveFailed")
            }
        }
    }

    fun delete(id: String) {
        if (_state.value.deletingId != null) return
        viewModelScope.launch {
            _state.value = _state.value.copy(deletingId = id)
            try {
                repository.deleteRecurrence(id)
                _message.value = UiMessage.Res("recurrenceDeleted")
                _state.value = _state.value.copy(deletingId = null)
                reload.value++
            } catch (e: ApiException) {
                _state.value = _state.value.copy(deletingId = null)
                _message.value = UiMessage.Text(e.message)
            } catch (e: Exception) {
                _state.value = _state.value.copy(deletingId = null)
                _message.value = UiMessage.Res("recurrenceDeleteFailed")
            }
        }
    }

    private suspend fun load(accountId: String?) {
        if (accountId == null) {
            _state.value = _state.value.copy(
                loading = false,
                refreshing = false,
                error = null,
                recurrences = emptyList(),
            )
            return
        }

        _state.value = _state.value.copy(loading = !_state.value.refreshing, error = null)
        try {
            if (!categoriesLoaded) {
                runCatching { repository.categories() }.getOrNull()?.let { categories ->
                    categoriesLoaded = true
                    _state.value = _state.value.copy(categories = categories)
                }
            }
            val recurrences = repository.recurrences(accountId)
            _state.value = _state.value.copy(
                loading = false,
                refreshing = false,
                recurrences = recurrences,
            )
        } catch (e: ApiException) {
            _state.value = _state.value.copy(loading = false, refreshing = false, error = e.message)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                loading = false,
                refreshing = false,
                error = e.message ?: "Não foi possível carregar as recorrências.",
            )
        }
    }
}
