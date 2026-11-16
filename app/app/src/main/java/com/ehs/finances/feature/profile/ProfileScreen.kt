package com.ehs.finances.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehs.finances.BuildConfig
import com.ehs.finances.core.i18n.AppLanguage
import com.ehs.finances.core.i18n.LocalStrings
import com.ehs.finances.core.settings.AppTheme
import com.ehs.finances.core.settings.LocalAppSettings
import com.ehs.finances.core.ui.MessageEffect
import com.ehs.finances.core.ui.components.AppDropdown
import com.ehs.finances.core.ui.components.AppTextField
import com.ehs.finances.core.ui.components.ConfirmDialog
import com.ehs.finances.core.ui.components.ContentCard
import com.ehs.finances.core.ui.components.DropdownOption
import com.ehs.finances.core.ui.components.EmptyNote
import com.ehs.finances.core.ui.components.FormDialog
import com.ehs.finances.core.ui.components.FormError
import com.ehs.finances.core.ui.components.InitialAvatar
import com.ehs.finances.core.ui.components.PrimaryButton
import com.ehs.finances.core.ui.components.ScreenTitle
import com.ehs.finances.core.ui.components.SectionHeader
import com.ehs.finances.core.ui.components.SwitchRow
import com.ehs.finances.core.ui.containerViewModel
import com.ehs.finances.core.ui.resolve
import com.ehs.finances.core.ui.theme.FinancesTheme
import com.ehs.finances.data.model.FinancialAccount
import com.ehs.finances.domain.Money

@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val container = com.ehs.finances.di.LocalContainer.current
    val vm: ProfileViewModel = containerViewModel {
        ProfileViewModel(it.authRepository, it.financeState)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val accounts by container.financeState.visibleAccounts.collectAsStateWithLifecycle()
    val activeAccountId by container.financeState.activeAccountId.collectAsStateWithLifecycle()
    val preferences by container.financeState.preferences.collectAsStateWithLifecycle()
    val settings = LocalAppSettings.current
    val theme by settings.theme.collectAsStateWithLifecycle()
    val language by settings.language.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    MessageEffect(message, onConsumed = vm::consumeMessage)
    LaunchedEffect(Unit) { vm.load() }

    var showAccountForm by remember { mutableStateOf(false) }
    var showLogout by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle(
            title = strings["profile"],
            subtitle = strings["profileSubtitle"],
            action = {
                OutlinedButton(onClick = onBack, shape = RoundedCornerShape(12.dp)) {
                    Text(strings["goBack"])
                }
            },
        )

        ContentCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InitialAvatar(name = state.username.ifBlank { state.email }, size = 54.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        state.username.ifBlank { state.email },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        state.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            AppTextField(
                value = state.username,
                onValueChange = vm::setUsername,
                label = strings["profileNameLabel"],
                placeholder = strings["usernamePlaceholder"],
            )
            state.error?.let { error ->
                Spacer(Modifier.height(8.dp))
                FormError(error.resolve(strings))
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                text = if (state.saving) strings["profileSaving"] else strings["profileSave"],
                onClick = vm::saveUsername,
                loading = state.saving,
                enabled = state.username.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        ContentCard {
            SectionHeader(
                title = strings["accounts"],
                subtitle = strings["activeAccountLabel"],
                action = {
                    OutlinedButton(
                        onClick = { showAccountForm = true },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(strings["newAccount"])
                    }
                },
            )
            Spacer(Modifier.height(10.dp))
            state.accountsError?.let { error ->
                Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    color = FinancesTheme.colors.danger,
                )
                Spacer(Modifier.height(8.dp))
            }
            if (accounts.isEmpty()) {
                EmptyNote(strings["noAccounts"])
                Spacer(Modifier.height(8.dp))
                Text(
                    strings["createAccountFirst"],
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                accounts.forEachIndexed { index, account ->
                    if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    AccountRow(
                        account = account,
                        selected = account.id == activeAccountId,
                        onSelect = { vm.setActiveAccount(account.id) },
                    )
                }
            }
        }

        ContentCard {
            SectionHeader(
                title = strings["preferences"],
                subtitle = strings["showBusinessAccountDescription"],
            )
            Spacer(Modifier.height(10.dp))
            SwitchRow(
                title = strings["showBusinessAccount"],
                subtitle = null,
                checked = preferences.showBusinessAccount,
                onCheckedChange = vm::setShowBusinessAccount,
            )
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            SectionHeader(strings["appearance"])
            Spacer(Modifier.height(10.dp))
            SwitchRow(
                title = strings["themeLabel"],
                subtitle = strings[if (theme == AppTheme.DARK) "themeDark" else "themeLight"],
                checked = theme == AppTheme.DARK,
                onCheckedChange = { settings.toggleTheme() },
            )
            Spacer(Modifier.height(8.dp))
            SwitchRow(
                title = strings["languageLabel"],
                subtitle = if (language == AppLanguage.EN) "English" else "Português (BR)",
                checked = language == AppLanguage.EN,
                onCheckedChange = { settings.toggleLanguage() },
            )
        }

        OutlinedButton(
            onClick = { showLogout = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(strings["logoutAction"], color = MaterialTheme.colorScheme.error)
        }

        Text(
            text = "${strings["versionLabel"]} ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
    }

    if (showAccountForm) {
        AccountFormDialog(
            saving = state.creatingAccount,
            onDismiss = { showAccountForm = false },
            onSave = { name, type, balance ->
                vm.createAccount(name, type, balance)
                showAccountForm = false
            },
        )
    }

    if (showLogout) {
        ConfirmDialog(
            title = strings["logoutAction"],
            text = strings["logoutConfirm"],
            confirmLabel = strings["logoutAction"],
            dismissLabel = strings["cancel"],
            onConfirm = vm::logout,
            onDismiss = { showLogout = false },
        )
    }
}

@Composable
private fun AccountRow(
    account: FinancialAccount,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) FinancesTheme.colors.primarySoft else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                account.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${account.type} · ${Money.format(account.balance, strings.locale)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AccountFormDialog(
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, openingBalance: Double) -> Unit,
) {
    val strings = LocalStrings.current
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("PF") }
    var openingBalance by remember { mutableStateOf("0") }
    var error by remember { mutableStateOf<String?>(null) }

    FormDialog(
        title = strings["newAccount"],
        onDismiss = onDismiss,
        confirmLabel = strings["save"],
        loading = saving,
        onConfirm = {
            val balance = Money.parse(openingBalance)
            error = when {
                name.trim().length < 2 -> "titleRequired"
                balance == null -> "valueInvalid"
                else -> null
            }
            if (error == null) onSave(name, type, balance ?: 0.0)
        },
    ) {
        AppTextField(
            value = name,
            onValueChange = { name = it },
            label = strings["accountName"],
        )
        AppDropdown(
            label = strings["accountType"],
            options = listOf(
                DropdownOption("PF", "PF"),
                DropdownOption("PJ", "PJ"),
            ),
            selectedValue = type,
            onSelect = { type = it },
        )
        AppTextField(
            value = openingBalance,
            onValueChange = { openingBalance = it },
            label = strings["openingBalance"],
            keyboardType = KeyboardType.Decimal,
        )
        FormError(error?.let { strings[it] })
    }
}
