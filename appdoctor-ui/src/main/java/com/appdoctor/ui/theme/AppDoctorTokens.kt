package com.appdoctor.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Central design tokens for the AppDoctor dashboard. Kept in one place so the UI stays
 * visually consistent and easy to restyle.
 */
internal object AppDoctorTokens {

    val Background = Color(0xFF0E1116)
    val Surface = Color(0xFF161B22)
    val SurfaceVariant = Color(0xFF21262D)
    val Primary = Color(0xFF4F9CF9)
    val OnPrimary = Color(0xFF04121F)
    val OnBackground = Color(0xFFE6EDF3)
    val OnSurfaceMuted = Color(0xFF8B949E)

    val Good = Color(0xFF3FB950)
    val Warn = Color(0xFFD29922)
    val Bad = Color(0xFFF85149)

    val ColorScheme = darkColorScheme(
        primary = Primary,
        onPrimary = OnPrimary,
        background = Background,
        onBackground = OnBackground,
        surface = Surface,
        onSurface = OnBackground,
        surfaceVariant = SurfaceVariant,
        onSurfaceVariant = OnSurfaceMuted,
    )

    val Typography = Typography()
}
