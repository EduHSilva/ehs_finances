package com.ehs.finances.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehs.finances.core.i18n.LocalStrings
import com.ehs.finances.core.ui.components.AppPasswordField
import com.ehs.finances.core.ui.components.AppTextField
import com.ehs.finances.core.ui.components.ContentCard
import com.ehs.finances.core.ui.components.ErrorBanner
import com.ehs.finances.core.ui.components.PrimaryButton
import com.ehs.finances.core.ui.components.SettingsActions
import com.ehs.finances.core.ui.containerViewModel
import com.ehs.finances.core.ui.resolve
import com.ehs.finances.core.ui.theme.FinancesTheme

@Composable
fun AuthScreen() {
    val vm: AuthViewModel = containerViewModel { AuthViewModel(it.authRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val context = LocalContext.current
    val isRegister = state.mode == AuthMode.REGISTER

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        FinancesTheme.colors.primarySoft.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(strings["appName"], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                SettingsActions()
            }

            Spacer(Modifier.height(32.dp))
            Text(
                text = strings["landingTitle"],
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = strings["landingSubtitle"],
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            ContentCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = strings[if (isRegister) "registerTitle" else "loginTitle"],
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = strings[if (isRegister) "registerSubtitle" else "loginSubtitle"],
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(18.dp))

                AppTextField(
                    value = state.email,
                    onValueChange = vm::setEmail,
                    label = strings["emailLabel"],
                    placeholder = strings["emailPlaceholder"],
                    keyboardType = KeyboardType.Email,
                )
                Spacer(Modifier.height(12.dp))
                if (isRegister) {
                    AppTextField(
                        value = state.username,
                        onValueChange = vm::setUsername,
                        label = strings["usernameLabel"],
                        placeholder = strings["usernamePlaceholder"],
                    )
                    Spacer(Modifier.height(12.dp))
                }
                AppPasswordField(
                    value = state.password,
                    onValueChange = vm::setPassword,
                    label = strings["passwordLabel"],
                )

                state.error?.let { error ->
                    Spacer(Modifier.height(12.dp))
                    ErrorBanner(error.resolve(strings))
                }

                Spacer(Modifier.height(20.dp))
                PrimaryButton(
                    text = strings[
                        when {
                            state.loading && isRegister -> "registering"
                            state.loading -> "loggingIn"
                            isRegister -> "registerAction"
                            else -> "loginAction"
                        },
                    ],
                    onClick = vm::submit,
                    modifier = Modifier.fillMaxWidth(),
                    loading = state.loading,
                )

                if (!isRegister && GoogleSignIn.isConfigured) {
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(Modifier.weight(1f))
                        Text(
                            text = strings["orDivider"],
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp),
                        )
                        HorizontalDivider(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = { vm.signInWithGoogle(context) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !state.googleLoading && !state.loading,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(MaterialTheme.colorScheme.onSurface, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "G",
                                color = MaterialTheme.colorScheme.surface,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(if (state.googleLoading) strings["loggingIn"] else strings["continueWithGoogle"])
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = strings[if (isRegister) "haveAccount" else "needAccount"],
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = vm::toggleMode, enabled = !state.loading) {
                        Text(strings[if (isRegister) "goToLogin" else "goToRegister"])
                    }
                }
            }

            if (!isRegister) {
                Spacer(Modifier.height(24.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LandingPoint(strings["landingPointOne"])
                    LandingPoint(strings["landingPointTwo"])
                    LandingPoint(strings["landingPointThree"])
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Desenvolvido por EHS Solutions",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    if (state.registered) {
        AlertDialog(
            onDismissRequest = vm::dismissRegistered,
            icon = {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = FinancesTheme.colors.success,
                )
            },
            title = { Text(strings["registerSuccessTitle"]) },
            text = { Text(strings["registerSuccessText"]) },
            confirmButton = {
                TextButton(onClick = vm::dismissRegistered) { Text(strings["loginNow"]) }
            },
        )
    }
}

@Composable
private fun LandingPoint(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = FinancesTheme.colors.primarySoft,
            modifier = Modifier.size(22.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
