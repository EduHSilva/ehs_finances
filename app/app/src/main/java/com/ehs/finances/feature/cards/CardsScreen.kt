package com.ehs.finances.feature.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
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
import com.ehs.finances.core.ui.cardStatusLabel
import com.ehs.finances.core.ui.components.AppTextField
import com.ehs.finances.core.ui.components.ConfirmDialog
import com.ehs.finances.core.ui.components.ContentCard
import com.ehs.finances.core.ui.components.EmptyNote
import com.ehs.finances.core.ui.components.ErrorBanner
import com.ehs.finances.core.ui.components.FormDialog
import com.ehs.finances.core.ui.components.FormError
import com.ehs.finances.core.ui.components.LoadingScreen
import com.ehs.finances.core.ui.components.PrimaryButton
import com.ehs.finances.core.ui.components.RefreshableColumn
import com.ehs.finances.core.ui.components.ScreenTitle
import com.ehs.finances.core.ui.components.SectionHeader
import com.ehs.finances.core.ui.components.Tag
import com.ehs.finances.core.ui.containerViewModel
import com.ehs.finances.core.ui.theme.FinancesTheme
import com.ehs.finances.data.model.CreditCard
import com.ehs.finances.data.model.CreditCardInput
import com.ehs.finances.domain.Money
import java.math.BigDecimal

@Composable
fun CardsScreen(
    onBack: () -> Unit,
    onOpenInvoices: (cardId: String) -> Unit,
) {
    val vm: CardsViewModel = containerViewModel {
        CardsViewModel(it.financeState, it.financesRepository)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val container = com.ehs.finances.di.LocalContainer.current
    val account by container.financeState.activeAccount.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    MessageEffect(message, onConsumed = vm::consumeMessage)

    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CreditCard?>(null) }
    var deleteTarget by remember { mutableStateOf<CreditCard?>(null) }

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
            title = strings["cards"],
            subtitle = strings["cardsSubtitle"],
            action = {
                OutlinedButton(onClick = onBack, shape = RoundedCornerShape(12.dp)) {
                    Text(strings["goBack"])
                }
            },
        )

        PrimaryButton(
            text = strings["newCard"],
            onClick = {
                editing = null
                showForm = true
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = account != null,
        )

        if (account == null) {
            ContentCard {
                Text(strings["noAccounts"], style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    strings["createAccountFirst"],
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            ContentCard {
                SectionHeader(
                    title = strings["openInvoices"],
                    subtitle = strings["openInvoicesHelp"],
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    Money.format(state.openInvoicesTotal, strings.locale),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            when {
                state.loading && state.cards.isEmpty() -> LoadingScreen(strings["loading"])
                state.error != null -> ErrorBanner(
                    message = state.error.orEmpty(),
                    onRetry = vm::refresh,
                    retryLabel = strings["retry"],
                )
                state.cards.isEmpty() -> ContentCard {
                    SectionHeader(strings["cards"])
                    Spacer(Modifier.height(8.dp))
                    EmptyNote(strings["noCards"])
                }
                else -> ContentCard {
                    SectionHeader(strings["cards"])
                    Spacer(Modifier.height(4.dp))
                    state.cards.forEachIndexed { index, card ->
                        if (index > 0) HorizontalDivider(Modifier.padding(vertical = 10.dp))
                        CardRow(
                            card = card,
                            deleting = state.deletingId == card.id,
                            onOpen = { onOpenInvoices(card.id) },
                            onEdit = {
                                editing = card
                                showForm = true
                            },
                            onDelete = { deleteTarget = card },
                        )
                    }
                }
            }
        }
    }

    if (showForm) {
        CardFormDialog(
            card = editing,
            saving = state.saving,
            onDismiss = { showForm = false },
            onSave = { input ->
                vm.save(editing?.id, input)
                showForm = false
            },
        )
    }

    deleteTarget?.let { card ->
        ConfirmDialog(
            title = strings["confirmDeleteTitle"],
            text = strings["cardDeleteConfirm"],
            confirmLabel = strings["delete"],
            dismissLabel = strings["cancel"],
            onConfirm = { vm.delete(card.id) },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun CardRow(
    card: CreditCard,
    deleting: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val strings = LocalStrings.current
    var menuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(FinancesTheme.colors.primarySoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.CreditCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    card.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOf(
                        strings.format("cardClosingShort", "day" to card.closingDay),
                        strings.format("cardDueShort", "day" to card.dueDay),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    Money.format(card.openInvoiceTotal, strings.locale),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    strings["cardOpenInvoices"],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }, enabled = !deleting) {
                    Icon(Icons.Filled.MoreVert, contentDescription = strings["actions"])
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(strings["invoiceOpenDetail"]) },
                        leadingIcon = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onOpen()
                        },
                    )
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val usage = card.limitUsage
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (card.cardLimit == null) {
                        strings["cardNoLimit"]
                    } else {
                        "${strings["cardLimit"]} ${Money.format(card.cardLimit, strings.locale)} · " +
                            "${strings["cardLimitUsage"]} ${usage?.let { "${(it * 100).toInt().coerceIn(0, 999)}%" } ?: "—"}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (usage != null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .background(FinancesTheme.colors.border, RoundedCornerShape(3.dp)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(usage.coerceIn(0.0, 1.0).toFloat())
                                .fillMaxHeight()
                                .background(
                                    if (usage >= 0.9) FinancesTheme.colors.danger else MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(3.dp),
                                ),
                        )
                    }
                }
            }
            if (!card.isActive) {
                Tag(
                    text = cardStatusLabel(card.status, strings),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    background = FinancesTheme.colors.warningSoft,
                )
            }
        }
    }
}

