package com.ehs.finances.feature.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehs.finances.core.i18n.LocalStrings
import com.ehs.finances.core.i18n.Strings
import com.ehs.finances.core.ui.MessageEffect
import com.ehs.finances.core.ui.components.AppTextField
import com.ehs.finances.core.ui.components.ContentCard
import com.ehs.finances.core.ui.components.EmptyNote
import com.ehs.finances.core.ui.components.ErrorBanner
import com.ehs.finances.core.ui.components.FormDialog
import com.ehs.finances.core.ui.components.FormError
import com.ehs.finances.core.ui.components.LinkButton
import com.ehs.finances.core.ui.components.LoadingScreen
import com.ehs.finances.core.ui.components.RefreshableColumn
import com.ehs.finances.core.ui.components.ScreenTitle
import com.ehs.finances.core.ui.components.SectionHeader
import com.ehs.finances.core.ui.components.SegmentedToggle
import com.ehs.finances.core.ui.containerViewModel
import com.ehs.finances.core.ui.theme.FinancesTheme
import com.ehs.finances.data.model.CategoryShare
import com.ehs.finances.data.model.PendingItem
import com.ehs.finances.data.model.ProjectionItem
import com.ehs.finances.data.model.ProjectionPoint
import com.ehs.finances.data.model.Recurrence
import com.ehs.finances.domain.ApiDates
import com.ehs.finances.domain.Money
import java.math.BigDecimal
import java.time.YearMonth
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    onOpenProfile: () -> Unit,
) {
    val vm: DashboardViewModel = containerViewModel {
        DashboardViewModel(it.financeState, it.financesRepository)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val container = com.ehs.finances.di.LocalContainer.current
    val account by container.financeState.activeAccount.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    MessageEffect(message, onConsumed = vm::consumeMessage)

    var confirmTarget by remember { mutableStateOf<PendingItem?>(null) }

    RefreshableColumn(
        isRefreshing = state.refreshing,
        onRefresh = vm::refresh,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle(
            title = strings["dashboard"],
            subtitle = strings["projectionTitle"],
            action = {
                SegmentedToggle(
                    options = DashboardViewModel.HORIZONS.map { "$it d" },
                    selectedIndex = DashboardViewModel.HORIZONS.indexOf(state.days).coerceAtLeast(0),
                    onSelect = { index -> vm.setDays(DashboardViewModel.HORIZONS[index]) },
                    modifier = Modifier.width(190.dp),
                )
            },
        )

        when {
            account == null -> ContentCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        Icons.Filled.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(strings["noAccounts"], style = MaterialTheme.typography.titleMedium)
                        Text(
                            strings["createAccountFirst"],
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                LinkButton(strings["manageAccounts"], onClick = onOpenProfile)
            }

            state.loading && state.projection == null -> LoadingScreen(strings["dashboardLoading"])

            state.error != null && state.projection == null -> ErrorBanner(
                message = state.error.orEmpty(),
                onRetry = vm::refresh,
                retryLabel = strings["retry"],
            )

            state.projection != null -> {
                val projection = state.projection!!
                val summary = state.summary
                val cash = summary?.cash
                val investments = summary?.investments

                ContentCard {
                    Text(
                        strings["netWorth"],
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        Money.format(summary?.netWorth?.current ?: projection.currentBalance, strings.locale),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        strings["netWorthHelp"],
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = FinancesTheme.colors.border)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            strings.format("netWorthProjected", "days" to state.days),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            Money.format(summary?.netWorth?.projected ?: projection.closingBalance, strings.locale),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                val monthNet = cash?.monthNet ?: 0.0
                SummaryMetricCard(
                    title = strings["cash"],
                    primary = Money.format(cash?.currentBalance ?: projection.currentBalance, strings.locale),
                    rows = listOf(
                        MetricRow(
                            label = strings["monthResult"],
                            value = signedMoney(monthNet, strings.locale),
                            caption = "${strings["monthIncome"]} ${Money.format(cash?.monthIncome ?: 0.0, strings.locale)} · " +
                                "${strings["monthExpense"]} ${Money.format(cash?.monthExpenses ?: 0.0, strings.locale)}",
                            color = amountColor(monthNet),
                        ),
                        MetricRow(
                            label = strings["projectedClosingBalance"],
                            value = Money.format(cash?.projectedClosingBalance ?: projection.closingBalance, strings.locale),
                        ),
                    ),
                )

                val investmentMonthNet = investments?.monthNet ?: 0.0
                SummaryMetricCard(
                    title = strings["investments"],
                    primary = Money.format(investments?.total ?: 0.0, strings.locale),
                    rows = listOf(
                        MetricRow(
                            label = strings["monthNet"],
                            value = signedMoney(investmentMonthNet, strings.locale),
                            caption = "${strings["contributions"]} ${Money.format(investments?.monthContributions ?: 0.0, strings.locale)} · " +
                                "${strings["withdrawals"]} ${Money.format(investments?.monthWithdrawals ?: 0.0, strings.locale)}",
                            color = amountColor(investmentMonthNet),
                        ),
                        MetricRow(
                            label = strings["projectedClosingBalance"],
                            value = Money.format(investments?.projectedClosingBalance ?: 0.0, strings.locale),
                            caption = strings["investmentProjectionHelp"],
                        ),
                    ),
                )

                val livingCost = summary?.livingCost
                ContentCard {
                    SectionHeader(
                        title = strings["livingCost"],
                        subtitle = strings.format("livingCostHelp", "months" to (livingCost?.months ?: 6)),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            Money.format(livingCost?.monthlyAverage ?: 0.0, strings.locale),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            strings["perMonth"],
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = FinancesTheme.colors.border)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                strings["reserveMonths"],
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            livingCost?.reserve?.title?.let { title ->
                                Text(
                                    title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Text(
                            reserveMonthsValue(livingCost?.reserve?.months, strings),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                ContentCard {
                    SectionHeader(
                        title = strings["topCategories"],
                        subtitle = strings["topCategoriesHelp"],
                    )
                    Text(
                        ApiDates.formatMonth(YearMonth.now(), strings.locale),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    val topCategories = summary?.topCategories.orEmpty()
                    if (topCategories.isEmpty()) {
                        EmptyNote(strings["noTopCategories"])
                    } else {
                        topCategories.forEachIndexed { index, category ->
                            if (index > 0) Spacer(Modifier.height(10.dp))
                            CategoryShareRow(category, strings)
                        }
                    }
                }

                ContentCard {
                    SectionHeader(
                        title = strings["projectedCashflow"],
                        subtitle = listOfNotNull(
                            projection.calculatedAt?.let {
                                strings.format(
                                    "projectionCalculatedAt",
                                    "date" to ApiDates.format(ApiDates.parse(it), strings.locale),
                                )
                            },
                            "${strings["projectionConfidence"]}: ${strings[confidenceKey(projection.confidence)]}",
                        ).joinToString(" · "),
                    )
                    Spacer(Modifier.height(12.dp))
                    ProjectionChart(projection.series)
                }

                ContentCard {
                    SectionHeader(strings["projectionAssumptions"])
                    Spacer(Modifier.height(8.dp))
                    if (projection.assumptions.isEmpty()) {
                        Text(
                            strings["noProjectionAssumptions"],
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        projection.assumptions.forEach { assumption ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("•", color = MaterialTheme.colorScheme.primary)
                                Text(
                                    assumption,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                ContentCard {
                    SectionHeader(
                        title = strings["pendingItems"],
                        subtitle = strings["pendingItemsHelp"],
                    )
                    Spacer(Modifier.height(8.dp))
                    val pendingItems = state.pending?.items.orEmpty()
                    if (pendingItems.isEmpty()) {
                        EmptyNote(strings["noPendingItems"])
                    } else {
                        pendingItems.forEachIndexed { index, item ->
                            if (index > 0) Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(item.title, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        listOfNotNull(
                                            ApiDates.format(ApiDates.parse(item.date), strings.locale),
                                            strings["pendingKind${item.kind.lowercase().replaceFirstChar { it.uppercase() }}"],
                                            strings["transactionStatus${item.status.lowercase().replaceFirstChar { it.uppercase() }}"],
                                        ).joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = (if (item.income) "+" else "-") + Money.format(item.amount, strings.locale),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (item.income) FinancesTheme.colors.success else FinancesTheme.colors.danger,
                                )
                                if (state.confirmingId == item.id) {
                                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    IconButton(onClick = { confirmTarget = item }) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = strings["confirm"],
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                ContentCard {
                    SectionHeader(strings["projectionItems"])
                    Spacer(Modifier.height(8.dp))
                    if (projection.items.isEmpty()) {
                        EmptyNote(strings["noProjectionItems"])
                    } else {
                        val labels = installmentLabels(projection.items, state.recurrences, strings)
                        projection.items.forEachIndexed { index, item ->
                            if (index > 0) Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(item.title, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        listOfNotNull(
                                            ApiDates.format(ApiDates.parse(item.date), strings.locale),
                                            projectionKindLabel(item.kind, strings),
                                            labels[index],
                                        ).joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = (if (item.income) "+" else "-") + Money.format(item.amount, strings.locale),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (item.income) FinancesTheme.colors.success else FinancesTheme.colors.danger,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    confirmTarget?.let { item ->
        ConfirmPendingDialog(
            item = item,
            strings = strings,
            onDismiss = { confirmTarget = null },
            onConfirm = { value ->
                vm.confirmPending(item, value)
                confirmTarget = null
            },
        )
    }
}

@Composable
private fun ConfirmPendingDialog(
    item: PendingItem,
    strings: Strings,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    val recurrence = !item.kind.equals("TRANSACTION", ignoreCase = true)
    var value by remember {
        mutableStateOf(BigDecimal.valueOf(item.amount).stripTrailingZeros().toPlainString())
    }
    var error by remember { mutableStateOf<String?>(null) }

    FormDialog(
        title = strings[if (recurrence) "confirmRecurrenceTitle" else "confirmTransactionTitle"],
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
            "${item.title} · ${ApiDates.format(ApiDates.parse(item.date), strings.locale)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        Text(
            strings[if (recurrence) "confirmRecurrenceHelp" else "confirmValueHelp"],
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FormError(error?.let { strings[it] })
    }
}

private data class MetricRow(
    val label: String,
    val value: String,
    val caption: String? = null,
    val color: Color? = null,
)

@Composable
private fun SummaryMetricCard(
    title: String,
    primary: String,
    rows: List<MetricRow>,
    modifier: Modifier = Modifier,
) {
    ContentCard(modifier = modifier) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            primary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = FinancesTheme.colors.border)
        rows.forEach { row ->
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(
                    row.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        row.value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = row.color ?: MaterialTheme.colorScheme.onSurface,
                    )
                    if (row.caption != null) {
                        Text(
                            row.caption,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryShareRow(category: CategoryShare, strings: com.ehs.finances.core.i18n.Strings) {
    val color = categoryColor(category.color)
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                category.title ?: strings["uncategorized"],
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                Money.format(category.amount, strings.locale),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "${category.share.roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(FinancesTheme.colors.border, RoundedCornerShape(2.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth((category.share.coerceIn(0.0, 100.0) / 100.0).toFloat())
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(2.dp)),
            )
        }
    }
}

private fun signedMoney(value: Double, locale: Locale): String {
    val sign = when {
        value > 0 -> "+"
        value < 0 -> "-"
        else -> ""
    }
    return sign + Money.format(kotlin.math.abs(value), locale)
}

@Composable
private fun amountColor(value: Double): Color = when {
    value > 0 -> FinancesTheme.colors.success
    value < 0 -> FinancesTheme.colors.danger
    else -> MaterialTheme.colorScheme.onSurface
}

private fun reserveMonthsValue(
    months: Double?,
    strings: com.ehs.finances.core.i18n.Strings,
): String {
    if (months == null) return "—"
    val value = String.format(strings.locale, "%.1f", months)
    return strings.format("reserveMonthsValue", "count" to value)
}

private fun categoryColor(hex: String?): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(Color(0xFF64748B))

@Composable
private fun ProjectionChart(series: List<ProjectionPoint>) {
    val strings = LocalStrings.current
    if (series.isEmpty()) {
        EmptyNote(strings["noProjectionData"])
        return
    }

    val min = minOf(0.0, series.minOfOrNull { it.balance } ?: 0.0)
    val max = maxOf(1.0, series.maxOfOrNull { it.balance } ?: 1.0)
    val range = (max - min).coerceAtLeast(0.0001)
    val positive = MaterialTheme.colorScheme.primary
    val negative = FinancesTheme.colors.danger

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
        ) {
            val bar = size.width / series.size
            series.forEachIndexed { index, point ->
                val height = ((point.balance - min) / range * size.height)
                    .toFloat()
                    .coerceAtLeast(3f)
                drawRoundRect(
                    color = if (point.balance >= 0) positive else negative,
                    topLeft = Offset(index * bar + 1f, size.height - height),
                    size = Size((bar - 2f).coerceAtLeast(1f), height),
                    cornerRadius = CornerRadius(3f, 3f),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                Money.format(min, strings.locale),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                Money.format(max, strings.locale),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                ApiDates.format(ApiDates.parse(series.first().date), strings.locale),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                ApiDates.format(ApiDates.parse(series.last().date), strings.locale),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (min < 0) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { ((0 - min) / range).toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = negative,
                trackColor = positive,
            )
        }
    }
}

private fun confidenceKey(confidence: String): String =
    "confidence${confidence.lowercase().replaceFirstChar { it.uppercase() }}"

private fun projectionKindLabel(kind: String, strings: com.ehs.finances.core.i18n.Strings): String {
    val key = "projectionKind${kind.lowercase().replaceFirstChar { it.uppercase() }}"
    val label = strings[key]
    return if (label == key) kind else label
}

private fun installmentLabels(
    items: List<ProjectionItem>,
    recurrences: List<Recurrence>,
    strings: com.ehs.finances.core.i18n.Strings,
): Map<Int, String> {
    val positions = mutableMapOf<String, Int>()
    val result = mutableMapOf<Int, String>()

    items.withIndex()
        .filter { it.value.kind.equals("INSTALLMENT", ignoreCase = true) }
        .sortedBy { it.value.date ?: "" }
        .forEach { indexed ->
            val item = indexed.value
            val recurrence = recurrences.firstOrNull { candidate ->
                if (item.recurrenceId != null) {
                    candidate.id == item.recurrenceId
                } else {
                    candidate.type.equals("INSTALLMENT", ignoreCase = true) && candidate.title == item.title
                }
            }
            val total = item.installments ?: recurrence?.installments ?: return@forEach
            val group = item.recurrenceId ?: recurrence?.id ?: item.title
            val position = (positions[group] ?: item.payed ?: recurrence?.payed ?: 0) + 1
            positions[group] = position
            val current = item.installmentNumber ?: position
            result[indexed.index] = strings.format(
                "installmentProgress",
                "current" to current,
                "total" to total,
            )
        }

    return result
}
