package com.ehs.finances.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehs.finances.core.network.ApiException
import com.ehs.finances.core.ui.UiMessage
import com.ehs.finances.data.model.Category
import com.ehs.finances.data.model.CategoryInput
import com.ehs.finances.data.repository.FinancesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
    val categories: List<Category> = emptyList(),
    val search: String = "",
    val typeFilter: String = TYPE_ALL,
    val saving: Boolean = false,
    val deletingId: String? = null,
) {
    val filtered: List<Category>
        get() {
            val term = search.trim().lowercase()
            return categories.filter { category ->
                val matchesType = when (typeFilter) {
                    TYPE_INCOME -> category.income
                    TYPE_EXPENSE -> !category.income
                    else -> true
                }
                val matchesSearch = term.isEmpty() || category.title.lowercase().contains(term)
                matchesType && matchesSearch
            }
        }

    companion object {
        const val TYPE_ALL = "__all"
        const val TYPE_INCOME = "income"
        const val TYPE_EXPENSE = "expense"
    }
}

class CategoriesViewModel(private val repository: FinancesRepository) : ViewModel() {
    private val _state = MutableStateFlow(CategoriesUiState())
    val state: StateFlow<CategoriesUiState> = _state.asStateFlow()

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    init {
        load()
    }

    fun load(silent: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                loading = !silent,
                refreshing = silent,
                error = null,
            )
            try {
                val categories = repository.categories()
                _state.value = _state.value.copy(
                    loading = false,
                    refreshing = false,
                    categories = categories,
                )
            } catch (e: ApiException) {
                _state.value = _state.value.copy(
                    loading = false,
                    refreshing = false,
                    error = e.message,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    refreshing = false,
                    error = e.message ?: "Não foi possível carregar as categorias.",
                )
            }
        }
    }

    fun refresh() = load(silent = true)

    fun setSearch(value: String) {
        _state.value = _state.value.copy(search = value)
    }

    fun setTypeFilter(value: String) {
        _state.value = _state.value.copy(typeFilter = value)
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun save(id: String?, input: CategoryInput) {
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true)
            try {
                if (id == null) repository.createCategory(input)
                else repository.updateCategory(id, input)
                _message.value = UiMessage.Res("categorySaved")
                _state.value = _state.value.copy(saving = false)
                load()
            } catch (e: ApiException) {
                _state.value = _state.value.copy(saving = false)
                _message.value = UiMessage.Text(e.message)
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false)
                _message.value = UiMessage.Res("categorySaveFailed")
            }
        }
    }

    fun delete(id: String) {
        if (_state.value.deletingId != null) return
        viewModelScope.launch {
            _state.value = _state.value.copy(deletingId = id)
            try {
                repository.deleteCategory(id)
                _message.value = UiMessage.Res("categoryDeleted")
                _state.value = _state.value.copy(deletingId = null)
                load()
            } catch (e: ApiException) {
                _state.value = _state.value.copy(deletingId = null)
                _message.value = UiMessage.Text(e.message)
            } catch (e: Exception) {
                _state.value = _state.value.copy(deletingId = null)
                _message.value = UiMessage.Res("categoryDeleteFailed")
            }
        }
    }
}
