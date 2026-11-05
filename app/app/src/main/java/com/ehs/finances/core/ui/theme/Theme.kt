package com.ehs.finances.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class FinancesColors(
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val sidebar: Color,
    val sidebarText: Color,
    val subtle: Color,
    val primarySoft: Color,
    val border: Color,
    val borderStrong: Color,
)

private val LightExtended = FinancesColors(
    success = LightSuccess,
    successSoft = LightSuccessSoft,
    warning = LightWarning,
    warningSoft = LightWarningSoft,
    danger = LightDanger,
    dangerSoft = LightDangerSoft,
    sidebar = LightSidebar,
    sidebarText = LightSidebarText,
    subtle = LightSubtle,
    primarySoft = LightPrimarySoft,
    border = LightBorder,
    borderStrong = LightBorderStrong,
)

private val DarkExtended = FinancesColors(
    success = DarkSuccess,
    successSoft = DarkSuccessSoft,
    warning = DarkWarning,
    warningSoft = DarkWarningSoft,
    danger = DarkDanger,
    dangerSoft = DarkDangerSoft,
    sidebar = DarkSidebar,
    sidebarText = DarkSidebarText,
    subtle = DarkSubtle,
    primarySoft = DarkPrimarySoft,
    border = DarkBorder,
    borderStrong = DarkBorderStrong,
)

private val LightScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimarySoft,
    onPrimaryContainer = Color(0xFF0B4A45),
    secondary = LightSuccess,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = LightSuccessSoft,
    onSecondaryContainer = Color(0xFF14532D),
    tertiary = LightWarning,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = LightWarningSoft,
    onTertiaryContainer = Color(0xFF7C3E06),
    error = LightDanger,
    onError = Color(0xFFFFFFFF),
    errorContainer = LightDangerSoft,
    onErrorContainer = Color(0xFF8A1F1F),
    background = LightBackground,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = Color(0xFFF1F4F7),
    onSurfaceVariant = LightMuted,
    outline = LightBorderStrong,
    outlineVariant = LightBorder,
    inverseSurface = LightSidebar,
    inverseOnSurface = Color(0xFFF1F4F6),
)

private val DarkScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimarySoft,
    onPrimaryContainer = Color(0xFFC7FFF6),
    secondary = DarkSuccess,
    onSecondary = Color(0xFF052E1B),
    secondaryContainer = DarkSuccessSoft,
    onSecondaryContainer = Color(0xFFC6F6DA),
    tertiary = DarkWarning,
    onTertiary = Color(0xFF3B2A05),
    tertiaryContainer = DarkWarningSoft,
    onTertiaryContainer = Color(0xFFFFE9B0),
    error = DarkDanger,
    onError = Color(0xFF3B0A0A),
    errorContainer = DarkDangerSoft,
    onErrorContainer = Color(0xFFFFD5D5),
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = Color(0xFF232C34),
    onSurfaceVariant = DarkMuted,
    outline = DarkBorderStrong,
    outlineVariant = DarkBorder,
    inverseSurface = DarkText,
    inverseOnSurface = DarkBackground,
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
            content = content,
        )
    }
}

object FinancesTheme {
    val colors: FinancesColors
        @Composable get() = LocalFinancesColors.current
}
