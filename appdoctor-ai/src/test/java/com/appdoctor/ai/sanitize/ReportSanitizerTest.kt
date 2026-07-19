package com.appdoctor.ai.sanitize

import com.appdoctor.ai.sampleReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ReportSanitizerTest {
    @Test
    fun `redacts headers bodies and identifiers`() {
        val sanitizer = CompositeReportSanitizer(BuiltInReportSanitizers.defaults() + ApplicationIdentifierSanitizer())
        val sanitized = sanitizer.sanitize(sampleReport())
        assertEquals("[REDACTED]", sanitized.metadata.packageName)
        assertEquals("[REDACTED]", sanitized.metadata.deviceModel)
        assertEquals("[REDACTED]", sanitized.collectorSummaries["network"]?.get("cookie"))
        val timelineUrl = sanitized.timeline?.events?.firstOrNull()?.get("url").orEmpty()
        assertFalse(timelineUrl.contains("?"))
    }
}
