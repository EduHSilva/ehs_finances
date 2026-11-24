package com.ehs.finances.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Cores fora do esquema Material: sucesso/alerta/erro "soft", sidebar escura, tags neutras e
 * textos sutis. Espelham os tokens do design system EHS (base comum + accent esmeralda).
 */
@Immutable
data class FinancesColors(
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val sidebar: Color,
    val sidebarTop: Color,
    val sidebarText: Color,
    val sidebarAccent: Color,
    val sidebarOnAccent: Color,
    val subtle: Color,
    val primarySoft: Color,
    val primaryStrong: Color,
    val onPrimarySoft: Color,
    val border: Color,
    val borderStrong: Color,
    val neutralInk: Color,
    val neutralInkSoft: Color,
)

private val LightExtended = FinancesColors(
    success = LightSuccess,
    successSoft = LightSuccessSoft,
    warning = LightWarning,
    warningSoft = LightWarningSoft,
    danger = LightDanger,
    dangerSoft = LightDangerSoft,
    sidebar = LightSidebar,
    sidebarTop = LightSidebarTop,
    sidebarText = LightSidebarText,
    sidebarAccent = LightSidebarAccent,
    sidebarOnAccent = DarkOnPrimary,
    subtle = LightSubtle,
    primarySoft = LightPrimarySoft,
    primaryStrong = LightPrimaryStrong,
    onPrimarySoft = LightPrimaryStrong,
    border = LightBorder,
    borderStrong = LightBorderStrong,
    neutralInk = LightNeutralInk,
    neutralInkSoft = LightNeutralInkSoft,
)

private val DarkExtended = FinancesColors(
    success = DarkSuccess,
    successSoft = DarkSuccessSoft,
    warning = DarkWarning,
    warningSoft = DarkWarningSoft,
    danger = DarkDanger,
    dangerSoft = DarkDangerSoft,
    sidebar = DarkSidebar,
    sidebarTop = DarkSidebarTop,
    sidebarText = DarkSidebarText,
    sidebarAccent = DarkSidebarAccent,
    sidebarOnAccent = DarkOnPrimary,
    subtle = DarkSubtle,
    primarySoft = DarkPrimarySoft,
    primaryStrong = DarkPrimaryStrong,
    onPrimarySoft = DarkPrimaryStrong,
    border = DarkBorder,
    borderStrong = DarkBorderStrong,
    neutralInk = DarkNeutralInk,
    neutralInkSoft = DarkNeutralInkSoft,
)

private val LightScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimarySoft,
    onPrimaryContainer = LightPrimaryStrong,
    secondary = LightSuccess,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = LightSuccessSoft,
    onSecondaryContainer = LightOnSuccess,
    tertiary = LightWarning,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = LightWarningSoft,
    onTertiaryContainer = LightOnWarning,
    error = LightDanger,
    onError = Color(0xFFFFFFFF),
    errorContainer = LightDangerSoft,
    onErrorContainer = LightOnDanger,
    background = LightBackground,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightMuted,
    outline = LightBorderStrong,
    outlineVariant = LightBorder,
    inverseSurface = LightSidebar,
    inverseOnSurface = Color(0xFFE9E5D9),
)

private val DarkScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimarySoft,
    onPrimaryContainer = DarkOnSuccess,
    secondary = DarkSuccess,
    onSecondary = Color(0xFF10231B),
    secondaryContainer = DarkSuccessSoft,
    onSecondaryContainer = DarkOnSuccess,
    tertiary = DarkWarning,
    onTertiary = Color(0xFF2E2408),
    tertiaryContainer = DarkWarningSoft,
    onTertiaryContainer = DarkOnWarning,
    error = DarkDanger,
    onError = Color(0xFF3B1207),
    errorContainer = DarkDangerSoft,
    onErrorContainer = DarkOnDanger,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMuted,
    outline = DarkBorderStrong,
    outlineVariant = DarkBorder,
    inverseSurface = DarkText,
    inverseOnSurface = DarkBackground,
)

/** Raios do design system: 8 / 10 / 14 / 20. */
private val FinancesShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

val LocalFinancesColors = staticCompositionLocalOf { LightExtended }

@Composable
fun FinancesTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkExtended else LightExtended
    CompositionLocalProvider(LocalFinancesColors provides colors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = FinancesTypography,
            shapes = FinancesShapes,
            content = content,
        )
    }
}

object FinancesTheme {
    val colors: FinancesColors
        @Composable get() = LocalFinancesColors.current
}
