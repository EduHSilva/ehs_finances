package com.ehs.finances.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.ehs.finances.core.network.ApiClient
import com.ehs.finances.core.prefs.AppPreferences
import com.ehs.finances.core.session.SessionManager
import com.ehs.finances.core.settings.AppSettings
import com.ehs.finances.data.remote.AuthApi
import com.ehs.finances.data.remote.FinancesApi
import com.ehs.finances.data.repository.AuthRepository
import com.ehs.finances.data.repository.FinanceState
import com.ehs.finances.data.repository.FinancesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json

/** Injeção de dependência manual: uma instância viva enquanto o processo existir. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext

    val preferences = AppPreferences(appContext)
    val settings = AppSettings(preferences)

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    val sessionManager = SessionManager(preferences, json)

    private val apiClient = ApiClient(sessionManager)

    val authRepository = AuthRepository(
        api = apiClient.retrofit.create(AuthApi::class.java),
        session = sessionManager,
    )

    val financesRepository = FinancesRepository(
        api = apiClient.retrofit.create(FinancesApi::class.java),
    )

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val financeState = FinanceState(
        repository = financesRepository,
        prefs = preferences,
        sessionManager = sessionManager,
        scope = appScope,
    )
}

val LocalContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer não fornecido")
}
