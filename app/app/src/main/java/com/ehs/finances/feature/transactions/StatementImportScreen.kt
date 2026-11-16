package com.ehs.finances.feature.transactions

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehs.finances.core.i18n.LocalStrings
import com.ehs.finances.core.ui.MessageEffect
import com.ehs.finances.core.ui.StatusTag
import com.ehs.finances.core.ui.categoryLabel
import com.ehs.finances.core.ui.components.AppDropdown
import com.ehs.finances.core.ui.components.ConfirmDialog
import com.ehs.finances.core.ui.components.ContentCard
import com.ehs.finances.core.ui.components.DropdownOption
import com.ehs.finances.core.ui.components.EmptyNote
import com.ehs.finances.core.ui.components.PrimaryButton
import com.ehs.finances.core.ui.components.SecondaryButton
import com.ehs.finances.core.ui.components.SectionHeader
import com.ehs.finances.core.ui.components.Tag
import com.ehs.finances.core.ui.components.dashedRoundedBorder
import com.ehs.finances.core.ui.containerViewModel
import com.ehs.finances.core.ui.theme.FinancesTheme
import com.ehs.finances.data.model.Transaction
import com.ehs.finances.domain.ApiDates
import com.ehs.finances.domain.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun StatementImportScreen(
    reviewOnly: Boolean,
    onBack: () -> Unit,
) {
    val vm: StatementImportViewModel = containerViewModel {
        StatementImportViewModel(it.financeState, it.financesRepository)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(reviewOnly) { vm.start(reviewOnly) }
    MessageEffect(message, onConsumed = vm::consumeMessage)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val name = queryFileName(context, uri)
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { stream -> stream.readBytes() }
                    }.getOrNull()
                }
                vm.onFilePicked(name, bytes)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = strings["goBack"])
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        strings[if (state.reviewOnly) "reviewImports" else "importStatement"],
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        strings[if (state.reviewOnly) "reviewImportsDesc" else "importStatementDesc"],
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (state.accountId == null) {
            item {
                ContentCard {
                    Text(strings["importAccountRequired"], style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        strings["createAccountFirst"],
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(strings["manageAccounts"], onClick = onBack)
                }
            }
            return@LazyColumn
        }

        when (state.step) {
            ImportStep.UPLOAD -> {
                item {
                    ContentCard {
                        Text(strings["importFileHelpTitle"], style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            strings["importFileHelp"],
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .dashedRoundedBorder(FinancesTheme.colors.borderStrong)
                            .clickable { launcher.launch(arrayOf("*/*")) }
                            .padding(vertical = 32.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.UploadFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp),
                        )
                        Text(strings["importChooseFile"], style = MaterialTheme.typography.titleMedium)
                        Text(
                            strings["importFormats"],
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item {
                    Text(
                        strings["importPdfHint"],
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            ImportStep.PREVIEW -> {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Tag(state.format.name, color = MaterialTheme.colorScheme.onSurface, background = FinancesTheme.colors.subtle)
                        Tag(
                            strings.format("importImportableCount", "count" to state.importable),
                            color = FinancesTheme.colors.success,
                            background = FinancesTheme.colors.successSoft,
                        )
                        Tag(
                            strings.format("importDuplicateCount", "count" to state.duplicates),
                            color = FinancesTheme.colors.warning,
                            background = FinancesTheme.colors.warningSoft,
                        )
                        if (state.invalidCount > 0) {
                            Tag(
                                strings.format("importInvalidCount", "count" to state.invalidCount),
                                color = FinancesTheme.colors.danger,
                                background = FinancesTheme.colors.dangerSoft,
                            )
                        }
                    }
                }
                item {
                    Text(
                        state.fileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (state.invalidCount > 0) {
                    item {
                        Text(
                            strings.format("importInvalidHelp", "count" to state.invalidCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = FinancesTheme.colors.warning,
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton(
                            text = strings["importChooseAnotherFile"],
                            onClick = vm::reset,
                            modifier = Modifier.weight(1f),
                        )
                        PrimaryButton(
                            text = strings.format("importConfirmCount", "count" to state.includedCount),
                            onClick = vm::confirmImport,
                            modifier = Modifier.weight(1f),
                            enabled = state.canConfirm,
                            loading = state.confirming || state.loadingPreview,
                        )
                    }
                }
                items(state.rows, key = { it.row }) { row ->
                    ImportRowCard(
                        row = row,
                        state = state,
                        onToggle = { vm.toggleInclude(row.row) },
                        onIncome = { income -> vm.setRowIncome(row.row, income) },
                        onCategory = { id -> vm.setRowCategory(row.row, id) },
                    )
                }
            }

            ImportStep.RESULT -> {
                state.result?.let { result ->
                    item {
                        ContentCard {
                            Text(strings["importDoneTitle"], style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                strings.format(
                                    "importDoneDescription",
                                    "imported" to result.imported,
                                    "duplicates" to result.duplicates,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item {
                    SectionHeader(
                        title = strings["importReviewTitle"],
                        subtitle = strings["importReviewHelp"],
                        action = {
                            Tag(
                                state.reviewing.size.toString(),
                                color = FinancesTheme.colors.warning,
                                background = FinancesTheme.colors.warningSoft,
                            )
                        },
                    )
                }
                if (state.reviewing.isEmpty()) {
                    item { EmptyNote(strings["importReviewEmpty"]) }
                } else {
                    items(state.reviewing, key = { it.id }) { transaction ->
                        ReviewRow(
                            transaction = transaction,
                            categories = state.categories,
                            selectedCategory = state.reviewCategories[transaction.id].orEmpty(),
                            loading = state.reviewingId == transaction.id,
                            onCategory = { vm.setReviewCategory(transaction.id, it.ifBlank { null }) },
                            onApprove = { vm.approve(transaction.id) },
                            onDiscard = { vm.requestDiscard(transaction) },
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.result != null) {
                            SecondaryButton(
                                text = strings["importAnother"],
                                onClick = vm::reset,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        PrimaryButton(
                            text = strings["close"],
                            onClick = onBack,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }

    val discard = state.discardTarget
    if (discard != null) {
        ConfirmDialog(
            title = strings["importDiscardTitle"],
            text = strings.format("importDiscardDesc", "title" to discard.title),
            confirmLabel = strings["importDiscard"],
            dismissLabel = strings["cancel"],
            onConfirm = vm::confirmDiscard,
            onDismiss = vm::dismissDiscard,
        )
    }
}

@Composable
private fun ImportRowCard(
    row: ImportRowState,
    state: ImportUiState,
    onToggle: () -> Unit,
    onIncome: (Boolean) -> Unit,
    onCategory: (String?) -> Unit,
) {
    val strings = LocalStrings.current
    ContentCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Checkbox(
                checked = row.include,
                onCheckedChange = { onToggle() },
                enabled = row.isValid && !row.duplicate,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    row.description.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        row.date?.let { ApiDates.format(ApiDates.parse(it), strings.locale) },
                        row.amount?.let { Money.format(it, strings.locale) },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                !row.isValid -> Tag(
                    strings["importLineInvalid"],
                    color = FinancesTheme.colors.danger,
                    background = FinancesTheme.colors.dangerSoft,
                )
                row.duplicate -> Tag(
                    strings["importLineDuplicate"],
                    color = FinancesTheme.colors.warning,
                    background = FinancesTheme.colors.warningSoft,
                )
                else -> Tag(
                    strings["importLineValid"],
                    color = FinancesTheme.colors.success,
                    background = FinancesTheme.colors.successSoft,
                )
            }
        }

        if (row.isValid) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppDropdown(
                    label = strings["typeLabel"],
                    options = listOf(
                        DropdownOption("income", strings["income"]),
                        DropdownOption("expense", strings["expense"]),
                    ),
                    selectedValue = if (row.income) "income" else "expense",
                    onSelect = { onIncome(it == "income") },
                    modifier = Modifier.weight(1f),
                )
                AppDropdown(
                    label = strings["categoryLabel"],
                    options = listOf(DropdownOption("", strings["noCategory"])) +
                        state.categories.filter { it.income == row.income }
                            .map { DropdownOption(it.id, categoryLabel(it, strings)) },
                    selectedValue = row.categoryId.orEmpty(),
                    onSelect = { onCategory(it.ifBlank { null }) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ReviewRow(
    transaction: Transaction,
    categories: List<com.ehs.finances.data.model.Category>,
    selectedCategory: String,
    loading: Boolean,
    onCategory: (String) -> Unit,
    onApprove: () -> Unit,
    onDiscard: () -> Unit,
) {
    val strings = LocalStrings.current
    val options = listOf(DropdownOption("", strings["noCategory"])) +
        categories.filter { it.income == transaction.income }
            .map { DropdownOption(it.id, categoryLabel(it, strings)) }
    ContentCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(transaction.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                Text(
                    listOfNotNull(
                        ApiDates.format(ApiDates.parse(transaction.executionDate), strings.locale),
                        Money.format(transaction.value, strings.locale),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusTag(transaction.status)
            if (loading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onApprove) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = strings["importApprove"],
                        tint = FinancesTheme.colors.success,
                    )
                }
                IconButton(onClick = onDiscard) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = strings["importDiscard"],
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        AppDropdown(
            label = strings["categoryLabel"],
            options = options,
            selectedValue = selectedCategory,
            onSelect = onCategory,
        )
    }
}

private fun queryFileName(context: Context, uri: Uri): String = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
}.getOrNull() ?: uri.lastPathSegment ?: "extrato"
