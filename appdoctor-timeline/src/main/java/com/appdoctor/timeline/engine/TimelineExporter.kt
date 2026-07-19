package com.appdoctor.timeline.engine

import com.appdoctor.timeline.model.RuntimeTimelineEvent

public class TimelineExporter(
    private val formatter: TimelineFormatter = TimelineFormatter(),
) {
    public fun exportJson(events: List<RuntimeTimelineEvent>): String {
        val ordered = events.sortedBy { it.timestamp }
        return buildString {
            append("[")
            ordered.forEachIndexed { index, event ->
                if (index > 0) append(",")
                append("{")
                appendJson("timestamp", event.timestamp.toString(), raw = true); append(",")
                appendJson("sessionId", event.sessionId); append(",")
                appendJson("source", event.source); append(",")
                appendJson("collectorId", event.collectorId); append(",")
                appendJson("category", event.category.name); append(",")
                appendJson("title", event.title); append(",")
                appendJson("summary", event.summary); append(",")
                appendJson("severity", event.severity?.name); append(",")
                appendJson("relatedIssueId", event.relatedIssueId); append(",")
                appendJson("sourceMetric", event.sourceMetric); append(",")
                append("\"metadata\":{")
                event.metadata.entries.forEachIndexed { mIndex, (key, value) ->
                    if (mIndex > 0) append(",")
                    appendJson(key, value.toString())
                }
                append("}")
                append("}")
            }
            append("]")
        }
    }

    public fun exportMarkdown(events: List<RuntimeTimelineEvent>): String {
        val ordered = events.sortedBy { it.timestamp }
        return buildString {
            appendLine("# AppDoctor Timeline Export")
            appendLine()
            appendLine("| Timestamp | Collector | Category | Severity | Title | Summary | Related Issue |")
            appendLine("|---|---|---|---|---|---|---|")
            ordered.forEach { event ->
                appendLine(
                    "| ${event.timestamp} | ${event.collectorId} | ${event.category.name} | " +
                        "${event.severity?.name ?: "-"} | ${escapeCell(event.title)} | ${escapeCell(event.summary)} | ${event.relatedIssueId ?: "-"} |",
                )
            }
            appendLine()
            appendLine("## Event Metadata")
            appendLine()
            ordered.forEach { event ->
                appendLine("### ${event.timestamp} — ${event.title}")
                appendLine()
                appendLine("- Session: `${event.sessionId}`")
                appendLine("- Source Metric: `${event.sourceMetric}`")
                appendLine("- Formatted: `${formatter.formatLine(event)}`")
                if (event.metadata.isEmpty()) {
                    appendLine("- Metadata: _(none)_")
                } else {
                    event.metadata.forEach { (key, value) ->
                        appendLine("- $key: `$value`")
                    }
                }
                appendLine()
            }
        }
    }

    private fun StringBuilder.appendJson(key: String, value: String?, raw: Boolean = false) {
        append("\"").append(escapeJson(key)).append("\":")
        if (value == null) {
            append("null")
        } else if (raw) {
            append(value)
        } else {
            append("\"").append(escapeJson(value)).append("\"")
        }
    }

    private fun escapeJson(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")

    private fun escapeCell(value: String): String = value.replace("|", "\\|").replace("\n", " ")
}
