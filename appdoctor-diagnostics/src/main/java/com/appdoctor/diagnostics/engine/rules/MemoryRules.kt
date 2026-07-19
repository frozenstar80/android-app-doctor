package com.appdoctor.diagnostics.engine.rules

import com.appdoctor.diagnostics.engine.IssueRule
import com.appdoctor.diagnostics.engine.RuleContext
import com.appdoctor.diagnostics.engine.RuleEvidence
import com.appdoctor.diagnostics.engine.RuleMatch
import com.appdoctor.diagnostics.model.IssueCategory
import com.appdoctor.diagnostics.model.IssueSeverity

public class SustainedHighMemoryUsageRule : IssueRule {
    override fun evaluate(context: RuleContext): RuleMatch? {
        val samples = context.memorySamples.takeLast(SAMPLE_COUNT)
        if (samples.size < SAMPLE_COUNT) return null
        val breaches = samples.count { it.usagePercent >= THRESHOLD_PERCENT }
        if (breaches < REQUIRED_BREACHES) return null
        return RuleMatch(
            id = "memory.sustained_high_usage",
            title = "Sustained high memory usage",
            description = "Heap usage remained above ${THRESHOLD_PERCENT.toInt()}% for $breaches/$SAMPLE_COUNT recent samples.",
            category = IssueCategory.MEMORY,
            severity = IssueSeverity.ERROR,
            collectorIds = listOf("memory"),
            documentationLink = DOC_LINK,
            evidence = RuleEvidence(
                supportingPoints = breaches,
                totalPoints = samples.size,
                consistency = breaches.toDouble() / samples.size.toDouble(),
            ),
        )
    }

    private companion object {
        private const val SAMPLE_COUNT = 6
        private const val REQUIRED_BREACHES = 5
        private const val THRESHOLD_PERCENT = 85f
        private const val DOC_LINK = "https://github.com/frozenstar80/android-app-doctor/blob/main/docs/DIAGNOSTICS.md#memory-rules"
    }
}

public class RapidMemoryGrowthRule : IssueRule {
    override fun evaluate(context: RuleContext): RuleMatch? {
        val window = context.memorySamples.takeLast(SAMPLE_COUNT)
        if (window.size < SAMPLE_COUNT) return null
        val growth = window.last().usagePercent - window.first().usagePercent
        if (growth < GROWTH_THRESHOLD_PERCENT) return null
        val breaches = window.zipWithNext().count { (a, b) -> b.usagePercent > a.usagePercent }
        return RuleMatch(
            id = "memory.rapid_growth",
            title = "Rapid memory growth",
            description = "Heap usage grew by ${growth.toInt()} percentage points across the recent analysis window.",
            category = IssueCategory.MEMORY,
            severity = IssueSeverity.WARNING,
            collectorIds = listOf("memory"),
            documentationLink = DOC_LINK,
            evidence = RuleEvidence(
                supportingPoints = breaches.coerceAtLeast(1),
                totalPoints = (window.size - 1).coerceAtLeast(1),
                consistency = breaches.toDouble() / (window.size - 1).toDouble(),
            ),
        )
    }

    private companion object {
        private const val SAMPLE_COUNT = 6
        private const val GROWTH_THRESHOLD_PERCENT = 18f
        private const val DOC_LINK = "https://github.com/frozenstar80/android-app-doctor/blob/main/docs/DIAGNOSTICS.md#memory-rules"
    }
}
