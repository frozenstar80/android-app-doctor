package com.appdoctor.ai.engine

import com.appdoctor.ai.AiResponse

public class AiExportFormatter {
    public fun toMarkdown(response: AiResponse): String = buildString {
        appendLine("# AppDoctor AI Analysis")
        appendLine()
        appendLine("- Session: `${response.sessionId}`")
        appendLine("- Provider: `${response.provider}`")
        appendLine("- Model: `${response.model}`")
        appendLine("- Generated: `${response.generatedAtMillis}`")
        appendLine("- Confidence: `${response.analysis?.confidence ?: 0}`")
        appendLine()
        response.analysis?.let { analysis ->
            appendLine("## Executive Summary")
            appendLine(analysis.executiveSummary)
            appendLine()
            appendLine("## Performance Findings")
            analysis.performanceFindings.forEach { appendLine("- $it") }
            appendLine("## Root Cause Candidates")
            analysis.rootCauseCandidates.forEach { appendLine("- $it") }
            appendLine("## Recommendations")
            analysis.recommendations.forEach { appendLine("- $it") }
            appendLine("## Optimization Opportunities")
            analysis.optimizationOpportunities.forEach { appendLine("- $it") }
            appendLine("## Risk Assessment")
            appendLine(analysis.riskAssessment)
            appendLine()
            appendLine("## Action Items")
            analysis.actionItems.forEach { appendLine("- $it") }
            appendLine("## Next Investigation Steps")
            analysis.nextInvestigationSteps.forEach { appendLine("- $it") }
        } ?: appendLine("No analysis available.")
        response.error?.let {
            appendLine()
            appendLine("## Error")
            appendLine("- type: `${it.type}`")
            appendLine("- message: `${it.message}`")
        }
    }

    public fun toJson(response: AiResponse): String = buildString {
        append("{")
        append("\"sessionId\":\"${escape(response.sessionId)}\",")
        append("\"provider\":\"${escape(response.provider)}\",")
        append("\"model\":\"${escape(response.model)}\",")
        append("\"generatedAtMillis\":${response.generatedAtMillis},")
        append("\"latencyMillis\":${response.latencyMillis},")
        append("\"fromCache\":${response.fromCache},")
        append("\"analysis\":")
        response.analysis?.let { analysis ->
            append("{")
            append("\"executiveSummary\":\"${escape(analysis.executiveSummary)}\",")
            append("\"performanceFindings\":${stringList(analysis.performanceFindings)},")
            append("\"rootCauseCandidates\":${stringList(analysis.rootCauseCandidates)},")
            append("\"recommendations\":${stringList(analysis.recommendations)},")
            append("\"optimizationOpportunities\":${stringList(analysis.optimizationOpportunities)},")
            append("\"riskAssessment\":\"${escape(analysis.riskAssessment)}\",")
            append("\"confidence\":${analysis.confidence},")
            append("\"actionItems\":${stringList(analysis.actionItems)},")
            append("\"nextInvestigationSteps\":${stringList(analysis.nextInvestigationSteps)}")
            append("}")
        } ?: append("null")
        append(",\"error\":")
        response.error?.let { error ->
            append("{\"type\":\"${error.type}\",\"message\":\"${escape(error.message)}\"}")
        } ?: append("null")
        append("}")
    }

    private fun stringList(values: List<String>): String =
        values.joinToString(prefix = "[", postfix = "]") { "\"${escape(it)}\"" }

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
}
