package com.ehs.finances.feature.transactions

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.ehs.finances.core.i18n.Strings
import com.ehs.finances.core.ui.EntryTypeTag
import com.ehs.finances.core.ui.MessageEffect
import com.ehs.finances.core.ui.StatusTag
import com.ehs.finances.core.ui.categoryLabel
import com.ehs.finances.core.ui.classificationLabel
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
import com.ehs.finances.core.ui.components.RefreshableColumn
import com.ehs.finances.core.ui.components.ScreenTitle
import com.ehs.finances.core.ui.components.SwitchRow
import com.ehs.finances.core.ui.containerViewModel
import com.ehs.finances.core.ui.theme.FinancesTheme
import com.ehs.finances.data.model.Category
import com.ehs.finances.data.model.CreditCard
import com.ehs.finances.data.model.Transaction
import com.ehs.finances.data.model.TransactionInput
import com.ehs.finances.domain.ApiDates
import com.ehs.finances.domain.Money
import java.math.BigDecimal
import java.time.LocalDate

@Composable
fun TransactionsScreen(
    onOpenImport: (review: Boolean) -> Unit,
    onOpenTransfers: () -> Unit = {},
    onOpenInvoices: () -> Unit = {},
) {
    val vm: TransactionsViewModel = containerViewModel {
        TransactionsViewModel(it.financeState, it.financesRepository)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val container = com.ehs.finances.di.LocalContainer.current
    val account by container.financeState.activeAccount.collectAsStateWithLifecycle()
    val visibleAccounts by container.financeState.visibleAccounts.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    MessageEffect(message, onConsumed = vm::consumeMessage)

    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Transaction?>(null) }
    var deleteTarget by remember { mutableStateOf<Transaction?>(null) }
    var confirmTarget by remember { mutableStateOf<Transaction?>(null) }

    RefreshableColumn(
        isRefreshing = state.refreshing,
        onRefresh = vm::refresh,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle(strings["transactions"], strings["transactionsSubtitle"])

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onOpenImport(false) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                enabled = account != null,
            ) {
                Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings["importStatement"], maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
                onClick = onOpenTransfers,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                enabled = account != null,
            ) {
                Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings["transfers"], maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        androidx.compose.material3.Button(
            onClick = {
                editing = null
                showForm = true
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            enabled = account != null,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(strings["newTransaction"], maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

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
        }

        if (state.reviewCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FinancesTheme.colors.warningSoft, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    strings.format("reviewImportsCount", "count" to state.reviewCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = FinancesTheme.colors.warning,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onOpenImport(true) }) {
                    Text(strings["reviewImports"], color = FinancesTheme.colors.warning)
                }
            }
        }

        OutlinedTextField(
            value = state.search,
            onValueChange = vm::setSearch,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(strings["filterTransactions"]) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppDropdown(
                label = strings["filterCategory"],
                options = listOf(DropdownOption(TransactionsUiState.FILTER_ALL, strings["all"])) +
                    state.categories.map { DropdownOption(it.id, categoryLabel(it, strings)) } +
                    DropdownOption(TransactionsUiState.FILTER_NONE, strings["noCategory"]),
                selectedValue = state.categoryFilter,
                onSelect = vm::setCategoryFilter,
                modifier = Modifier.weight(1f),
            )
            AppDropdown(
                label = strings["filterStatus"],
                options = listOf(
                    DropdownOption(TransactionsUiState.FILTER_ALL, strings["all"]),
                    DropdownOption("OK", strings["transactionStatusOk"]),
                    DropdownOption("PENDING", strings["transactionStatusPending"]),
                    DropdownOption("REVIEW", strings["transactionStatusReview"]),
                    DropdownOption("DELAY", strings["transactionStatusDelay"]),
                    DropdownOption("DELETED", strings["transactionStatusDeleted"]),
                ),
                selectedValue = state.statusFilter,
                onSelect = vm::setStatusFilter,
                modifier = Modifier.weight(1f),
            )
        }

        when {
            state.loading && state.transactions.isEmpty() -> LoadingScreen(strings["loading"])
            state.error != null -> ErrorBanner(
                message = state.error.orEmpty(),
                onRetry = vm::refresh,
                retryLabel = strings["retry"],
            )
            state.filtered.isEmpty() -> EmptyNote(strings["noTransactionsFiltered"])
            else -> ContentCard {
                state.filtered.forEachIndexed { index, transaction ->
                    if (index > 0) HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    TransactionRow(
                        transaction = transaction,
                        categories = state.categories,
                        counterpartName = transaction.counterpartAccountId?.let { id ->
                            visibleAccounts.firstOrNull { it.id == id }?.name
                        },
                        confirming = state.confirmingId == transaction.id,
                        onConfirm = { confirmTarget = transaction },
                        onReview = { onOpenImport(true) },
                        onOpenInvoices = onOpenInvoices,
                        onEdit = {
                            editing = transaction
                            showForm = true
                        },
                        onDelete = { deleteTarget = transaction },
                    )
                }
            }
        }
    }

    if (showForm) {
        TransactionFormDialog(
            transaction = editing,
            categories = state.categories,
            cards = state.cards,
            saving = state.saving,
            onDismiss = { showForm = false },
            onSave = { input, status ->
                vm.save(editing?.id, input, status, editing?.creditCardId)
                showForm = false
            },
        )
    }

    confirmTarget?.let { transaction ->
        ConfirmTransactionDialog(
            transaction = transaction,
            strings = strings,
            onDismiss = { confirmTarget = null },
            onConfirm = { value ->
                vm.confirm(transaction.id, value)
                confirmTarget = null
            },
        )
    }

    deleteTarget?.let { transaction ->
        ConfirmDialog(
            title = strings["confirmDeleteTitle"],
            text = strings["deleteTransactionConfirm"],
            confirmLabel = strings["delete"],
            dismissLabel = strings["cancel"],
            onConfirm = { vm.delete(transaction.id) },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun TransactionRow(
    transaction: Transaction,
    categories: List<Category>,
    counterpartName: String?,
    confirming: Boolean,
    onConfirm: () -> Unit,
    onReview: () -> Unit,
    onOpenInvoices: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val strings = LocalStrings.current
    var menuOpen by remember { mutableStateOf(false) }
    val category = categories.firstOrNull { it.id == transaction.categoryId }
    val categoryText = category?.let { categoryLabel(it, strings) }
        ?: categoryLabel(transaction.category, transaction.categoryKey, strings)

    val accent = when {
        transaction.isCard -> MaterialTheme.colorScheme.primary
        transaction.isTransfer -> FinancesTheme.colors.warning
        transaction.isInvoicePayment -> FinancesTheme.colors.subtle
        transaction.income -> FinancesTheme.colors.success
        else -> FinancesTheme.colors.danger
    }
    val accentSoft = when {
        transaction.isCard -> FinancesTheme.colors.primarySoft
        transaction.isTransfer -> FinancesTheme.colors.warningSoft
        transaction.isInvoicePayment -> FinancesTheme.colors.warningSoft
        transaction.income -> FinancesTheme.colors.successSoft
        else -> FinancesTheme.colors.dangerSoft
    }
    val icon = when {
        transaction.isCard -> Icons.Filled.CreditCard
        transaction.isTransfer -> Icons.Filled.SwapHoriz
        transaction.isInvoicePayment -> Icons.Filled.ReceiptLong
        transaction.income -> Icons.Filled.TrendingUp
        else -> Icons.Filled.TrendingDown
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(accentSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                transaction.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                transactionSubtitle(transaction, categoryText, counterpartName, strings),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusTag(transaction.status)
                EntryTypeTag(transaction.entryType)
            }
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = (if (transaction.income) "+" else "-") + Money.format(transaction.value, strings.locale),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    transaction.isTransfer || transaction.isInvoicePayment -> MaterialTheme.colorScheme.onSurface
                    transaction.income -> FinancesTheme.colors.success
                    else -> FinancesTheme.colors.danger
                },
            )
            if (confirming) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = strings["actions"])
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (transaction.isManagedByService) {
                            // TRANSFER/INVOICE_PAYMENT não passam pelo CRUD de lançamentos.
                            DropdownMenuItem(
                                text = { Text(strings["invoiceOpenDetail"]) },
                                leadingIcon = { Icon(Icons.Filled.ReceiptLong, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    onOpenInvoices()
                                },
                            )
                        } else {
                            if (transaction.status.equals("PENDING", ignoreCase = true)) {
                                DropdownMenuItem(
                                    text = { Text(strings["confirm"]) },
                                    leadingIcon = { Icon(Icons.Filled.Check, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        onConfirm()
                                    },
                                )
                            }
                            if (transaction.status.equals("REVIEW", ignoreCase = true)) {
                                DropdownMenuItem(
                                    text = { Text(strings["reviewImport"]) },
                                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        onReview()
                                    },
                                )
                            }
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
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
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
    }
}

/** Linha de detalhe: data, cartão/contraparte/categoria e classificação. */
private fun transactionSubtitle(
    transaction: Transaction,
    categoryText: String,
    counterpartName: String?,
    strings: Strings,
): String {
    val details = mutableListOf<String>()
    details += ApiDates.format(ApiDates.parse(transaction.executionDate), strings.locale)
    when {
        // Pagamento de fatura não tem categoria: mostra o cartão e o vencimento do ciclo.
        transaction.isInvoicePayment -> {
            transaction.creditCardName?.let { details += "${strings["linkedCard"]}: $it" }
            transaction.invoiceDueDate?.let {
                details += strings.format(
                    "invoiceDueDate",
                    "date" to ApiDates.format(ApiDates.parse(it), strings.locale),
                )
            }
        }
        transaction.isTransfer -> {
            val counterpart = counterpartName
            if (counterpart != null) {
                details += strings.format(
                    if (transaction.income) "entryTransferFrom" else "entryTransferTo",
                    "account" to counterpart,
                )
            }
        }
        else -> {
            details += categoryText
            transaction.creditCardName?.let { details += "${strings["linkedCard"]}: $it" }
            classificationLabel(transaction.classification, strings)?.let { details += it }
        }
    }
    return details.joinToString(" · ")
}

@Composable
private fun TransactionFormDialog(
    transaction: Transaction?,
    categories: List<Category>,
    cards: List<CreditCard>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (input: TransactionInput, status: String) -> Unit,
) {
    val strings = LocalStrings.current
    val initialCategory = remember(transaction) {
        transaction?.categoryId?.takeIf { it.isNotBlank() }
            ?: categories.firstOrNull { it.title == transaction?.category }?.id
    }

    var title by remember { mutableStateOf(transaction?.title.orEmpty()) }
    var value by remember {
        mutableStateOf(transaction?.value?.let { BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }.orEmpty())
    }
    var income by remember { mutableStateOf(transaction?.income ?: false) }
    var categoryId by remember { mutableStateOf(initialCategory.orEmpty()) }
    var date by remember { mutableStateOf(ApiDates.parse(transaction?.executionDate) ?: LocalDate.now()) }
    var pending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // "" = conta/caixa; id do cartão = compra no cartão.
    var creditCardId by remember { mutableStateOf(transaction?.creditCardId.orEmpty()) }

    val categoryOptions = listOf(DropdownOption("", strings["noCategory"])) +
        categories.filter { it.income == income }.map { DropdownOption(it.id, categoryLabel(it, strings)) }
    val cardOptions = listOf(DropdownOption("", strings["cashOption"])) +
        cards.filter { it.isActive }.map { DropdownOption(it.id, it.name) }
    val onCard = creditCardId.isNotBlank()

    FormDialog(
        title = strings[if (transaction == null) "newTransaction" else "editTransaction"],
        onDismiss = onDismiss,
        confirmLabel = strings["save"],
        loading = saving,
        onConfirm = {
            val amount = Money.parse(value)
            error = when {
                title.isBlank() -> "titleRequired"
                amount == null || amount <= 0.0 -> "valueInvalid"
                else -> null
            }
            if (error == null) {
                onSave(
                    TransactionInput(
                        title = title,
                        value = amount!!,
                        income = income,
                        categoryId = categoryId.ifBlank { null },
                        executionDate = date,
                        creditCardId = creditCardId.ifBlank { null },
                    ),
                    if (pending) "PENDING" else "OK",
                )
            }
        },
    ) {
        AppTextField(
            value = title,
            onValueChange = { title = it },
            label = strings["transactionTitleLabel"],
            placeholder = strings["transactionTitlePlaceholder"],
        )
        AppTextField(
            value = value,
            onValueChange = { value = it },
            label = strings["valueLabel"],
            keyboardType = KeyboardType.Decimal,
        )
        SwitchRow(
            title = strings["typeLabel"],
            subtitle = if (income) strings["income"] else strings["expense"],
            checked = income,
            onCheckedChange = { checked ->
                income = checked
                val selected = categories.firstOrNull { it.id == categoryId }
                if (selected != null && selected.income != checked) categoryId = ""
                // Compra no cartão é sempre despesa no v1.
                if (checked) creditCardId = ""
            },
            enabled = !onCard,
        )
        AppDropdown(
            label = strings["paymentMethod"],
            options = if (cards.isEmpty()) listOf(DropdownOption("", strings["cashOption"])) else cardOptions,
            selectedValue = creditCardId,
            onSelect = {
                creditCardId = it
                if (it.isNotBlank()) income = false
            },
            placeholder = strings["cashOption"],
            enabled = cards.isNotEmpty() && !income,
        )
        when {
            cards.isEmpty() -> Text(
                strings["noCreditCardsHint"],
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            onCard -> Text(
                strings["transactionCardSyncHint"],
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AppDropdown(
            label = strings["categoryLabel"],
            options = categoryOptions,
            selectedValue = categoryId,
            onSelect = { categoryId = it },
            placeholder = strings["selectCategory"],
        )
        if (transaction?.recurrence != null) {
            Text(
                strings["recurrenceCategorySyncHint"],
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AppDateField(
            label = strings["dateLabel"],
            date = date,
            onChange = { date = it },
        )
        if (transaction == null) {
            SwitchRow(
                title = strings["pendingTransaction"],
                subtitle = strings["pendingTransactionHelp"],
                checked = pending,
                onCheckedChange = { pending = it },
            )
        }
        FormError(error?.let { strings[it] })
    }
}

@Composable
private fun ConfirmTransactionDialog(
    transaction: Transaction,
    strings: Strings,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var value by remember {
        mutableStateOf(BigDecimal.valueOf(transaction.value).stripTrailingZeros().toPlainString())
    }
    var error by remember { mutableStateOf<String?>(null) }

    FormDialog(
        title = strings["confirmTransactionTitle"],
        onDismiss = onDismiss,
        confirmLabel = strings["confirm"],
        onConfirm = {
            val amount = Money.parse(value)
            if (amount == null || amount <= 0.0) {
                error = "confirmValueInvalid"
            } else {
                onConfirm(amount)
            }
        },
    ) {
        Text(
            "${transaction.title} · ${ApiDates.format(ApiDates.parse(transaction.executionDate), strings.locale)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AppTextField(
            value = value,
            onValueChange = { value = it },
            label = strings["valueLabel"],
            keyboardType = KeyboardType.Decimal,
        )
        Text(
            strings["confirmValueHelp"],
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FormError(error?.let { strings[it] })
    }
}
