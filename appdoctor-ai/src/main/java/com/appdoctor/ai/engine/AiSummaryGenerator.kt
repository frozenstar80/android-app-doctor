package com.appdoctor.ai.engine

import com.appdoctor.session.model.SessionReport

public class AiSummaryGenerator {

    public fun executiveSummary(report: SessionReport): String {
        val health = report.healthReport?.overallScore
        val issues = report.diagnostics?.openIssueCount ?: 0
        val events = report.timeline?.totalEvents ?: 0
        return "Session ${report.sessionId} ran for ${report.durationMillis} ms, " +
            "health=${health ?: "n/a"}, openIssues=$issues, timelineEvents=$events."
    }

    public fun findings(report: SessionReport): List<String> = buildList {
        report.healthReport?.let {
            add("Health score ${it.overallScore}/100 (perf=${it.performanceScore}, mem=${it.memoryScore}).")
        }
        report.diagnostics?.let {
            add("Diagnostics found ${it.issueCount} issues; ${it.openIssueCount} remain open.")
        }
        report.timeline?.let {
            add("Timeline captured ${it.totalEvents} events with ${it.groupedEvents} grouped clusters.")
        }
    }.ifEmpty { listOf("No major findings were available in the session report.") }
}
