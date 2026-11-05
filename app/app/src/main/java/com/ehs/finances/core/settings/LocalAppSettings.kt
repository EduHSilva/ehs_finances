package com.ehs.finances.core.settings

import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppSettings = staticCompositionLocalOf<AppSettings> {
    error("AppSettings não fornecido")
}
