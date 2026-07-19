package com.appdoctor.ai.engine

import com.appdoctor.ai.AiAnalysis
import com.appdoctor.session.model.SessionReport

public class AiSessionAnalyzer(
    private val summaryGenerator: AiSummaryGenerator = AiSummaryGenerator(),
    private val recommendationFormatter: AiRecommendationFormatter = AiRecommendationFormatter(),
) {

    public fun fallback(report: SessionReport): AiAnalysis {
        val findings = summaryGenerator.findings(report)
        val recommendations = report.diagnostics?.issues
            ?.mapNotNull { it["title"]?.takeIf(String::isNotBlank) }
            ?.take(5)
            ?: listOf("Inspect open diagnostics and timeline clusters for the highest-impact issue first.")
        val rootCauses = report.diagnostics?.issues
            ?.mapNotNull { issue ->
                val title = issue["title"].orEmpty()
                val category = issue["category"].orEmpty()
                if (title.isBlank()) null else "$category: $title"
            }
            ?.take(5)
            ?: listOf("Insufficient issue metadata; collect another report after reproducing the problem.")
        val opportunities = report.collectorSummaries.entries
            .map { (collector, summary) -> "$collector: ${summary.entries.take(2).joinToString { "${it.key}=${it.value}" }}" }
            .take(5)
            .ifEmpty { listOf("No collector summaries were available.") }
        val confidence = if ((report.diagnostics?.openIssueCount ?: 0) > 0) 72 else 58
        return AiAnalysis(
            executiveSummary = summaryGenerator.executiveSummary(report),
            performanceFindings = findings,
            rootCauseCandidates = rootCauses,
            recommendations = recommendations,
            optimizationOpportunities = opportunities,
            riskAssessment = if ((report.healthReport?.overallScore ?: 100) < 70) {
                "Elevated risk: degraded runtime health indicators were observed."
            } else {
                "Moderate risk: no critical degradation was detected in the aggregated report."
            },
            confidence = confidence,
            actionItems = recommendationFormatter.actionItems(recommendations),
            nextInvestigationSteps = recommendationFormatter.nextSteps(rootCauses),
        )
    }

    public fun parseOrFallback(raw: String, report: SessionReport): AiAnalysis {
        val fallback = fallback(report)
        if (raw.isBlank()) return fallback
        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return fallback
        return fallback.copy(
            executiveSummary = lines.firstOrNull() ?: fallback.executiveSummary,
            performanceFindings = lines.take(4).ifEmpty { fallback.performanceFindings },
        )
    }
}
