package com.ehs.finances.core.ui

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

@Composable
fun StatusTag(status: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    val strings = LocalStrings.current
    val colors = FinancesTheme.colors
    val palette = remember(status, colors) {
        when (status.uppercase()) {
            "OK" -> colors.success to colors.successSoft
            "PENDING", "REVIEW", "ACTIVE" -> colors.warning to colors.warningSoft
            "DELAY", "OVERDUE" -> colors.danger to colors.dangerSoft
            "PAYED" -> colors.success to colors.successSoft
            else -> colors.danger to colors.subtle
        }
    }
    Tag(
        text = statusLabel(status, strings),
        modifier = modifier,
        color = palette.first,
        background = palette.second,
    )
}
