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

public data class SessionSharePayload(
    public val file: File,
    public val mimeType: String,
)

public class SessionManager(
    private val sessionId: String,
    private val startedAtMillis: Long,
    private val builder: SessionBuilder,
    private val exporter: SessionExporter,
    private val repository: SessionRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    public fun generate(): SessionReport = runBlocking { generateAsync() }

    public suspend fun generateAsync(): SessionReport = withContext(ioDispatcher) {
        builder.build(
            sessionId = sessionId,
            startTimeMillis = startedAtMillis,
            endTimeMillis = clockMillis(),
        )
    }

    public fun save(): SessionReport = runBlocking {
        val report = generateAsync()
        repository.save(report)
        report
    }

    public fun export(format: SessionExportFormat, outputDirectory: File): File = runBlocking {
        val report = generateAsync()
        repository.save(report)
        withContext(ioDispatcher) { exporter.export(report, format, outputDirectory) }
    }

    public fun share(format: SessionExportFormat, outputDirectory: File): SessionSharePayload {
        val file = export(format, outputDirectory)
        return SessionSharePayload(file = file, mimeType = mimeType(format))
    }

    public fun storedReports(): List<SessionReport> = repository.all()

    public fun latestStoredReport(): SessionReport? = repository.latest()

    private fun mimeType(format: SessionExportFormat): String = when (format) {
        SessionExportFormat.JSON -> "application/json"
        SessionExportFormat.MARKDOWN -> "text/markdown"
        SessionExportFormat.ZIP -> "application/zip"
    }
}
