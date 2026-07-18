package com.appdoctor.network.internal

import com.appdoctor.network.model.CapturedBody
import com.appdoctor.network.model.NetworkRequestRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object NetworkRequestTextExporter {
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun toText(record: NetworkRequestRecord): String = buildString {
        appendLine("${record.method} ${record.url}")
        appendLine("Time: ${timestampFormat.format(Date(record.timestampMillis))}")
        appendLine("Duration: ${record.responseTimeMillis} ms")
        appendLine("Status: ${record.statusCode ?: "FAILED"}")
        appendLine("Success: ${record.success}")
        if (!record.failureMessage.isNullOrBlank()) appendLine("Failure: ${record.failureMessage}")
        appendLine()

        appendLine("=== Request Headers ===")
        record.requestHeaders.forEach { appendLine("${it.name}: ${it.value}") }
        appendLine()
        appendLine("=== Request Body ===")
        appendLine(bodyText(record.requestBody))
        appendLine()

        appendLine("=== Response Headers ===")
        record.responseHeaders.forEach { appendLine("${it.name}: ${it.value}") }
        appendLine()
        appendLine("=== Response Body ===")
        appendLine(bodyText(record.responseBody))
    }

    private fun bodyText(body: CapturedBody?): String {
        if (body == null) return "(Not captured)"
        val formatted = BodyPreviewFormatter.format(body.contentType, body.text, body.isBinary)
        return if (body.truncated && formatted.isNotBlank()) "$formatted\n…truncated" else formatted
    }
}
