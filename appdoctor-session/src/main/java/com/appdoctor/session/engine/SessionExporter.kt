package com.appdoctor.session.engine

import com.appdoctor.session.SessionExportFormat
import com.appdoctor.session.model.SessionReport
import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

public class SessionExporter(
    private val formatter: SessionFormatter = SessionFormatter(),
) {
    public fun export(
        report: SessionReport,
        format: SessionExportFormat,
        outputDirectory: File,
    ): File {
        if (!outputDirectory.exists()) outputDirectory.mkdirs()
        return when (format) {
            SessionExportFormat.JSON -> {
                val file = File(outputDirectory, "session-${report.sessionId}.json")
                file.writeText(formatter.reportJson(report))
                file
            }
            SessionExportFormat.MARKDOWN -> {
                val file = File(outputDirectory, "session-${report.sessionId}.md")
                file.writeText(formatter.reportMarkdown(report))
                file
            }
            SessionExportFormat.ZIP -> {
                val file = File(outputDirectory, "session-${report.sessionId}.zip")
                ZipOutputStream(BufferedOutputStream(file.outputStream())).use { zip ->
                    zip.put("report.json", formatter.reportJson(report))
                    zip.put("report.md", formatter.reportMarkdown(report))
                    zip.put("timeline.json", formatter.timelineJson(report.timeline))
                    zip.put("health.json", formatter.healthJson(report.healthReport))
                    zip.put("diagnostics.json", formatter.diagnosticsJson(report.diagnostics))
                    zip.put("metadata.json", formatter.metadataJson(report.metadata))
                }
                file
            }
        }
    }

    private fun ZipOutputStream.put(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }
}
