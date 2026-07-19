package com.appdoctor.diagnostics.engine

import com.appdoctor.diagnostics.model.DiagnosticIssue
import com.appdoctor.diagnostics.model.IssueStatus

/**
 * Applies all rules, calculates confidence, and emits deterministic issues.
 */
public class IssueDetector(
    private val rules: List<IssueRule>,
    private val recommendationProvider: RecommendationProvider,
    private val confidenceCalculator: ConfidenceCalculator,
    private val minimumConfidence: Int,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {

    public fun detect(context: RuleContext): List<DiagnosticIssue> {
        val now = clockMillis()
        return rules.mapNotNull { rule ->
            val match = rule.evaluate(context) ?: return@mapNotNull null
            val confidence = confidenceCalculator.calculate(
                supportingPoints = match.evidence.supportingPoints,
                totalPoints = match.evidence.totalPoints,
                consistency = match.evidence.consistency,
            )
            if (confidence < minimumConfidence) return@mapNotNull null
            DiagnosticIssue(
                id = match.id,
                title = match.title,
                description = match.description,
                category = match.category,
                severity = match.severity,
                confidence = confidence,
                timestampMillis = now,
                collectorIds = match.collectorIds,
                recommendation = recommendationProvider.recommendationFor(match.id),
                documentationLink = match.documentationLink,
                status = IssueStatus.OPEN,
            )
        }
    }
}
