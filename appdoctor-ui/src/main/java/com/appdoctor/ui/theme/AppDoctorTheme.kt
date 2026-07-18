package com.appdoctor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Self-contained dark theme for the AppDoctor dashboard.
 *
 * Intentionally does not follow the host app's theme so diagnostics look identical in
 * every app that embeds AppDoctor.
 */
@Composable
internal fun AppDoctorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppDoctorTokens.ColorScheme,
        typography = AppDoctorTokens.Typography,
        content = content,
    )
}
