package com.appdoctor.core.monitor.cpu

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the `/proc/self/stat` parsing that backs [CpuMonitor]. The tricky part
 * is that the `comm` field can itself contain spaces and parentheses.
 */
class CpuMonitorParseTest {

    @Test
    fun `parses utime and stime from a simple stat line`() {
        // comm = "app"; utime = 50, stime = 20 (fields 14 & 15).
        val line = "1234 (app) R 1 1234 1234 0 -1 4194560 100 0 0 0 50 20 5 3 20"
        assertEquals(70L, CpuMonitor.parseProcessTicks(line))
    }

    @Test
    fun `handles comm containing spaces and parentheses`() {
        val line = "42 (weird )(name) R 1 42 42 0 -1 4194560 7 0 0 0 11 4 0 0 20"
        assertEquals(15L, CpuMonitor.parseProcessTicks(line))
    }

    @Test
    fun `returns -1 for malformed input`() {
        assertEquals(-1L, CpuMonitor.parseProcessTicks("not a stat line"))
        assertEquals(-1L, CpuMonitor.parseProcessTicks(""))
    }
}
