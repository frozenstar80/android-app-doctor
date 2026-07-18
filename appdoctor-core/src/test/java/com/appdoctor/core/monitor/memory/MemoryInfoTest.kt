package com.appdoctor.core.monitor.memory

import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for the derived [MemoryInfo.usagePercent]. */
class MemoryInfoTest {

    @Test
    fun `usage percent is used over max`() {
        val info = MemoryInfo(usedBytes = 50, maxBytes = 200, freeBytes = 150, nativeAllocatedBytes = 0)
        assertEquals(25f, info.usagePercent, 0.0001f)
    }

    @Test
    fun `usage percent is zero when max is zero`() {
        assertEquals(0f, MemoryInfo.Empty.usagePercent, 0.0001f)
    }
}
