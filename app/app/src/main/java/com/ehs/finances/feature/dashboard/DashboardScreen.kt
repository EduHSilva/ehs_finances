package com.ehs.finances.feature.dashboard

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehs.finances.core.i18n.LocalStrings
import com.ehs.finances.core.ui.MessageEffect
import com.ehs.finances.core.ui.components.ContentCard
import com.ehs.finances.core.ui.components.EmptyNote
import com.ehs.finances.core.ui.components.ErrorBanner
import com.ehs.finances.core.ui.components.LinkButton
import com.ehs.finances.core.ui.components.LoadingScreen
import com.ehs.finances.core.ui.components.ScreenTitle
import com.ehs.finances.core.ui.components.SectionHeader
import com.ehs.finances.core.ui.components.SegmentedToggle
import com.ehs.finances.core.ui.containerViewModel
import com.ehs.finances.core.ui.theme.FinancesTheme
import com.ehs.finances.data.model.ProjectionItem
import com.ehs.finances.data.model.ProjectionPoint
import com.ehs.finances.data.model.Recurrence
import com.ehs.finances.domain.ApiDates
import com.ehs.finances.domain.Money

@Composable
fun DashboardScreen(
    onOpenProfile: () -> Unit,
    onOpenGoals: () -> Unit,
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

    Column(
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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        title = strings["currentBalance"],
                        value = Money.format(projection.currentBalance, strings.locale),
                    )
                    MetricCard(
                        title = strings["projectedClosingBalance"],
                        value = Money.format(projection.closingBalance, strings.locale),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        title = strings["investmentBalance"],
                        value = Money.format(state.investments?.balance ?: 0.0, strings.locale),
                        caption = state.investments?.let {
                            "${strings["contributions"]} ${Money.format(it.totalContributions, strings.locale)} · " +
                                "${strings["withdrawals"]} ${Money.format(it.totalWithdrawals, strings.locale)}"
                        },
                        action = { LinkButton(strings["manageGoals"], onClick = onOpenGoals) },
                    )
                    MetricCard(
                        title = strings["projectionConfidence"],
                        value = strings[confidenceKey(projection.confidence)],
                    )
                }

                ContentCard {
                    SectionHeader(
                        title = strings["projectedCashflow"],
                        subtitle = projection.calculatedAt?.let {
                            strings.format(
                                "projectionCalculatedAt",
                                "date" to ApiDates.format(ApiDates.parse(it), strings.locale),
                            )
                        },
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
                                    IconButton(onClick = { vm.confirmPending(item) }) {
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
}

@Composable
private fun RowScope.MetricCard(
    title: String,
    value: String,
    caption: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    ContentCard(modifier = Modifier.weight(1f)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        if (caption != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        action?.invoke()
    }
}

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
