package com.appdoctor.compose.internal

import java.util.Locale

/**
 * Small, allocation-light formatting helpers for the Compose dashboard tab. Kept UI-free and
 * pure so they are trivially unit-testable (mirrors the database module's `SqlFormatter`).
 * Not public API.
 */
internal object ComposeFormatter {

    private const val NANOS_PER_MILLI = 1_000_000.0

    /** Formats a (possibly large) count compactly, e.g. `950 -> "950"`, `1536 -> "1.5K"`. */
    fun count(value: Long): String = when {
        value < 1_000L -> value.toString()
        value < 1_000_000L -> String.format(Locale.US, "%.1fK", value / 1_000.0)
        value < 1_000_000_000L -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
        else -> String.format(Locale.US, "%.1fB", value / 1_000_000_000.0)
    }

    /** Formats a per-second rate with one decimal, e.g. `12.34 -> "12.3/s"`. */
    fun rate(perSecond: Double): String =
        String.format(Locale.US, "%.1f/s", perSecond.coerceAtLeast(0.0))

    /** Formats a nanosecond duration as milliseconds, e.g. `1_230_000 -> "1.23 ms"`. */
    fun durationNanos(nanos: Double): String =
        String.format(Locale.US, "%.2f ms", nanos / NANOS_PER_MILLI)

    /** Formats a nanosecond duration as milliseconds. */
    fun durationNanos(nanos: Long): String = durationNanos(nanos.toDouble())

    /** Formats a `0..1` fraction as a percentage with one decimal, e.g. `0.128 -> "12.8%"`. */
    fun percent(fraction: Double): String =
        String.format(Locale.US, "%.1f%%", fraction.coerceIn(0.0, 1.0) * 100.0)

    /** Formats an elapsed millisecond span human-readably, e.g. `1500 -> "1.5s"`, `90000 -> "1.5m"`. */
    fun duration(millis: Long): String = when {
        millis < 1_000L -> "${millis}ms"
        millis < 60_000L -> String.format(Locale.US, "%.1fs", millis / 1_000.0)
        millis < 3_600_000L -> String.format(Locale.US, "%.1fm", millis / 60_000.0)
        else -> String.format(Locale.US, "%.1fh", millis / 3_600_000.0)
    }
}
