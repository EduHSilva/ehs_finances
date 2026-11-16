package com.ehs.finances.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.SnackbarHostState
import com.ehs.finances.core.i18n.Strings

/** Mensagem de feedback: chave traduzível ou texto pronto (erros da API). */
sealed interface UiMessage {
    data class Res(val key: String, val args: Map<String, Any> = emptyMap()) : UiMessage

    data class Text(val value: String) : UiMessage
}

fun UiMessage.resolve(strings: Strings): String = when (this) {
    is UiMessage.Text -> value
    is UiMessage.Res -> args.entries.fold(strings[key]) { text, (name, value) ->
        text.replace("{$name}", value.toString())
    }
}

val LocalSnackbarHost = staticCompositionLocalOf<SnackbarHostState> {
    error("SnackbarHostState não fornecido")
}
