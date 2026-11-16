package com.ehs.finances.feature.auth

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehs.finances.core.network.ApiException
import com.ehs.finances.core.ui.UiMessage
import com.ehs.finances.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AuthMode { LOGIN, REGISTER }

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val loading: Boolean = false,
    val googleLoading: Boolean = false,
    val error: UiMessage? = null,
    val registered: Boolean = false,
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun setEmail(value: String) {
        _state.value = _state.value.copy(email = value, error = null)
    }

    fun setPassword(value: String) {
        _state.value = _state.value.copy(password = value, error = null)
    }

    fun setUsername(value: String) {
        _state.value = _state.value.copy(username = value, error = null)
    }

    fun toggleMode() {
        val next = if (_state.value.mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN
        _state.value = _state.value.copy(mode = next, error = null, registered = false)
    }

    fun dismissRegistered() {
        _state.value = _state.value.copy(registered = false, mode = AuthMode.LOGIN, password = "")
    }

    fun submit() {
        val current = _state.value
        if (current.loading) return

        val email = current.email.trim()
        val validation = when {
            !EMAIL_REGEX.matches(email) -> UiMessage.Res("invalidEmail")
            current.password.length < 6 -> UiMessage.Res("passwordTooShort")
            current.mode == AuthMode.REGISTER && current.username.trim().length < 2 ->
                UiMessage.Res("usernameTooShort")
            else -> null
        }
        if (validation != null) {
            _state.value = current.copy(error = validation)
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                if (current.mode == AuthMode.LOGIN) {
                    repository.login(email = email, password = current.password)
                } else {
                    repository.register(
                        email = email,
                        username = current.username,
                        password = current.password,
                    )
                    _state.value = _state.value.copy(registered = true)
                }
            } catch (e: ApiException) {
                _state.value = _state.value.copy(
                    error = UiMessage.Text(
                        e.message.ifBlank {
                            if (current.mode == AuthMode.LOGIN) "Não foi possível entrar." else "Não foi possível criar a conta."
                        },
                    ),
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = UiMessage.Res(
                        if (current.mode == AuthMode.LOGIN) "loginFailed" else "registerFailed",
                    ),
                )
            } finally {
                _state.value = _state.value.copy(loading = false, googleLoading = false)
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        if (_state.value.googleLoading) return
        viewModelScope.launch {
            _state.value = _state.value.copy(googleLoading = true, error = null)
            try {
                val token = GoogleSignIn.idToken(context)
                if (token.isNullOrBlank()) {
                    _state.value = _state.value.copy(error = UiMessage.Res("loginFailed"))
                } else {
                    repository.loginWithGoogle(token)
                }
            } catch (e: GetCredentialCancellationException) {
                // Usuário fechou o seletor de contas: não é erro.
            } catch (e: ApiException) {
                _state.value = _state.value.copy(error = UiMessage.Text(e.message))
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = UiMessage.Res("loginFailed"))
            } finally {
                _state.value = _state.value.copy(googleLoading = false)
            }
        }
    }

    private companion object {
        val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
