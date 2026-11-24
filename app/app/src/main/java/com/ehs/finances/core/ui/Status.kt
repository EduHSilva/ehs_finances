package com.ehs.finances.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ehs.finances.core.i18n.LocalStrings
import com.ehs.finances.core.i18n.Strings
import com.ehs.finances.core.ui.components.Tag
import com.ehs.finances.core.ui.theme.FinancesTheme
import com.ehs.finances.data.model.Category

/** Traduz o `defaultKey` das categorias do backend quando existir. */
fun categoryLabel(category: Category, strings: Strings): String {
    val key = category.defaultKey
    if (!key.isNullOrBlank()) {
        val translated = strings[key]
        if (translated != key) return translated
    }
    return category.title.ifBlank { strings["noCategory"] }
}

fun categoryLabel(title: String?, defaultKey: String?, strings: Strings): String {
    if (!defaultKey.isNullOrBlank()) {
        val translated = strings[defaultKey]
        if (translated != defaultKey) return translated
    }
    return title?.takeIf { it.isNotBlank() } ?: strings["noCategory"]
}

fun classificationLabel(classification: String?, strings: Strings): String? {
    val value = classification?.takeIf { it.isNotBlank() } ?: return null
    val key = "classification${value.lowercase().replaceFirstChar { it.uppercase() }}"
    val translated = strings[key]
    return if (translated == key) value else translated
}

fun statusLabel(status: String, strings: Strings): String {
    val key = "transactionStatus${status.lowercase().replaceFirstChar { it.uppercase() }}"
    val translated = strings[key]
    return if (translated == key) status else translated
}

fun invoiceStatusLabel(status: String, strings: Strings): String {
    val key = "invoiceStatus${status.lowercase().replaceFirstChar { it.uppercase() }}"
    val translated = strings[key]
    return if (translated == key) status else translated
}

fun cardStatusLabel(status: String, strings: Strings): String {
    val key = "cardStatus${status.lowercase().replaceFirstChar { it.uppercase() }}"
    val translated = strings[key]
    return if (translated == key) status else translated
}

/** Etiqueta de status da fatura (`OPEN`, `PARTIAL`, `PAID`). */
@Composable
fun InvoiceStatusTag(status: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    val strings = LocalStrings.current
    val colors = FinancesTheme.colors
    val primary = MaterialTheme.colorScheme.primary
    val palette = remember(status, colors, primary) {
        when (status.uppercase()) {
            "PAID" -> colors.success to colors.successSoft
            "PARTIAL" -> colors.warning to colors.warningSoft
            else -> primary to colors.primarySoft
        }
    }
    Tag(
        text = invoiceStatusLabel(status, strings),
        modifier = modifier,
        color = palette.first,
        background = palette.second,
    )
}

/** Etiqueta curta das movimentações especiais (`CARD`, `TRANSFER`, `INVOICE_PAYMENT`). */
@Composable
fun EntryTypeTag(entryType: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    val strings = LocalStrings.current
    val colors = FinancesTheme.colors
    val primary = MaterialTheme.colorScheme.primary
    val key = when (entryType.uppercase()) {
        "CARD" -> "entryBadgeCard"
        "TRANSFER" -> "entryBadgeTransfer"
        "INVOICE_PAYMENT" -> "entryBadgeInvoicePayment"
        else -> return
    }
    // Tipo de recurso = neutro por padrão; só o cartão (accent do produto) ganha cor.
    val isCard = entryType.uppercase() == "CARD"
    val color = if (isCard) primary else colors.neutralInk
    val background = if (isCard) colors.primarySoft else colors.neutralInkSoft
    Tag(text = strings[key], modifier = modifier, color = color, background = background)
}

@Composable
fun StatusTag(status: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    val strings = LocalStrings.current
    val colors = FinancesTheme.colors
    val primary = MaterialTheme.colorScheme.primary
    val palette = remember(status, colors) {
        when (status.uppercase()) {
            "OK", "PAYED" -> colors.success to colors.successSoft
            "PENDING", "REVIEW" -> colors.warning to colors.warningSoft
            "DELAY", "OVERDUE", "FAILED", "ERROR" -> colors.danger to colors.dangerSoft
            "ACTIVE" -> primary to colors.primarySoft
            else -> colors.neutralInk to colors.neutralInkSoft
        }
    }
    Tag(
        text = statusLabel(status, strings),
        modifier = modifier,
        color = palette.first,
        background = palette.second,
    )
}
