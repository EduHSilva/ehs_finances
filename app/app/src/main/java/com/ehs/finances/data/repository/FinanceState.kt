package com.ehs.finances.data.repository

import com.ehs.finances.core.network.ApiException
import com.ehs.finances.core.prefs.AppPreferences
import com.ehs.finances.core.session.SessionManager
import com.ehs.finances.data.model.FinancePreferences
import com.ehs.finances.data.model.FinancialAccount
import com.ehs.finances.data.model.FinancialAccountInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado compartilhado de contas e preferências (equivalente ao
 * `useFinancialAccounts` + `useFinancePreferences` do web).
 *
 * A conta ativa e as preferências são gravadas por usuário, como os cookies do web.
 */
class FinanceState(
    private val repository: FinancesRepository,
    private val prefs: AppPreferences,
    private val sessionManager: SessionManager,
    private val scope: CoroutineScope,
) {
    private val _accounts = MutableStateFlow<List<FinancialAccount>>(emptyList())
    val accounts: StateFlow<List<FinancialAccount>> = _accounts.asStateFlow()

    private val _preferences = MutableStateFlow(FinancePreferences())
    val preferences: StateFlow<FinancePreferences> = _preferences.asStateFlow()

    private val _activeAccountId = MutableStateFlow<String?>(null)
    val activeAccountId: StateFlow<String?> = _activeAccountId.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val visibleAccounts: StateFlow<List<FinancialAccount>> =
        combine(_accounts, _preferences) { accounts, preferences ->
            accounts.filter { account ->
                !account.type.equals("PJ", ignoreCase = true) || preferences.showBusinessAccount
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val activeAccount: StateFlow<FinancialAccount?> =
        combine(_accounts, _activeAccountId) { accounts, id ->
            accounts.firstOrNull { it.id == id }
        }.stateIn(scope, SharingStarted.Eagerly, null)

    private var loadedFor: String? = null

    init {
        scope.launch {
            combine(_accounts, _preferences) { accounts, preferences ->
                accounts.filter { account ->
                    !account.type.equals("PJ", ignoreCase = true) || preferences.showBusinessAccount
                }
            }.collect { visible ->
                val current = _activeAccountId.value
                if (current == null || visible.none { it.id == current }) {
                    val next = visible.firstOrNull()?.id
                    if (next != current) {
                        _activeAccountId.value = next
                        prefs.writeString(activeAccountKey(), next)
                    }
                }
            }
        }
    }

    /** Carrega preferências e contas; usa o cache local enquanto a API responde. */
    suspend fun load(force: Boolean = false) {
        val user = userId()
        if (_loading.value) return
        if (!force && loadedFor == user) return

        _activeAccountId.value = prefs.readString(activeAccountKey())
        _preferences.value = readCachedPreferences()
        _loading.value = true
        _error.value = null

        try {
            coroutineScope {
                val preferencesDeferred = async { runCatching { repository.preferences() } }
                val accountsDeferred = async { repository.accounts() }
                val preferencesResult = preferencesDeferred.await()
                val accounts = accountsDeferred.await()

                preferencesResult.getOrNull()?.let { value ->
                    _preferences.value = value
                    writeCachedPreferences(value)
                }
                _accounts.value = accounts
                loadedFor = user
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            _error.value = e.message
        } catch (e: Exception) {
            _error.value = e.message ?: "Não foi possível carregar as contas."
        } finally {
            _loading.value = false
        }
    }

    suspend fun refresh() = load(force = true)

    suspend fun updatePreferences(value: FinancePreferences) {
        val saved = repository.updatePreferences(value)
        _preferences.value = saved
        writeCachedPreferences(saved)
    }

    suspend fun createAccount(input: FinancialAccountInput): FinancialAccount {
        val account = repository.createAccount(input)
        if (account.type.equals("PJ", ignoreCase = true) && !_preferences.value.showBusinessAccount) {
            runCatching {
                updatePreferences(_preferences.value.copy(showBusinessAccount = true))
            }
        }
        _accounts.value = _accounts.value + account
        setActiveAccount(account.id)
        return account
    }

    fun setActiveAccount(accountId: String) {
        if (_accounts.value.none { it.id == accountId }) return
        _activeAccountId.value = accountId
        prefs.writeString(activeAccountKey(), accountId)
    }

    /** Chamado ao sair: evita exibir dados do usuário anterior no próximo login. */
    fun clear() {
        _accounts.value = emptyList()
        _preferences.value = FinancePreferences()
        _activeAccountId.value = null
        _error.value = null
        loadedFor = null
    }

    private fun userId(): String =
        sessionManager.session.value?.user?.id?.takeIf { it.isNotBlank() } ?: "anonymous"

    private fun activeAccountKey(): String = "finance-active-account-${userId()}"

    private fun cachedPreferencesKey(): String = "finance-preferences-${userId()}"

    private fun readCachedPreferences(): FinancePreferences {
        val show = prefs.readString("${cachedPreferencesKey()}-show-business")
            ?.toBooleanStrictOrNull()
            ?: true
        val currency = prefs.readString("${cachedPreferencesKey()}-currency") ?: "BRL"
        return FinancePreferences(showBusinessAccount = show, currency = currency)
    }

    private fun writeCachedPreferences(value: FinancePreferences) {
        prefs.writeString("${cachedPreferencesKey()}-show-business", value.showBusinessAccount.toString())
        prefs.writeString("${cachedPreferencesKey()}-currency", value.currency)
    }
}
