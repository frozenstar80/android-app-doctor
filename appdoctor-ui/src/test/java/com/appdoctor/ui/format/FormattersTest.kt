package com.appdoctor.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for the dashboard [Formatters]. */
class FormattersTest {

    @Test
    fun `bytes formats across units`() {
        assertEquals("512 B", Formatters.bytes(512))
        assertEquals("1.0 KB", Formatters.bytes(1_024))
        assertEquals("1.5 KB", Formatters.bytes(1_536))
        assertEquals("1.0 MB", Formatters.bytes(1_048_576))
    }

    @Test
    fun `percent and fps use one decimal`() {
        assertEquals("42.1%", Formatters.percent(42.1f))
        assertEquals("59.9", Formatters.fps(59.94f))
    }
}
