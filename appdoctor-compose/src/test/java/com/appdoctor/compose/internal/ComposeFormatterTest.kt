package com.appdoctor.compose.internal

import org.junit.Assert.assertEquals
import org.junit.Test

class ComposeFormatterTest {

    @Test
    fun `count is compact`() {
        assertEquals("950", ComposeFormatter.count(950L))
        assertEquals("1.5K", ComposeFormatter.count(1_536L))
        assertEquals("2.5M", ComposeFormatter.count(2_500_000L))
        assertEquals("1.0B", ComposeFormatter.count(1_000_000_000L))
    }

    @Test
    fun `rate has one decimal and a per-second suffix`() {
        assertEquals("12.3/s", ComposeFormatter.rate(12.34))
        assertEquals("0.0/s", ComposeFormatter.rate(-5.0)) // clamped to zero
    }

    @Test
    fun `duration from nanos is milliseconds`() {
        assertEquals("1.23 ms", ComposeFormatter.durationNanos(1_230_000L))
    }

    @Test
    fun `percent scales a fraction`() {
        assertEquals("12.8%", ComposeFormatter.percent(0.128))
        assertEquals("100.0%", ComposeFormatter.percent(1.5)) // clamped
    }

    @Test
    fun `duration picks a human unit`() {
        assertEquals("500ms", ComposeFormatter.duration(500L))
        assertEquals("1.5s", ComposeFormatter.duration(1_500L))
        assertEquals("1.5m", ComposeFormatter.duration(90_000L))
    }
}
