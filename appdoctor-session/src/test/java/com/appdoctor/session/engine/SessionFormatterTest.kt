package com.appdoctor.session.engine

import com.appdoctor.session.model.SessionMetadata
import com.appdoctor.session.model.SessionReport
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionFormatterTest {
    @Test
    fun `formats json and markdown`() {
        val report = sampleReport()
        val formatter = SessionFormatter()

        val json = formatter.reportJson(report)
        val markdown = formatter.reportMarkdown(report)

        assertTrue(json.contains("\"sessionId\":\"session-1\""))
        assertTrue(json.contains("\"metadata\""))
        assertTrue(markdown.contains("# AppDoctor Session Report"))
        assertTrue(markdown.contains("## Collector Summaries"))
    }

    private fun sampleReport(): SessionReport = SessionReport(
        sessionId = "session-1",
        metadata = SessionMetadata(
            sessionId = "session-1",
            startTimeMillis = 1L,
            endTimeMillis = 2L,
            durationMillis = 1L,
            appVersion = "1.0",
            versionCode = 1L,
            buildVariant = "DEBUG",
            packageName = "com.example",
            deviceModel = "Pixel",
            androidVersion = "15",
            apiLevel = 35,
            manufacturer = "Google",
            screenSize = "1080x2400",
            orientation = "PORTRAIT",
        ),
        generatedAtMillis = 2L,
        durationMillis = 1L,
        timeline = null,
        diagnostics = null,
        healthReport = null,
        collectorSummaries = mapOf("memory" to mapOf("peakMemoryBytes" to "128")),
        analyticsSummaries = emptyMap(),
        deviceInformation = mapOf("model" to "Pixel"),
        applicationInformation = mapOf("package" to "com.example"),
        configuration = mapOf("enableSessionReports" to "true"),
        buildInformation = mapOf("buildVariant" to "DEBUG"),
    )
}
