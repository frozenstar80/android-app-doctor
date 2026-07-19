package com.appdoctor.diagnostics.engine

import com.appdoctor.diagnostics.model.DiagnosticIssue
import com.appdoctor.diagnostics.model.HealthReport
import com.appdoctor.diagnostics.model.IssueCategory
import com.appdoctor.diagnostics.model.IssueSeverity
import kotlin.math.roundToInt

/**
 * Computes deterministic health scores from observed metrics and open issues.
 */
public class HealthEngine {

    public fun compute(
        context: RuleContext,
        openIssues: List<DiagnosticIssue>,
        timestampMillis: Long,
    ): HealthReport {
        val memoryScore = scoreMemory(context, openIssues)
        val networkScore = scoreNetwork(context, openIssues)
        val databaseScore = scoreDatabase(context, openIssues)
        val composeScore = scoreCompose(context, openIssues)
        val performanceScore = scorePerformance(context, openIssues)
        val overall = ((memoryScore + networkScore + databaseScore + composeScore + performanceScore) / 5.0)
            .roundToInt()
            .coerceIn(0, 100)

        return HealthReport(
            overallScore = overall,
            performanceScore = performanceScore,
            memoryScore = memoryScore,
            networkScore = networkScore,
            databaseScore = databaseScore,
            composeScore = composeScore,
            timestampMillis = timestampMillis,
        )
    }

    private fun scoreMemory(context: RuleContext, openIssues: List<DiagnosticIssue>): Int {
        if (context.memorySamples.isEmpty()) return 100
        val avgUsage = context.memorySamples.map { it.usagePercent }.average().toFloat()
        val growth = context.memorySamples.last().usagePercent - context.memorySamples.first().usagePercent
        val usagePenalty = ((avgUsage - 60f).coerceAtLeast(0f) * 1.2f).roundToInt()
        val growthPenalty = ((growth - 5f).coerceAtLeast(0f) * 1.5f).roundToInt()
        val issuePenalty = issuePenalty(openIssues) { category, _ ->
            category == IssueCategory.MEMORY || category == IssueCategory.CROSS_COLLECTOR
        }
        return (100 - usagePenalty - growthPenalty - issuePenalty).coerceIn(0, 100)
    }

    private fun scoreNetwork(context: RuleContext, openIssues: List<DiagnosticIssue>): Int {
        if (context.networkSamples.isEmpty()) return 100
        val avgLatency = context.networkSamples.map { it.latencyMillis }.average()
        val failures = context.networkSamples.count { !it.success }
        val failureRate = failures.toDouble() / context.networkSamples.size.toDouble()
        val latencyPenalty = ((avgLatency - 250.0).coerceAtLeast(0.0) / 15.0).roundToInt()
        val failurePenalty = (failureRate * 60.0).roundToInt()
        val issuePenalty = issuePenalty(openIssues) { category, collectors ->
            category == IssueCategory.NETWORK || collectors.contains("network")
        }
        return (100 - latencyPenalty - failurePenalty - issuePenalty).coerceIn(0, 100)
    }

    private fun scoreDatabase(context: RuleContext, openIssues: List<DiagnosticIssue>): Int {
        if (context.databaseSamples.isEmpty()) return 100
        val avgDuration = context.databaseSamples.map { it.durationMillis }.average()
        val failures = context.databaseSamples.count { !it.success }
        val failureRate = failures.toDouble() / context.databaseSamples.size.toDouble()
        val durationPenalty = ((avgDuration - 8.0).coerceAtLeast(0.0) * 1.5).roundToInt()
        val failurePenalty = (failureRate * 70.0).roundToInt()
        val issuePenalty = issuePenalty(openIssues) { category, collectors ->
            category == IssueCategory.DATABASE || collectors.contains("database")
        }
        return (100 - durationPenalty - failurePenalty - issuePenalty).coerceIn(0, 100)
    }

    private fun scoreCompose(context: RuleContext, openIssues: List<DiagnosticIssue>): Int {
        if (context.composeSamples.isEmpty()) return 100
        val avgRate = context.composeSamples.map { it.recompositionRate }.average()
        val avgFrameDropRate = context.composeSamples.map { it.frameDropRate }.average()
        val recompositionPenalty = ((avgRate - 40.0).coerceAtLeast(0.0) * 0.4).roundToInt()
        val frameDropPenalty = (avgFrameDropRate * 120.0).roundToInt()
        val issuePenalty = issuePenalty(openIssues) { category, collectors ->
            category == IssueCategory.COMPOSE || collectors.contains("compose")
        }
        return (100 - recompositionPenalty - frameDropPenalty - issuePenalty).coerceIn(0, 100)
    }

    private fun scorePerformance(context: RuleContext, openIssues: List<DiagnosticIssue>): Int {
        if (context.fpsSamples.isEmpty()) {
            val penalty = issuePenalty(openIssues) { category, _ ->
                category == IssueCategory.PERFORMANCE || category == IssueCategory.CROSS_COLLECTOR
            }
            return (100 - penalty).coerceIn(0, 100)
        }
        val avgFps = context.fpsSamples.map { it.averageFps }.average()
        val fpsPenalty = ((55.0 - avgFps).coerceAtLeast(0.0) * 2.0).roundToInt()
        val issuePenalty = issuePenalty(openIssues) { category, _ ->
            category == IssueCategory.PERFORMANCE || category == IssueCategory.CROSS_COLLECTOR
        }
        return (100 - fpsPenalty - issuePenalty).coerceIn(0, 100)
    }

    private fun issuePenalty(
        openIssues: List<DiagnosticIssue>,
        include: (IssueCategory, List<String>) -> Boolean,
    ): Int = openIssues
        .filter { include(it.category, it.collectorIds) }
        .sumOf {
            when (it.severity) {
                IssueSeverity.INFO -> 2
                IssueSeverity.WARNING -> 6
                IssueSeverity.ERROR -> 12
                IssueSeverity.CRITICAL -> 20
            }
        }
        .coerceAtMost(70)
}
