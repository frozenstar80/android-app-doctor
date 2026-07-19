package com.appdoctor.session.engine

import com.appdoctor.session.SessionExportFormat
import com.appdoctor.session.model.SessionMetadata
import com.appdoctor.session.model.SessionReport
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipInputStream

class SessionExporterTest {
    @Test
    fun `exports json markdown and zip`() {
        val exporter = SessionExporter()
        val report = sampleReport()
        val output = createTempDir(prefix = "session-export")

        val json = exporter.export(report, SessionExportFormat.JSON, output)
        val markdown = exporter.export(report, SessionExportFormat.MARKDOWN, output)
        val zip = exporter.export(report, SessionExportFormat.ZIP, output)

        assertTrue(json.exists() && json.readText().contains("session-1"))
        assertTrue(markdown.exists() && markdown.readText().contains("Session Report"))
        assertTrue(zip.exists())
        assertZipContains(zip, "report.json")
        assertZipContains(zip, "report.md")
        assertZipContains(zip, "timeline.json")
        assertZipContains(zip, "health.json")
        assertZipContains(zip, "diagnostics.json")
        assertZipContains(zip, "metadata.json")
    }

    private fun assertZipContains(file: File, name: String) {
        ZipInputStream(file.inputStream()).use { zip ->
            val names = generateSequence { zip.nextEntry }.map { it.name }.toSet()
            assertTrue(names.contains(name))
        }
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
        collectorSummaries = emptyMap(),
        analyticsSummaries = emptyMap(),
        deviceInformation = emptyMap(),
        applicationInformation = emptyMap(),
        configuration = emptyMap(),
        buildInformation = emptyMap(),
    )
}
