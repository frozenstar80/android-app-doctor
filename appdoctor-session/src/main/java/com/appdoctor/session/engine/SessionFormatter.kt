package com.appdoctor.session.engine

import com.appdoctor.session.model.DiagnosticsSection
import com.appdoctor.session.model.HealthSection
import com.appdoctor.session.model.SessionMetadata
import com.appdoctor.session.model.SessionReport
import com.appdoctor.session.model.TimelineSection

public class SessionFormatter {
    public fun reportJson(report: SessionReport): String = writeJson(
        linkedMapOf(
            "sessionId" to report.sessionId,
            "generatedAtMillis" to report.generatedAtMillis,
            "durationMillis" to report.durationMillis,
            "metadata" to metadataMap(report.metadata),
            "timeline" to timelineMap(report.timeline),
            "diagnostics" to diagnosticsMap(report.diagnostics),
            "healthReport" to healthMap(report.healthReport),
            "collectorSummaries" to report.collectorSummaries,
            "analyticsSummaries" to report.analyticsSummaries,
            "deviceInformation" to report.deviceInformation,
            "applicationInformation" to report.applicationInformation,
            "configuration" to report.configuration,
            "buildInformation" to report.buildInformation,
        ),
    )

    public fun metadataJson(metadata: SessionMetadata): String = writeJson(metadataMap(metadata))

    public fun timelineJson(timeline: TimelineSection?): String = writeJson(timelineMap(timeline))

    public fun diagnosticsJson(diagnostics: DiagnosticsSection?): String = writeJson(diagnosticsMap(diagnostics))

    public fun healthJson(health: HealthSection?): String = writeJson(healthMap(health))

    public fun reportMarkdown(report: SessionReport): String = buildString {
        appendLine("# AppDoctor Session Report")
        appendLine()
        appendLine("- Session ID: `${report.sessionId}`")
        appendLine("- Generated: `${report.generatedAtMillis}`")
        appendLine("- Duration: `${report.durationMillis} ms`")
        appendLine()
        appendLine("## Session Metadata")
        metadataMap(report.metadata).forEach { (key, value) -> appendLine("- $key: `${value ?: "-"}`") }
        appendLine()
        appendLine("## Timeline")
        report.timeline?.let {
            appendLine("- Total events: `${it.totalEvents}`")
            appendLine("- Grouped events: `${it.groupedEvents}`")
            appendLine("- Issue references: `${it.issueReferences}`")
        } ?: appendLine("- Not available")
        appendLine()
        appendLine("## Diagnostics")
        report.diagnostics?.let {
            appendLine("- Issue count: `${it.issueCount}`")
            appendLine("- Open issues: `${it.openIssueCount}`")
        } ?: appendLine("- Not available")
        appendLine()
        appendLine("## Health")
        report.healthReport?.let {
            appendLine("- Overall: `${it.overallScore}`")
            appendLine("- Performance: `${it.performanceScore}`")
            appendLine("- Memory: `${it.memoryScore}`")
            appendLine("- Network: `${it.networkScore}`")
            appendLine("- Database: `${it.databaseScore}`")
            appendLine("- Compose: `${it.composeScore}`")
        } ?: appendLine("- Not available")
        appendLine()
        appendLine("## Collector Summaries")
        if (report.collectorSummaries.isEmpty()) {
            appendLine("- None")
        } else {
            report.collectorSummaries.forEach { (collector, summary) ->
                appendLine("### $collector")
                summary.forEach { (key, value) -> appendLine("- $key: `$value`") }
                appendLine()
            }
        }
        appendLine("## Analytics Summaries")
        if (report.analyticsSummaries.isEmpty()) {
            appendLine("- None")
        } else {
            report.analyticsSummaries.forEach { (module, summary) ->
                appendLine("### $module")
                summary.forEach { (key, value) -> appendLine("- $key: `$value`") }
                appendLine()
            }
        }
    }

    private fun metadataMap(metadata: SessionMetadata): Map<String, Any?> = linkedMapOf(
        "sessionId" to metadata.sessionId,
        "startTimeMillis" to metadata.startTimeMillis,
        "endTimeMillis" to metadata.endTimeMillis,
        "durationMillis" to metadata.durationMillis,
        "appVersion" to metadata.appVersion,
        "versionCode" to metadata.versionCode,
        "buildVariant" to metadata.buildVariant,
        "packageName" to metadata.packageName,
        "deviceModel" to metadata.deviceModel,
        "androidVersion" to metadata.androidVersion,
        "apiLevel" to metadata.apiLevel,
        "manufacturer" to metadata.manufacturer,
        "screenSize" to metadata.screenSize,
        "orientation" to metadata.orientation,
    )

    private fun timelineMap(timeline: TimelineSection?): Map<String, Any?>? = timeline?.let {
        linkedMapOf(
            "totalEvents" to it.totalEvents,
            "groupedEvents" to it.groupedEvents,
            "issueReferences" to it.issueReferences,
            "events" to it.events,
        )
    }

    private fun diagnosticsMap(diagnostics: DiagnosticsSection?): Map<String, Any?>? = diagnostics?.let {
        linkedMapOf(
            "issueCount" to it.issueCount,
            "openIssueCount" to it.openIssueCount,
            "issues" to it.issues,
        )
    }

    private fun healthMap(health: HealthSection?): Map<String, Any?>? = health?.let {
        linkedMapOf(
            "overallScore" to it.overallScore,
            "performanceScore" to it.performanceScore,
            "memoryScore" to it.memoryScore,
            "networkScore" to it.networkScore,
            "databaseScore" to it.databaseScore,
            "composeScore" to it.composeScore,
            "timestampMillis" to it.timestampMillis,
        )
    }

    private fun writeJson(value: Any?): String = buildString { appendJsonValue(this, value) }

    private fun appendJsonValue(builder: StringBuilder, value: Any?) {
        when (value) {
            null -> builder.append("null")
            is String -> builder.append("\"").append(escapeJson(value)).append("\"")
            is Number, is Boolean -> builder.append(value.toString())
            is Map<*, *> -> {
                builder.append("{")
                value.entries.forEachIndexed { index, entry ->
                    if (index > 0) builder.append(",")
                    builder.append("\"").append(escapeJson(entry.key.toString())).append("\":")
                    appendJsonValue(builder, entry.value)
                }
                builder.append("}")
            }
            is Iterable<*> -> {
                builder.append("[")
                value.forEachIndexed { index, item ->
                    if (index > 0) builder.append(",")
                    appendJsonValue(builder, item)
                }
                builder.append("]")
            }
            else -> builder.append("\"").append(escapeJson(value.toString())).append("\"")
        }
    }

    private fun escapeJson(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
}
