package com.ehs.finances.core.settings

import com.ehs.finances.core.i18n.AppLanguage
import com.ehs.finances.core.i18n.Strings
import com.ehs.finances.core.prefs.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppTheme { LIGHT, DARK }

/** Tema e idioma escolhidos pelo usuário, persistidos no aparelho. */
class AppSettings(private val prefs: AppPreferences) {
    private val _theme = MutableStateFlow(
        if (prefs.readString(KEY_THEME) == "dark") AppTheme.DARK else AppTheme.LIGHT,
    )
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    private val _language = MutableStateFlow(
        AppLanguage.fromTag(prefs.readString(KEY_LOCALE)),
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun toggleTheme() {
        val next = if (_theme.value == AppTheme.LIGHT) AppTheme.DARK else AppTheme.LIGHT
        _theme.value = next
        prefs.writeString(KEY_THEME, if (next == AppTheme.DARK) "dark" else "light")
    }

    fun toggleLanguage() {
        val next = AppLanguage.toggle(_language.value)
        _language.value = next
        prefs.writeString(KEY_LOCALE, next.tag)
    }

    fun strings(): Strings = Strings(_language.value)

    private companion object {
        const val KEY_THEME = "ehs-finances-theme"
        const val KEY_LOCALE = "ehs-finances-locale"
    }
}
