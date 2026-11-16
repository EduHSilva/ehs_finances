package com.ehs.finances.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ehs.finances.core.i18n.LocalStrings
import com.ehs.finances.di.AppContainer
import com.ehs.finances.di.LocalContainer
import kotlinx.coroutines.flow.StateFlow

/** Cria um ViewModel ligado ao [AppContainer] sem fábricas repetidas em cada tela. */
@Composable
inline fun <reified T : ViewModel> containerViewModel(
    crossinline create: (AppContainer) -> T,
): T {
    val container = LocalContainer.current
    val factory = remember(container) {
        viewModelFactory {
            initializer { create(container) }
        }
    }
    return viewModel(factory = factory)
}

/** Mostra a mensagem pendente do ViewModel no Snackbar global e a consome. */
@Composable
fun MessageEffect(
    message: UiMessage?,
    onConsumed: () -> Unit,
) {
    val strings = LocalStrings.current
    val snackbar = LocalSnackbarHost.current
    val context = LocalContext.current
    LaunchedEffect(message, context) {
        if (message != null) {
            snackbar.showSnackbar(message.resolve(strings))
            onConsumed()
        }
    }
}

@Composable
fun <T> collectState(flow: StateFlow<T>): T = flow.collectAsStateWithLifecycle().value
