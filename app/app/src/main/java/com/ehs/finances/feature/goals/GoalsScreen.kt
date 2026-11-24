package com.ehs.finances.feature.goals

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehs.finances.core.i18n.LocalStrings
import com.ehs.finances.core.ui.MessageEffect
import com.ehs.finances.core.ui.classificationLabel
import com.ehs.finances.core.ui.components.ContentCard
import com.ehs.finances.core.ui.components.EmptyNote
import com.ehs.finances.core.ui.components.ErrorBanner
import com.ehs.finances.core.ui.components.LoadingScreen
import com.ehs.finances.core.ui.components.MonthSelector
import com.ehs.finances.core.ui.components.RefreshableColumn
import com.ehs.finances.core.ui.components.ScreenTitle
import com.ehs.finances.core.ui.components.SectionHeader
import com.ehs.finances.core.ui.containerViewModel
import com.ehs.finances.core.ui.theme.FinancesTheme
import com.ehs.finances.domain.Money
import java.math.BigDecimal
import java.math.RoundingMode

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
            title = strings["categoryGoalReport"],
            subtitle = strings["categoryGoalReportHelp"],
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
            state.loading && state.transactions.isEmpty() -> LoadingScreen(strings["loading"])
            state.error != null -> ErrorBanner(
                message = state.error.orEmpty(),
                onRetry = vm::refresh,
                retryLabel = strings["retry"],
            )
            else -> {
                ClassificationShareCard(state)

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
                            val realized = rows.sumOf { it.realized }
                            val expected = rows.sumOf { it.expected }
                            CategoryGroupHeader(
                                classification = classification,
                                goalPercent = rows.sumOf { it.goal },
                                share = state.monthlyIncome.takeIf { it > 0 }
                                    ?.let { realized / it * 100.0 },
                                realized = realized,
                                expected = expected,
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
}

@Composable
private fun ClassificationShareCard(state: GoalsUiState) {
    val strings = LocalStrings.current
    val report = state.categoryReport()
    val income = state.monthlyIncome

    ContentCard {
        SectionHeader(
            title = strings["classificationShareTitle"],
            subtitle = strings["classificationShareDesc"],
        )
        Spacer(Modifier.height(8.dp))
        if (report.isEmpty()) {
            EmptyNote(strings["noCategoryReport"])
        } else {
            CATEGORY_GROUP_ORDER.forEachIndexed { index, classification ->
                if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                val realized = report
                    .filter { normalizeClassification(it.classification) == classification }
                    .sumOf { it.realized }
                val share = if (income > 0) realized / income * 100.0 else null
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        classificationLabel(classification, strings) ?: classification,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        Money.format(realized, strings.locale),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        share?.let { "${it.roundToInt()}%" } ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (income <= 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    strings["classificationShareNoIncome"],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val CATEGORY_GROUP_ORDER = listOf("ESSENTIAL", "INVESTMENT", "RECREATION")

private fun normalizeClassification(classification: String): String =
    classification.uppercase().takeIf { it in CATEGORY_GROUP_ORDER } ?: "ESSENTIAL"

private fun categoryGoalGroups(rows: List<CategoryGoalRow>): List<Pair<String, List<CategoryGoalRow>>> =
    CATEGORY_GROUP_ORDER.mapNotNull { classification ->
        val groupRows = rows.filter { normalizeClassification(it.classification) == classification }
        if (groupRows.isEmpty()) null else classification to groupRows
    }

@Composable
private fun CategoryGroupHeader(
    classification: String,
    goalPercent: Int,
    share: Double?,
    realized: Double,
    expected: Double,
) {
    val strings = LocalStrings.current
    val summary = listOfNotNull(
        if (goalPercent > 0) "${strings["categoryGoalsGoal"]} $goalPercent%" else null,
        share?.let { strings.format("categoryGoalsRealizedPercent", "value" to it.roundToInt()) },
    ).joinToString(" · ").ifEmpty { strings["noGoalSet"] }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                summary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
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

private fun Double.roundToInt(): Int = toBigDecimal().setScale(0, RoundingMode.HALF_UP).toInt()
