package com.ehs.finances.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehs.finances.core.network.ApiException
import com.ehs.finances.core.ui.UiMessage
import com.ehs.finances.data.model.FinancialAccountInput
import com.ehs.finances.data.repository.AuthRepository
import com.ehs.finances.data.repository.FinanceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val username: String = "",
    val email: String = "",
    val saving: Boolean = false,
    val loading: Boolean = false,
    val creatingAccount: Boolean = false,
    val accountsError: String? = null,
    val error: UiMessage? = null,
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val financeState: FinanceState,
) : ViewModel() {
    private val session = authRepository.currentSession.value

    private val _state = MutableStateFlow(
        ProfileUiState(
            username = session?.user?.name.orEmpty(),
            email = session?.user?.email.orEmpty(),
        ),
    )
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    fun setUsername(value: String) {
        _state.value = _state.value.copy(username = value, error = null)
    }

    fun consumeMessage() {
        _message.value = null
    }

    /** Recarrega perfil, contas e preferências (equivalente ao onMounted da tela de configurações). */
    fun load() {
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, accountsError = null)
            try {
                runCatching { authRepository.refreshUser() }
                    .onSuccess { syncUser() }
                financeState.load(force = true)
                financeState.error.value?.let { error ->
                    _state.value = _state.value.copy(accountsError = error)
                }
            } finally {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }

    fun saveUsername() {
        val current = _state.value
        if (current.saving) return
        val username = current.username.trim()
        if (username.length < 2) {
            _state.value = current.copy(error = UiMessage.Res("usernameTooShort"))
            return
        }
        if (username == session?.user?.name) {
            _message.value = UiMessage.Res("profileSaveSuccess")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null)
            try {
                authRepository.updateUsername(username)
                syncUser()
                _message.value = UiMessage.Res("profileSaveSuccess")
            } catch (e: ApiException) {
                _message.value = UiMessage.Text(e.message)
            } catch (e: Exception) {
                _message.value = UiMessage.Res("profileSaveFailed")
            } finally {
                _state.value = _state.value.copy(saving = false)
            }
        }
    }

    fun createAccount(name: String, type: String, openingBalance: Double) {
        if (_state.value.creatingAccount) return
        viewModelScope.launch {
            _state.value = _state.value.copy(creatingAccount = true)
            try {
                financeState.createAccount(
                    FinancialAccountInput(
                        name = name.trim(),
                        type = type,
                        openingBalance = openingBalance,
                    ),
                )
                _message.value = UiMessage.Res("accountCreated")
            } catch (e: ApiException) {
                _message.value = UiMessage.Text(e.message)
            } catch (e: Exception) {
                _message.value = UiMessage.Res("accountCreateFailed")
            } finally {
                _state.value = _state.value.copy(creatingAccount = false)
            }
        }
    }

    fun setActiveAccount(accountId: String) = financeState.setActiveAccount(accountId)

    /** Salva a preferência de exibir a conta PJ, como o checkbox da tela de configurações do web. */
    fun setShowBusinessAccount(value: Boolean) {
        if (financeState.preferences.value.showBusinessAccount == value) return
        viewModelScope.launch {
            try {
                financeState.updatePreferences(
                    financeState.preferences.value.copy(showBusinessAccount = value),
                )
            } catch (e: Exception) {
                _message.value = UiMessage.Res("preferencesSaveFailed")
            }
        }
    }

    fun logout() {
        financeState.clear()
        authRepository.logout()
    }

    private fun syncUser() {
        val user = authRepository.currentSession.value?.user ?: return
        _state.value = _state.value.copy(
            username = user.name,
            email = user.email,
        )
    }
}
