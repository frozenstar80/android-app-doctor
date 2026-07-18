package com.appdoctor.ui.format

import java.util.Locale

/**
 * Small, allocation-light formatting helpers shared across the dashboard.
 */
internal object Formatters {

    private const val KILO = 1024.0

    /** Formats a byte count as a human-readable string, e.g. `1536 -> "1.5 KB"`. */
    fun bytes(value: Long): String {
        if (value < KILO) return "$value B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var size = value / KILO
        var unitIndex = 0
        while (size >= KILO && unitIndex < units.lastIndex) {
            size /= KILO
            unitIndex++
        }
        return String.format(Locale.US, "%.1f %s", size, units[unitIndex])
    }

    /** Formats a `0..100` percentage with one decimal, e.g. `"42.1%"`. */
    fun percent(value: Float): String = String.format(Locale.US, "%.1f%%", value)

    /** Formats a frame rate with one decimal, e.g. `"59.7"`. */
    fun fps(value: Float): String = String.format(Locale.US, "%.1f", value)
}
