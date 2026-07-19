package com.appdoctor.session

import com.appdoctor.session.engine.SessionBuilder
import com.appdoctor.session.engine.SessionExporter
import com.appdoctor.session.engine.SessionRepository
import com.appdoctor.session.model.SessionReport
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Share-ready export metadata for a generated session report artifact.
 *
 * @property file generated export file.
 * @property mimeType MIME type associated with [file].
 */
public data class SessionSharePayload(
    public val file: File,
    public val mimeType: String,
)

/**
 * High-level facade for generating, storing, exporting, and sharing session reports.
 *
 * Operations are executed on [ioDispatcher] for predictable background behavior.
 */
public class SessionManager(
    private val sessionId: String,
    private val startedAtMillis: Long,
    private val builder: SessionBuilder,
    private val exporter: SessionExporter,
    private val repository: SessionRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    /**
     * Synchronously generates a fresh session report.
     *
     * @return generated report snapshot.
     */
    public fun generate(): SessionReport = runBlocking { generateAsync() }

    /**
     * Asynchronously generates a fresh session report.
     *
     * @return generated report snapshot.
     */
    public suspend fun generateAsync(): SessionReport = withContext(ioDispatcher) {
        builder.build(
            sessionId = sessionId,
            startTimeMillis = startedAtMillis,
            endTimeMillis = clockMillis(),
        )
    }

    /**
     * Generates and stores the report in the in-memory repository.
     *
     * @return stored report instance.
     */
    public fun save(): SessionReport = runBlocking {
        val report = generateAsync()
        repository.save(report)
        report
    }

    /**
     * Generates, stores, and exports a report file in the selected [format].
     *
     * @param format target export format.
     * @param outputDirectory destination directory.
     * @return exported file reference.
     */
    public fun export(format: SessionExportFormat, outputDirectory: File): File = runBlocking {
        val report = generateAsync()
        repository.save(report)
        withContext(ioDispatcher) { exporter.export(report, format, outputDirectory) }
    }

    /**
     * Generates and exports a report with share metadata.
     *
     * @param format target export format.
     * @param outputDirectory destination directory.
     * @return payload containing file and MIME type.
     */
    public fun share(format: SessionExportFormat, outputDirectory: File): SessionSharePayload {
        val file = export(format, outputDirectory)
        return SessionSharePayload(file = file, mimeType = mimeType(format))
    }

    /**
     * Returns all currently stored reports (newest first).
     */
    public fun storedReports(): List<SessionReport> = repository.all()

    /**
     * Returns the most recently stored report, if any.
     */
    public fun latestStoredReport(): SessionReport? = repository.latest()

    private fun mimeType(format: SessionExportFormat): String = when (format) {
        SessionExportFormat.JSON -> "application/json"
        SessionExportFormat.MARKDOWN -> "text/markdown"
        SessionExportFormat.ZIP -> "application/zip"
    }
}