@Composable
private fun CardFormDialog(
    card: CreditCard?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (CreditCardInput) -> Unit,
) {
    val strings = LocalStrings.current

    var name by remember { mutableStateOf(card?.name.orEmpty()) }
    var limit by remember {
        mutableStateOf(card?.cardLimit?.let { BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }.orEmpty())
    }
    var closingDay by remember { mutableStateOf((card?.closingDay ?: 1).toString()) }
    var dueDay by remember { mutableStateOf((card?.dueDay ?: 10).toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    FormDialog(
        title = strings[if (card == null) "newCard" else "editCard"],
        onDismiss = onDismiss,
        confirmLabel = strings["save"],
        loading = saving,
        onConfirm = {
            val closing = closingDay.trim().toIntOrNull()
            val due = dueDay.trim().toIntOrNull()
            val parsedLimit = if (limit.isBlank()) null else Money.parse(limit)
            error = when {
                name.isBlank() -> "titleRequired"
                closing == null || closing !in 1..31 -> "cardDayInvalid"
                due == null || due !in 1..31 -> "cardDayInvalid"
                limit.isNotBlank() && (parsedLimit == null || parsedLimit <= 0.0) -> "cardLimitInvalid"
                else -> null
            }
            if (error == null) {
                onSave(
                    CreditCardInput(
                        name = name,
                        cardLimit = parsedLimit,
                        closingDay = closing!!,
                        dueDay = due!!,
                    ),
                )
            }
        },
    ) {
        AppTextField(
            value = name,
            onValueChange = { name = it },
            label = strings["cardName"],
            placeholder = strings["cardNamePlaceholder"],
        )
        AppTextField(
            value = limit,
            onValueChange = { limit = it },
            label = strings["cardLimit"],
            keyboardType = KeyboardType.Decimal,
        )
        Text(
            strings["cardLimitHelp"],
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTextField(
                value = closingDay,
                onValueChange = { closingDay = it.filter(Char::isDigit).take(2) },
                label = strings["cardClosingDay"],
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            AppTextField(
                value = dueDay,
                onValueChange = { dueDay = it.filter(Char::isDigit).take(2) },
                label = strings["cardDueDay"],
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }
        FormError(error?.let { strings[it] })
    }
}
