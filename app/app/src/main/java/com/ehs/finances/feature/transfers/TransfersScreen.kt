package com.ehs.finances.feature.transfers

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.ehs.finances.core.i18n.LocalStrings
import com.ehs.finances.core.ui.MessageEffect
import com.ehs.finances.core.ui.components.AppDateField
import com.ehs.finances.core.ui.components.AppDropdown
import com.ehs.finances.core.ui.components.AppTextField
import com.ehs.finances.core.ui.components.ConfirmDialog
import com.ehs.finances.core.ui.components.ContentCard
import com.ehs.finances.core.ui.components.DropdownOption
import com.ehs.finances.core.ui.components.EmptyNote
import com.ehs.finances.core.ui.components.ErrorBanner
import com.ehs.finances.core.ui.components.FormDialog
import com.ehs.finances.core.ui.components.FormError
import com.ehs.finances.core.ui.components.LoadingScreen
import com.ehs.finances.core.ui.components.MonthSelector
import com.ehs.finances.core.ui.components.PrimaryButton
import com.ehs.finances.core.ui.components.RefreshableColumn
import com.ehs.finances.core.ui.components.ScreenTitle
import com.ehs.finances.core.ui.components.SectionHeader
import com.ehs.finances.core.ui.containerViewModel
import com.ehs.finances.core.ui.resolve
import com.ehs.finances.core.ui.theme.FinancesTheme
import com.ehs.finances.data.model.FinancialAccount
import com.ehs.finances.data.model.Transfer
import com.ehs.finances.data.model.TransferInput
import com.ehs.finances.domain.ApiDates
import com.ehs.finances.domain.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun TransfersScreen(onBack: () -> Unit) {
    val vm: TransfersViewModel = containerViewModel {
        TransfersViewModel(it.financeState, it.financesRepository)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val container = com.ehs.finances.di.LocalContainer.current
    val activeAccountId by container.financeState.activeAccountId.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    MessageEffect(message, onConsumed = vm::consumeMessage)

    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Transfer?>(null) }
    var deleteTarget by remember { mutableStateOf<Transfer?>(null) }

    RefreshableColumn(
        isRefreshing = state.refreshing,
        onRefresh = vm::refresh,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle(
            title = strings["transfers"],
            subtitle = strings["transfersSubtitle"],
            action = {
                OutlinedButton(onClick = onBack, shape = RoundedCornerShape(12.dp)) {
                    Text(strings["goBack"])
                }
            },
        )

        PrimaryButton(
            text = strings["newTransfer"],
            onClick = {
                editing = null
                showForm = true
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.accountId != null && state.accounts.size >= 2,
        )

        if (state.accounts.size < 2) {
            ContentCard {
                Text(
                    strings["transferNoDestinations"],
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        MonthSelector(
            month = state.month,
            onPrevious = { vm.setMonth(state.month.minusMonths(1)) },
            onNext = { vm.setMonth(state.month.plusMonths(1)) },
        )

        when {
            state.loading && state.transfers.isEmpty() -> LoadingScreen(strings["loading"])
            state.error != null -> ErrorBanner(
                message = state.error.orEmpty(),
                onRetry = vm::refresh,
                retryLabel = strings["retry"],
            )
            else -> ContentCard {
                SectionHeader(strings["transfers"])
                Spacer(Modifier.height(8.dp))
                val items = state.filtered
                if (items.isEmpty()) {
                    EmptyNote(strings["noTransfers"])
                } else {
                    items.forEachIndexed { index, transfer ->
                        if (index > 0) HorizontalDivider(Modifier.padding(vertical = 10.dp))
                        TransferRow(
                            transfer = transfer,
                            fromName = state.accountName(transfer.fromAccountId) ?: "—",
                            toName = state.accountName(transfer.toAccountId) ?: "—",
                            deleting = state.deletingId == transfer.transferId,
                            onEdit = {
                                editing = transfer
                                showForm = true
                            },
                            onDelete = { deleteTarget = transfer },
                        )
                    }
                }
            }
        }
    }

    if (showForm) {
        TransferFormDialog(
            transfer = editing,
            accounts = state.accounts,
            defaultAccountId = editing?.fromAccountId ?: activeAccountId ?: state.accountId,
            saving = state.saving,
            onDismiss = { showForm = false },
            onSave = { input ->
                vm.save(editing?.transferId, input)
                showForm = false
            },
        )
    }

    deleteTarget?.let { transfer ->
        ConfirmDialog(
            title = strings["confirmDeleteTitle"],
            text = strings["transferDeleteConfirm"],
            confirmLabel = strings["delete"],
            dismissLabel = strings["cancel"],
            onConfirm = { vm.delete(transfer.transferId) },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun TransferRow(
    transfer: Transfer,
    fromName: String,
    toName: String,
    deleting: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val strings = LocalStrings.current
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(FinancesTheme.colors.warningSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.SwapHoriz,
                contentDescription = null,
                tint = FinancesTheme.colors.warning,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                transfer.title?.takeIf { it.isNotBlank() } ?: strings["transfers"],
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOf(
                    ApiDates.format(ApiDates.parse(transfer.date), strings.locale),
                    "$fromName → $toName",
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                Money.format(transfer.value, strings.locale),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Box {
                IconButton(onClick = { menuOpen = true }, enabled = !deleting) {
                    Icon(Icons.Filled.MoreVert, contentDescription = strings["actions"])
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(strings["edit"]) },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(strings["delete"], color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TransferFormDialog(
    transfer: Transfer?,
    accounts: List<FinancialAccount>,
    defaultAccountId: String?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (TransferInput) -> Unit,
) {
    val strings = LocalStrings.current
    val accountOptions = accounts.map { DropdownOption(it.id, "${it.type} · ${it.name}") }

    var fromAccountId by remember { mutableStateOf(transfer?.fromAccountId ?: defaultAccountId.orEmpty()) }
    var toAccountId by remember {
        mutableStateOf(
            transfer?.toAccountId ?: accounts.firstOrNull { it.id != defaultAccountId }?.id.orEmpty(),
        )
    }
    var value by remember {
        mutableStateOf(transfer?.value?.let { BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }.orEmpty())
    }
    var date by remember { mutableStateOf(ApiDates.parse(transfer?.date) ?: LocalDate.now()) }
    var title by remember { mutableStateOf(transfer?.title.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    FormDialog(
        title = strings[if (transfer == null) "newTransfer" else "editTransfer"],
        onDismiss = onDismiss,
        confirmLabel = strings["save"],
        loading = saving,
        onConfirm = {
            val amount = Money.parse(value)
            error = when {
                amount == null || amount <= 0.0 -> "valueInvalid"
                fromAccountId.isBlank() || toAccountId.isBlank() -> "transferSameAccount"
                fromAccountId == toAccountId -> "transferSameAccount"
                else -> null
            }
            if (error == null) {
                onSave(
                    TransferInput(
                        fromAccountId = fromAccountId,
                        toAccountId = toAccountId,
                        value = amount!!,
                        date = date,
                        title = title.ifBlank { null },
                    ),
                )
            }
        },
    ) {
        AppDropdown(
            label = strings["transferFrom"],
            options = accountOptions,
            selectedValue = fromAccountId,
            onSelect = { fromAccountId = it },
            enabled = transfer == null,
        )
        AppDropdown(
            label = strings["transferTo"],
            options = accountOptions,
            selectedValue = toAccountId,
            onSelect = { toAccountId = it },
        )
        AppTextField(
            value = value,
            onValueChange = {
                value = it
                error = null
            },
            label = strings["valueLabel"],
            keyboardType = KeyboardType.Decimal,
        )
        AppDateField(
            label = strings["dateLabel"],
            date = date,
            onChange = { date = it },
        )
        AppTextField(
            value = title,
            onValueChange = { title = it },
            label = strings["transferTitle"],
            placeholder = strings["transferTitlePlaceholder"],
        )
        if (transfer != null) {
            Text(
                strings["transferOriginLocked"],
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FormError(error?.let { strings[it] })
    }
}
