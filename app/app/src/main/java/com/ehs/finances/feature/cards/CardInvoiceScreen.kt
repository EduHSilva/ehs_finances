package com.ehs.finances.feature.cards

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
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
import com.ehs.finances.core.ui.InvoiceStatusTag
import com.ehs.finances.core.ui.MessageEffect
import com.ehs.finances.core.ui.components.AppDateField
import com.ehs.finances.core.ui.components.AppTextField
import com.ehs.finances.core.ui.components.ConfirmDialog
import com.ehs.finances.core.ui.components.ContentCard
import com.ehs.finances.core.ui.components.EmptyNote
import com.ehs.finances.core.ui.components.ErrorBanner
import com.ehs.finances.core.ui.components.FormDialog
import com.ehs.finances.core.ui.components.FormError
import com.ehs.finances.core.ui.components.LoadingScreen
import com.ehs.finances.core.ui.components.RefreshableColumn
import com.ehs.finances.core.ui.components.ScreenTitle
import com.ehs.finances.core.ui.components.SectionHeader
import com.ehs.finances.core.ui.containerViewModel
import com.ehs.finances.core.ui.theme.FinancesTheme
import com.ehs.finances.data.model.CreditCardInvoice
import com.ehs.finances.data.model.CreditCardInvoicePayment
import com.ehs.finances.domain.ApiDates
import com.ehs.finances.domain.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Composable
fun CardInvoiceScreen(
    cardId: String,
    onBack: () -> Unit,
) {
    val vm: CardInvoiceViewModel = containerViewModel {
        CardInvoiceViewModel(cardId, it.financesRepository)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    MessageEffect(message, onConsumed = vm::consumeMessage)

    var payTarget by remember { mutableStateOf<CreditCardInvoice?>(null) }
    var deletePayment by remember { mutableStateOf<Pair<String, CreditCardInvoicePayment>?>(null) }

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
            title = state.card?.name ?: strings["cardInvoices"],
            subtitle = strings["cardInvoicesSubtitle"],
            action = {
                OutlinedButton(onClick = onBack, shape = RoundedCornerShape(12.dp)) {
                    Text(strings["goBack"])
                }
            },
        )

        state.card?.let { card ->
            ContentCard {
                Text(
                    "${strings["cardLimit"]}: " +
                        (card.cardLimit?.let { Money.format(it, strings.locale) } ?: strings["cardNoLimit"]),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    listOf(
                        strings.format("cardClosingShort", "day" to card.closingDay),
                        strings.format("cardDueShort", "day" to card.dueDay),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(strings["cardOpenInvoices"], style = MaterialTheme.typography.bodySmall)
                    Text(
                        Money.format(card.openInvoiceTotal, strings.locale),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        when {
            state.loading && state.invoices.isEmpty() -> LoadingScreen(strings["loading"])
            state.error != null && state.invoices.isEmpty() -> ErrorBanner(
                message = state.error.orEmpty(),
                onRetry = vm::refresh,
                retryLabel = strings["retry"],
            )
            state.sortedInvoices.isEmpty() -> ContentCard {
                SectionHeader(strings["cardInvoices"])
                Spacer(Modifier.height(8.dp))
                EmptyNote(strings["noCardInvoices"])
            }
            else -> {
                state.error?.let { error ->
                    ErrorBanner(message = error, onRetry = vm::refresh, retryLabel = strings["retry"])
                }
                state.sortedInvoices.forEach { invoice ->
                    InvoiceCard(
                        invoice = invoice,
                        saving = state.saving,
                        deletingPaymentId = state.deletingPaymentId,
                        onPay = { payTarget = invoice },
                        onDeletePayment = { payment -> deletePayment = invoice.invoiceMonth to payment },
                    )
                }
            }
        }
    }

    payTarget?.let { invoice ->
        PayInvoiceDialog(
            invoice = invoice,
            saving = state.saving,
            onDismiss = { payTarget = null },
            onConfirm = { value, date ->
                vm.pay(value, date, invoice.invoiceMonth)
                payTarget = null
            },
        )
    }

    deletePayment?.let { (invoiceMonth, payment) ->
        ConfirmDialog(
            title = strings["deleteInvoicePaymentTitle"],
            text = strings["deleteInvoicePaymentConfirm"],
            confirmLabel = strings["delete"],
            dismissLabel = strings["cancel"],
            onConfirm = { vm.deletePayment(invoiceMonth, payment.id) },
            onDismiss = { deletePayment = null },
        )
    }
}

@Composable
private fun InvoiceCard(
    invoice: CreditCardInvoice,
    saving: Boolean,
    deletingPaymentId: String?,
    onPay: () -> Unit,
    onDeletePayment: (CreditCardInvoicePayment) -> Unit,
) {
    val strings = LocalStrings.current
    val month = invoiceMonthLabel(invoice.invoiceMonth, strings.locale)

    ContentCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    month ?: strings.format("invoiceOf", "month" to invoice.invoiceMonth),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    listOfNotNull(
                        invoice.closingDate?.let {
                            strings.format(
                                "invoiceClosingDate",
                                "date" to ApiDates.format(ApiDates.parse(it), strings.locale),
                            )
                        },
                        invoice.dueDate?.let {
                            strings.format(
                                "invoiceDueDate",
                                "date" to ApiDates.format(ApiDates.parse(it), strings.locale),
                            )
                        },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            InvoiceStatusTag(invoice.status)
        }

        Spacer(Modifier.height(12.dp))
        InvoiceAmountRow(strings["invoiceTotal"], Money.format(invoice.total, strings.locale))
        InvoiceAmountRow(strings["invoicePaid"], Money.format(invoice.paid, strings.locale))
        InvoiceAmountRow(
            label = strings["invoiceRemaining"],
            value = Money.format(invoice.remaining, strings.locale),
            highlight = invoice.remaining > 0.0,
        )

        if (invoice.remaining > 0.0) {
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(FinancesTheme.colors.border, RoundedCornerShape(3.dp)),
            ) {
                val ratio = if (invoice.total > 0.0) invoice.paid / invoice.total else 0.0
                Box(
                    Modifier
                        .fillMaxWidth(ratio.coerceIn(0.0, 1.0).toFloat())
                        .height(5.dp)
                        .background(FinancesTheme.colors.success, RoundedCornerShape(3.dp)),
                )
            }
        }

        if (invoice.payments.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = FinancesTheme.colors.border)
            Spacer(Modifier.height(8.dp))
            Text(
                strings["invoicePayments"],
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            invoice.payments.forEach { payment ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        listOf(
                            Money.format(payment.value, strings.locale),
                            ApiDates.format(ApiDates.parse(payment.date), strings.locale),
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    if (deletingPaymentId == payment.id) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(
                            onClick = { onDeletePayment(payment) },
                            enabled = deletingPaymentId == null && !saving,
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = strings["deleteInvoicePayment"],
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }

        if (invoice.canPay) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onPay,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = !saving,
            ) {
                Text(strings["payInvoice"])
            }
        }
    }
}

@Composable
private fun InvoiceAmountRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (highlight) FinancesTheme.colors.danger else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PayInvoiceDialog(
    invoice: CreditCardInvoice,
    saving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (value: Double, date: LocalDate) -> Unit,
) {
    val strings = LocalStrings.current
    var value by remember {
        mutableStateOf(BigDecimal.valueOf(invoice.remaining).stripTrailingZeros().toPlainString())
    }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var error by remember { mutableStateOf<String?>(null) }

    FormDialog(
        title = strings["payInvoiceTitle"],
        onDismiss = onDismiss,
        confirmLabel = strings["payInvoice"],
        loading = saving,
        onConfirm = {
            val amount = Money.parse(value)
            error = when {
                amount == null || amount <= 0.0 -> "invoicePayInvalid"
                amount > invoice.remaining + 0.005 -> "invoicePayInvalid"
                else -> null
            }
            if (error == null) onConfirm(amount!!, date)
        },
    ) {
        Text(
            listOfNotNull(
                invoiceMonthLabel(invoice.invoiceMonth, strings.locale),
                "${strings["invoiceRemaining"]}: ${Money.format(invoice.remaining, strings.locale)}",
            ).joinToString(" · "),
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
        AppDateField(
            label = strings["payInvoiceDate"],
            date = date,
            onChange = { date = it },
        )
        Text(
            strings.format("payInvoiceHelp", "remaining" to Money.format(invoice.remaining, strings.locale)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FormError(error?.let { strings[it] })
    }
}

/** Formata `AAAA-MM` como mês por extenso; mantém o valor cru se vier fora do padrão. */
private fun invoiceMonthLabel(invoiceMonth: String, locale: java.util.Locale): String? =
    try {
        ApiDates.formatMonth(YearMonth.parse(invoiceMonth, DateTimeFormatter.ofPattern("yyyy-MM")), locale)
    } catch (e: DateTimeParseException) {
        null
    }
