package com.appdoctor.ai.engine

import com.appdoctor.ai.AiConfiguration
import com.appdoctor.ai.AiRequest
import com.appdoctor.session.model.SessionReport

public class AiPromptBuilder {
    public fun build(report: SessionReport, config: AiConfiguration, provider: String, model: String): AiRequest {
        val prompt = buildString {
            appendLine("You are AppDoctor AI. Analyze the following compact session report.")
            appendLine("Return: Executive Summary, Performance Findings, Root Cause Candidates, Recommendations,")
            appendLine("Optimization Opportunities, Risk Assessment, Confidence, Action Items, Next Steps.")
            appendLine()
            appendLine("Health Report:")
            val health = report.healthReport
            if (health == null) {
                appendLine("- unavailable")
            } else {
                appendLine("- overall=${health.overallScore}, perf=${health.performanceScore}, mem=${health.memoryScore}, net=${health.networkScore}, db=${health.databaseScore}, compose=${health.composeScore}")
            }
            appendLine("Issues:")
            val issues = report.diagnostics?.issues.orEmpty().take(8)
            if (issues.isEmpty()) appendLine("- none")
            issues.forEach { issue ->
                appendLine("- ${issue["severity"] ?: "INFO"} ${issue["category"] ?: "UNKNOWN"} ${issue["title"] ?: ""}".trim())
            }
            appendLine("Timeline summary:")
            report.timeline?.let {
                appendLine("- events=${it.totalEvents}, groups=${it.groupedEvents}, issueRefs=${it.issueReferences}")
            } ?: appendLine("- unavailable")
            appendLine("Collector summaries:")
            report.collectorSummaries.entries.take(8).forEach { (collector, summary) ->
                appendLine("- $collector: ${summary.entries.take(4).joinToString { "${it.key}=${it.value}" }}")
            }
            appendLine("Configuration:")
            appendLine("- ${report.configuration.entries.take(8).joinToString { "${it.key}=${it.value}" }}")
            appendLine("App metadata:")
            appendLine("- version=${report.metadata.appVersion}, build=${report.metadata.buildVariant}")
            appendLine("Device metadata:")
            appendLine("- android=${report.metadata.androidVersion}, api=${report.metadata.apiLevel}, manufacturer=${report.metadata.manufacturer}, model=${report.metadata.deviceModel}")
        }
        return AiRequest(
            sessionId = report.sessionId,
            prompt = prompt,
            provider = provider,
            model = model,
            temperature = config.temperature,
            timeoutMillis = config.timeoutMillis,
        )
    }
}
