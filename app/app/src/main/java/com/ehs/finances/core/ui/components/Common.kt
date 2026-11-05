package com.ehs.finances.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ehs.finances.core.ui.theme.FinancesTheme

@Composable
fun ContentCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, FinancesTheme.colors.border),
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        action?.invoke()
    }
}

@Composable
fun LinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Text(text, color = if (enabled) color else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    retryLabel: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(FinancesTheme.colors.dangerSoft, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = message,
            color = FinancesTheme.colors.danger,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        if (onRetry != null && retryLabel != null) {
            TextButton(onClick = onRetry) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = FinancesTheme.colors.danger,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(retryLabel, color = FinancesTheme.colors.danger, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

fun Modifier.dashedRoundedBorder(color: Color, radius: Dp = 8.dp, strokeWidth: Dp = 1.dp): Modifier =
    drawBehind {
        drawRoundRect(
            color = color,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius.toPx()),
            style = Stroke(
                width = strokeWidth.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
            ),
        )
    }

@Composable
fun EmptyNote(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .dashedRoundedBorder(FinancesTheme.colors.borderStrong)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun Tag(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = FinancesTheme.colors.danger,
    background: Color = FinancesTheme.colors.dangerSoft,
) {
    Box(
        modifier = modifier
            .background(background, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun InitialAvatar(name: String, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val initials = rememberInitials(name)
    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = MaterialTheme.colorScheme.onPrimary,
            style = if (size >= 56.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.labelLarge,
        )
    }
}

private fun rememberInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts.first().take(1).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

@Composable
fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .background(
                        if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                        RoundedCornerShape(8.dp),
                    )
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun LoadingScreen(text: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 64.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun Subtitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        },
    )
}
