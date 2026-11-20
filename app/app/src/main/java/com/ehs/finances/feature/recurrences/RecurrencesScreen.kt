package com.ehs.finances.feature.recurrences

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehs.finances.core.i18n.LocalStrings
import com.ehs.finances.core.ui.MessageEffect
import com.ehs.finances.core.ui.StatusTag
import com.ehs.finances.core.ui.categoryLabel
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
import com.ehs.finances.core.ui.components.PrimaryButton
import com.ehs.finances.core.ui.components.RefreshableColumn
import com.ehs.finances.core.ui.components.ScreenTitle
import com.ehs.finances.core.ui.components.SectionHeader
import com.ehs.finances.core.ui.components.SwitchRow
import com.ehs.finances.core.ui.containerViewModel
import com.ehs.finances.core.ui.theme.FinancesTheme
import com.ehs.finances.data.model.Category
import com.ehs.finances.data.model.Recurrence
import com.ehs.finances.data.model.RecurrenceInput
import com.ehs.finances.domain.Money
import java.math.BigDecimal

@Composable
fun RecurrencesScreen() {
    val vm: RecurrencesViewModel = containerViewModel {
        RecurrencesViewModel(it.financeState, it.financesRepository)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val container = com.ehs.finances.di.LocalContainer.current
    val account by container.financeState.activeAccount.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    MessageEffect(message, onConsumed = vm::consumeMessage)

    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Recurrence?>(null) }
    var deleteTarget by remember { mutableStateOf<Recurrence?>(null) }

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
            title = strings["recurrences"],
            subtitle = strings["recurrencesSubtitle"],
            action = {
                PrimaryButton(
                    text = strings["newRecurrence"],
                    onClick = {
                        editing = null
                        showForm = true
                    },
                    enabled = account != null,
                )
            },
        )

        when {
            account == null -> ContentCard {
                Text(strings["noAccounts"], style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    strings["createAccountFirst"],
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.loading && state.recurrences.isEmpty() -> LoadingScreen(strings["loading"])
            state.error != null -> ErrorBanner(
                message = state.error.orEmpty(),
                onRetry = vm::refresh,
                retryLabel = strings["retry"],
            )
            else -> {
                ContentCard {
                    SectionHeader(strings["recurrences"])
                    Spacer(Modifier.height(8.dp))
                    if (state.monthly.isEmpty()) {
                        EmptyNote(strings["noRecurrences"])
                    } else {
                        state.monthly.forEachIndexed { index, recurrence ->
                            if (index > 0) HorizontalDivider(Modifier.padding(vertical = 10.dp))
                            RecurrenceRow(
                                recurrence = recurrence,
                                categories = state.categories,
                                onEdit = {
                                    editing = recurrence
                                    showForm = true
                                },
                                onDelete = { deleteTarget = recurrence },
                            )
                        }
                    }
                }
                ContentCard {
                    SectionHeader(strings["installments"])
                    Spacer(Modifier.height(8.dp))
                    if (state.installments.isEmpty()) {
                        EmptyNote(strings["noInstallments"])
                    } else {
                        state.installments.forEachIndexed { index, recurrence ->
                            if (index > 0) HorizontalDivider(Modifier.padding(vertical = 10.dp))
                            RecurrenceRow(
                                recurrence = recurrence,
                                categories = state.categories,
                                onEdit = {
                                    editing = recurrence
                                    showForm = true
                                },
                                onDelete = { deleteTarget = recurrence },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        RecurrenceFormDialog(
            recurrence = editing,
            categories = state.categories,
            saving = state.saving,
            onDismiss = { showForm = false },
            onSave = { input, status ->
                vm.save(editing?.id, input, status)
                showForm = false
            },
        )
    }

    deleteTarget?.let { recurrence ->
        ConfirmDialog(
            title = strings["confirmDeleteTitle"],
            text = strings["deleteRecurrenceConfirm"],
            confirmLabel = strings["delete"],
            dismissLabel = strings["cancel"],
            onConfirm = { vm.delete(recurrence.id) },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun RecurrenceRow(
    recurrence: Recurrence,
    categories: List<Category>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val strings = LocalStrings.current
    var menuOpen by remember { mutableStateOf(false) }
    val category = categories.firstOrNull { it.id == recurrence.categoryId }
    val categoryText = category?.let { categoryLabel(it, strings) }
        ?: categoryLabel(recurrence.category, recurrence.categoryKey, strings)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    if (recurrence.income) FinancesTheme.colors.success else FinancesTheme.colors.danger,
                    CircleShape,
                ),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                recurrence.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                listOfNotNull(
                    categoryText,
                    strings.format("recurrenceDayLabel", "day" to recurrence.day),
                    strings[if (recurrence.isInstallment) "installmentType" else "monthType"],
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (recurrence.isInstallment && recurrence.installments != null) {
                val total = recurrence.installments
                val payed = recurrence.payed ?: 0
                LinearProgressIndicator(
                    progress = { (payed.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    strings.format("installmentProgress", "current" to payed, "total" to total),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusTag(recurrence.status)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                (if (recurrence.income) "+" else "-") + Money.format(recurrence.value, strings.locale),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (recurrence.income) FinancesTheme.colors.success else FinancesTheme.colors.danger,
            )
            Box {
                IconButton(onClick = { menuOpen = true }) {
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
private fun RecurrenceFormDialog(
    recurrence: Recurrence?,
    categories: List<Category>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (input: RecurrenceInput, status: String) -> Unit,
) {
    val strings = LocalStrings.current

    var title by remember { mutableStateOf(recurrence?.title.orEmpty()) }
    var value by remember {
        mutableStateOf(recurrence?.value?.let { BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }.orEmpty())
    }
    var income by remember { mutableStateOf(recurrence?.income ?: false) }
    var categoryId by remember { mutableStateOf(recurrence?.categoryId.orEmpty()) }
    var type by remember { mutableStateOf(recurrence?.type ?: "MONTH") }
    var day by remember { mutableStateOf((recurrence?.day ?: 1).toString()) }
    var installments by remember { mutableStateOf(recurrence?.installments?.toString().orEmpty()) }
    var payed by remember { mutableStateOf((recurrence?.payed ?: 0).toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    val categoryOptions = categories.filter { it.income == income }
        .map { DropdownOption(it.id, categoryLabel(it, strings)) }

    FormDialog(
        title = strings[if (recurrence == null) "newRecurrence" else "editRecurrence"],
        onDismiss = onDismiss,
        confirmLabel = strings["save"],
        loading = saving,
        onConfirm = {
            val amount = Money.parse(value)
            val dayValue = day.trim().toIntOrNull()
            val total = installments.trim().toIntOrNull()
            val payedValue = payed.trim().toIntOrNull() ?: 0
            error = when {
                title.isBlank() -> "titleRequired"
                amount == null || amount <= 0.0 -> "valueInvalid"
                dayValue == null || dayValue !in 1..31 -> "dayInvalid"
                categoryId.isBlank() -> "categoryRequired"
                type == "INSTALLMENT" && (total == null || total < 1) -> "installmentsInvalid"
                type == "INSTALLMENT" && (payedValue < 0 || payedValue > total!!) -> "payedInvalid"
                else -> null
            }
            if (error == null) {
                onSave(
                    RecurrenceInput(
                        title = title,
                        value = amount!!,
                        income = income,
                        categoryId = categoryId,
                        type = type,
                        day = dayValue!!,
                        installments = if (type == "INSTALLMENT") total else null,
                        payed = if (type == "INSTALLMENT") payedValue else null,
                    ),
                    recurrence?.status ?: "ACTIVE",
                )
            }
        },
    ) {
        AppTextField(
            value = title,
            onValueChange = { title = it },
            label = strings["recurrenceTitleLabel"],
            placeholder = strings["recurrenceTitlePlaceholder"],
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTextField(
                value = day,
                onValueChange = { day = it.filter(Char::isDigit).take(2) },
                label = strings["dayLabel"],
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            AppTextField(
                value = value,
                onValueChange = { value = it },
                label = strings["valueLabel"],
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
        }
        if (recurrence == null) {
            AppDropdown(
                label = strings["recurrenceTypeLabel"],
                options = listOf(
                    DropdownOption("MONTH", strings["monthType"]),
                    DropdownOption("INSTALLMENT", strings["installmentType"]),
                ),
                selectedValue = type,
                onSelect = { type = it },
            )
        }
        SwitchRow(
            title = strings["typeLabel"],
            subtitle = strings[if (income) "income" else "expense"],
            checked = income,
            onCheckedChange = { checked ->
                income = checked
                val selected = categories.firstOrNull { it.id == categoryId }
                if (selected != null && selected.income != checked) categoryId = ""
            },
        )
        AppDropdown(
            label = strings["categoryLabel"],
            options = categoryOptions,
            selectedValue = categoryId,
            onSelect = { categoryId = it },
            placeholder = strings["selectCategory"],
        )
        if (recurrence != null) {
            Text(
                strings["recurrenceCategorySyncHint"],
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (type == "INSTALLMENT") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(
                    value = installments,
                    onValueChange = { installments = it.filter(Char::isDigit).take(3) },
                    label = strings["installmentsLabel"],
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                AppTextField(
                    value = payed,
                    onValueChange = { payed = it.filter(Char::isDigit).take(3) },
                    label = strings["payedLabel"],
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        FormError(error?.let { strings[it] })
    }
}
