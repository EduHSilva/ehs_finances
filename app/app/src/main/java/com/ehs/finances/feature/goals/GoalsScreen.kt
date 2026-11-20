package com.ehs.finances.feature.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehs.finances.core.i18n.LocalStrings
import com.ehs.finances.core.ui.MessageEffect
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
import com.ehs.finances.core.ui.components.MonthSelector
import com.ehs.finances.core.ui.components.PrimaryButton
import com.ehs.finances.core.ui.components.RefreshableColumn
import com.ehs.finances.core.ui.components.ScreenTitle
import com.ehs.finances.core.ui.components.SectionHeader
import com.ehs.finances.core.ui.components.SwitchRow
import com.ehs.finances.core.ui.containerViewModel
import com.ehs.finances.core.ui.theme.FinancesTheme
import com.ehs.finances.data.model.MonthlyGoal
import com.ehs.finances.data.model.MonthlyGoalInput
import com.ehs.finances.domain.ApiDates
import com.ehs.finances.domain.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun GoalsScreen() {
    val vm: GoalsViewModel = containerViewModel {
        GoalsViewModel(it.financeState, it.financesRepository)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val container = com.ehs.finances.di.LocalContainer.current
    val account by container.financeState.activeAccount.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    MessageEffect(message, onConsumed = vm::consumeMessage)

    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MonthlyGoal?>(null) }
    var deleteTarget by remember { mutableStateOf<MonthlyGoal?>(null) }
    var contributionTarget by remember { mutableStateOf<MonthlyGoal?>(null) }

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
            title = strings["monthlyGoals"],
            subtitle = strings["goalsSubtitle"],
            action = {
                PrimaryButton(
                    text = strings["newGoal"],
                    onClick = {
                        editing = null
                        showForm = true
                    },
                    enabled = account != null,
                )
            },
        )

        MonthSelector(month = state.month, onPrevious = vm::previousMonth, onNext = vm::nextMonth)

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
            state.loading && state.goals.isEmpty() -> LoadingScreen(strings["loading"])
            state.error != null -> ErrorBanner(
                message = state.error.orEmpty(),
                onRetry = vm::refresh,
                retryLabel = strings["retry"],
            )
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(strings["expectedThisMonth"], Money.format(state.expectedTotal, strings.locale))
                    MetricCard(strings["realizedThisMonth"], Money.format(state.realizedTotal, strings.locale))
                }
                ContentCard {
                    Text(
                        strings["monthlyIncomeBase"],
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        Money.format(state.monthlyIncome, strings.locale),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    strings["goalRealizedHelp"],
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (state.goals.isEmpty()) {
                    ContentCard {
                        EmptyNote(strings["noGoalsYet"])
                        Spacer(Modifier.height(8.dp))
                        Text(
                            strings["createFirstGoal"],
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    state.goals.forEach { goal ->
                        GoalCard(
                            goal = goal,
                            state = state,
                            deleting = state.deletingId == goal.id,
                            onContribute = { contributionTarget = goal },
                            onEdit = {
                                editing = goal
                                showForm = true
                            },
                            onDelete = { deleteTarget = goal },
                        )
                    }
                }

                val report = state.categoryReport()
                ContentCard {
                    SectionHeader(
                        title = strings["categoryGoalReport"],
                        subtitle = strings["categoryGoalReportHelp"],
                    )
                    Spacer(Modifier.height(8.dp))
                    if (report.isEmpty()) {
                        EmptyNote(strings["noCategoryReport"])
                    } else {
                        categoryGoalGroups(report).forEachIndexed { index, (classification, rows) ->
                            if (index > 0) HorizontalDivider(Modifier.padding(vertical = 12.dp))
                            CategoryGroupHeader(
                                classification = classification,
                                realized = rows.sumOf { it.realized },
                                expected = rows.sumOf { it.expected },
                            )
                            rows.forEach { row ->
                                Spacer(Modifier.height(10.dp))
                                CategoryReportRow(row = row, income = state.monthlyIncome)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        GoalFormDialog(
            goal = editing,
            month = state.month,
            initialBalanceLocked = editing?.let { state.saved(it) > 0 } ?: false,
            saving = state.saving,
            onDismiss = { showForm = false },
            onSave = { input ->
                vm.save(editing?.id, input, editing?.active ?: true)
                showForm = false
            },
        )
    }

    contributionTarget?.let { goal ->
        ContributionDialog(
            goal = goal,
            month = state.month,
            saving = state.contributing,
            onDismiss = { contributionTarget = null },
            onConfirm = { amount, date, adjustment ->
                vm.contribute(goal, amount, date, adjustment)
                contributionTarget = null
            },
        )
    }

    deleteTarget?.let { goal ->
        ConfirmDialog(
            title = strings["confirmDeleteTitle"],
            text = strings["deleteGoalConfirm"],
            confirmLabel = strings["delete"],
            dismissLabel = strings["cancel"],
            onConfirm = { vm.delete(goal.id) },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun RowScope.MetricCard(title: String, value: String) {
    ContentCard(modifier = Modifier.weight(1f)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun GoalCard(
    goal: MonthlyGoal,
    state: GoalsUiState,
    deleting: Boolean,
    onContribute: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val strings = LocalStrings.current
    var menuOpen by remember { mutableStateOf(false) }
    val expected = state.expected(goal)
    val realized = state.realized(goal)
    val objective = state.objective(goal)
    val progress = if (expected > 0) (realized / expected * 100.0).coerceAtMost(100.0) else 0.0

    ContentCard {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (goal.mode.equals("PERCENT_INCOME", ignoreCase = true)) {
                        "${goal.amount}% ${strings["ofIncome"]}"
                    } else {
                        strings["fixedMonthlyAmount"]
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    goalPeriodLabel(goal, strings.locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (goal.reserve) {
                    Text(
                        strings["reserveBadge"],
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (state.initialBalance(goal) > 0) {
                    Text(
                        "${strings["initialBalance"]}: ${Money.format(state.initialBalance(goal), strings.locale)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (deleting) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
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

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    strings["realized"],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    Money.format(realized, strings.locale),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                "${strings["expected"]} ${Money.format(expected, strings.locale)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (progress / 100.0).toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${progress.roundToInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = onContribute) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text(strings["recordRealization"])
            }
        }

        if (objective != null) {
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    strings["objective"],
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${Money.format(state.saved(goal), strings.locale)} / ${Money.format(objective, strings.locale)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (state.objectiveProgress(goal) / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = FinancesTheme.colors.success,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                strings.format("objectiveProgress", "value" to state.objectiveProgress(goal).roundToInt()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val CATEGORY_GROUP_ORDER = listOf("ESSENTIAL", "INVESTMENT", "RECREATION")

private fun categoryGoalGroups(rows: List<CategoryGoalRow>): List<Pair<String, List<CategoryGoalRow>>> =
    CATEGORY_GROUP_ORDER.mapNotNull { classification ->
        val groupRows = rows.filter { row ->
            val key = row.classification.uppercase().takeIf { it in CATEGORY_GROUP_ORDER } ?: "ESSENTIAL"
            key == classification
        }
        if (groupRows.isEmpty()) null else classification to groupRows
    }

@Composable
private fun CategoryGroupHeader(classification: String, realized: Double, expected: Double) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            classificationLabel(classification, strings) ?: classification,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "${strings["categorySubtotal"]}: " +
                "${Money.format(realized, strings.locale)} / ${Money.format(expected, strings.locale)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CategoryReportRow(row: CategoryGoalRow, income: Double) {
    val strings = LocalStrings.current
    val color = runCatching { Color(android.graphics.Color.parseColor(row.color)) }
        .getOrDefault(MaterialTheme.colorScheme.primary)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(10.dp).background(color, CircleShape))
            Text(
                row.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                Money.format(row.realized, strings.locale),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            listOfNotNull(
                "${strings["expected"]} ${Money.format(row.expected, strings.locale)}",
                row.share?.let { strings.format("categoryShare", "value" to it.roundToInt()) },
                if (row.goal > 0) "${row.goal}%" else null,
                if (income <= 0 && row.expected <= 0) strings["noIncomeBase"] else null,
            ).joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (row.expected > 0) {
            LinearProgressIndicator(
                progress = { (row.progress / 100.0).toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = if (row.progress >= 100) FinancesTheme.colors.danger else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun GoalFormDialog(
    goal: MonthlyGoal?,
    month: YearMonth,
    initialBalanceLocked: Boolean,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (MonthlyGoalInput) -> Unit,
) {
    val strings = LocalStrings.current
    val defaultStart = month.atDay(1)
    val defaultEnd = month.atEndOfMonth()

    var title by remember { mutableStateOf(goal?.title.orEmpty()) }
    var mode by remember { mutableStateOf(goal?.mode ?: "FIXED") }
    var amount by remember {
        mutableStateOf(goal?.amount?.let { BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }.orEmpty())
    }
    var initialBalance by remember {
        mutableStateOf(
            (goal?.initialBalance ?: 0.0).let { BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() },
        )
    }
    var objective by remember {
        mutableStateOf(goal?.objective?.let { BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }.orEmpty())
    }
    var start by remember {
        mutableStateOf(goal?.dtInicio?.let { ApiDates.parse(it) } ?: defaultStart)
    }
    var hasEnd by remember { mutableStateOf(goal?.dtFim != null || goal == null) }
    var end by remember {
        mutableStateOf(goal?.dtFim?.let { ApiDates.parse(it) } ?: defaultEnd)
    }
    var reserve by remember { mutableStateOf(goal?.reserve == true) }
    var error by remember { mutableStateOf<String?>(null) }

    FormDialog(
        title = strings[if (goal == null) "newGoal" else "editGoal"],
        onDismiss = onDismiss,
        confirmLabel = strings["save"],
        loading = saving,
        onConfirm = {
            val amountValue = Money.parse(amount)
            val objectiveValue = objective.takeIf { it.isNotBlank() }?.let { Money.parse(it) }
            error = when {
                title.isBlank() -> "titleRequired"
                amountValue == null || amountValue <= 0.0 -> "valueInvalid"
                mode == "PERCENT_INCOME" && amountValue > 100.0 -> "percentageMaximum"
                initialBalance.isNotBlank() && Money.parse(initialBalance) == null -> "valueInvalid"
                hasEnd && end.isBefore(start) -> "goalEndBeforeStart"
                objective.isNotBlank() && objectiveValue == null -> "valueInvalid"
                else -> null
            }
            if (error == null) {
                onSave(
                    MonthlyGoalInput(
                        title = title,
                        mode = mode,
                        amount = amountValue!!,
                        initialBalance = Money.parse(initialBalance) ?: 0.0,
                        objective = objectiveValue?.takeIf { it > 0 },
                        periodMonth = start.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        dtInicio = start.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        dtFim = if (hasEnd) end.format(DateTimeFormatter.ISO_LOCAL_DATE) else null,
                        reserve = reserve,
                    ),
                )
            }
        },
    ) {
        AppTextField(
            value = title,
            onValueChange = { title = it },
            label = strings["goalName"],
            placeholder = strings["goalNameExample"],
        )
        AppDropdown(
            label = strings["goalType"],
            options = listOf(
                DropdownOption("FIXED", strings["fixedMonthlyAmount"]),
                DropdownOption("PERCENT_INCOME", strings["percentOfIncome"]),
            ),
            selectedValue = mode,
            onSelect = { mode = it },
        )
        AppTextField(
            value = amount,
            onValueChange = { amount = it },
            label = if (mode == "FIXED") strings["monthlyAmount"] else strings["incomePercent"],
            keyboardType = KeyboardType.Decimal,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTextField(
                value = initialBalance,
                onValueChange = { initialBalance = it },
                label = strings["initialBalance"],
                keyboardType = KeyboardType.Decimal,
                enabled = !initialBalanceLocked,
                supportingText = if (initialBalanceLocked) strings["initialBalanceLocked"] else null,
                modifier = Modifier.weight(1f),
            )
            AppTextField(
                value = objective,
                onValueChange = { objective = it },
                label = strings["objective"],
                keyboardType = KeyboardType.Decimal,
                supportingText = strings["objectiveHelp"],
                modifier = Modifier.weight(1f),
            )
        }
        AppDateField(label = strings["startDate"], date = start, onChange = { start = it })
        SwitchRow(
            title = strings["endDate"],
            subtitle = if (hasEnd) null else strings["noEndDate"],
            checked = hasEnd,
            onCheckedChange = { hasEnd = it },
        )
        if (hasEnd) {
            AppDateField(label = strings["endDate"], date = end, onChange = { end = it })
        }
        SwitchRow(
            title = strings["reserveGoal"],
            subtitle = strings["reserveGoalHelp"],
            checked = reserve,
            onCheckedChange = { reserve = it },
        )
        FormError(error?.let { strings[it] })
    }
}

@Composable
private fun ContributionDialog(
    goal: MonthlyGoal,
    month: YearMonth,
    saving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, date: String, adjustment: Boolean) -> Unit,
) {
    val strings = LocalStrings.current
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(month.atDay(1)) }
    var adjustment by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    FormDialog(
        title = strings[if (adjustment) "recordGoalAdjustment" else "recordGoalRealization"],
        onDismiss = onDismiss,
        confirmLabel = strings[if (adjustment) "applyAdjustment" else "recordRealization"],
        loading = saving,
        onConfirm = {
            val value = Money.parse(amount)
            error = if (value == null || value <= 0.0) "valueInvalid" else null
            if (value != null && value > 0.0) {
                onConfirm(value, date.format(DateTimeFormatter.ISO_LOCAL_DATE), adjustment)
            }
        },
    ) {
        Text(goal.title, style = MaterialTheme.typography.titleSmall)
        AppTextField(
            value = amount,
            onValueChange = { amount = it },
            label = strings["amount"],
            keyboardType = KeyboardType.Decimal,
        )
        AppDateField(label = strings["dateLabel"], date = date, onChange = { date = it })
        SwitchRow(
            title = strings["manualAdjustment"],
            subtitle = strings["manualAdjustmentHelp"],
            checked = adjustment,
            onCheckedChange = { adjustment = it },
        )
        FormError(error?.let { strings[it] })
    }
}

private fun goalPeriodLabel(goal: MonthlyGoal, locale: java.util.Locale): String {
    val start = goal.dtInicio?.let { ApiDates.parse(it) }
    val end = goal.dtFim?.let { ApiDates.parse(it) }
    return when {
        start == null -> goal.periodMonth
        end == null -> ApiDates.format(start, locale)
        else -> "${ApiDates.format(start, locale)} – ${ApiDates.format(end, locale)}"
    }
}

private fun Double.roundToInt(): Int = toBigDecimal().setScale(0, RoundingMode.HALF_UP).toInt()
