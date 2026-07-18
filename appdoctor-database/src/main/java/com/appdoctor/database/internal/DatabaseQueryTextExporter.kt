package com.appdoctor.database.internal

import com.appdoctor.database.model.DatabaseQuery
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Renders a [DatabaseQuery] to plain text for Copy / Export. */
internal object DatabaseQueryTextExporter {

    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun toText(query: DatabaseQuery): String = buildString {
        appendLine("[${query.type}] ${if (query.success) "OK" else "FAILED"}")
        appendLine("Time: ${timestampFormat.format(Date(query.timestampMillis))}")
        appendLine(String.format(Locale.US, "Duration: %.3f ms", query.durationMillis))
        appendLine("Thread: ${query.threadName}")
        appendLine("Database: ${query.databaseName ?: "(unknown)"}")
        query.rowsAffected?.let { appendLine("Rows affected: $it") }
        query.rowsReturned?.let { appendLine("Rows returned: $it") }
        query.transactionId?.let { appendLine("Transaction: #$it") }
        if (!query.error.isNullOrBlank()) appendLine("Exception: ${query.error}")
        appendLine()
        appendLine("=== SQL ===")
        appendLine(SqlFormatter.format(query.sql))
    }
}
