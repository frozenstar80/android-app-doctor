package com.appdoctor.diagnostics.engine.rules

import com.appdoctor.diagnostics.engine.IssueRule
import com.appdoctor.diagnostics.engine.RuleContext
import com.appdoctor.diagnostics.engine.RuleEvidence
import com.appdoctor.diagnostics.engine.RuleMatch
import com.appdoctor.diagnostics.model.IssueCategory
import com.appdoctor.diagnostics.model.IssueSeverity

public class SlowDatabaseQueriesRule : IssueRule {
    override fun evaluate(context: RuleContext): RuleMatch? {
        val samples = context.databaseSamples.take(LATEST_QUERIES)
        if (samples.size < MIN_QUERIES) return null
        val avgDuration = samples.map { it.durationMillis }.average()
        val slowCount = samples.count { it.durationMillis >= SLOW_QUERY_MS }
        if (avgDuration < AVERAGE_THRESHOLD_MS && slowCount < MIN_SLOW_COUNT) return null
        return RuleMatch(
            id = "database.slow_queries",
            title = "Slow database queries",
            description = "Average query duration is ${avgDuration.toInt()}ms with $slowCount slow queries in the latest sample window.",
            category = IssueCategory.DATABASE,
            severity = IssueSeverity.WARNING,
            collectorIds = listOf("database"),
            documentationLink = DOC_LINK,
            evidence = RuleEvidence(
                supportingPoints = slowCount.coerceAtLeast(1),
                totalPoints = samples.size,
                consistency = slowCount.toDouble() / samples.size.toDouble(),
            ),
        )
    }

    private companion object {
        private const val LATEST_QUERIES = 50
        private const val MIN_QUERIES = 12
        private const val AVERAGE_THRESHOLD_MS = 25.0
        private const val SLOW_QUERY_MS = 40.0
        private const val MIN_SLOW_COUNT = 6
        private const val DOC_LINK = "https://github.com/frozenstar80/android-app-doctor/blob/main/docs/DIAGNOSTICS.md#database-rules"
    }
}

public class HighDatabaseQueryFrequencyRule : IssueRule {
    override fun evaluate(context: RuleContext): RuleMatch? {
        if (context.databaseSamples.isEmpty()) return null
        val cutoff = context.nowMillis - WINDOW_MILLIS
        val recent = context.databaseSamples.filter { it.timestampMillis >= cutoff }
        if (recent.size < QUERY_COUNT_THRESHOLD) return null
        return RuleMatch(
            id = "database.high_frequency",
            title = "High database query frequency",
            description = "Observed ${recent.size} queries in the last 60 seconds.",
            category = IssueCategory.DATABASE,
            severity = IssueSeverity.INFO,
            collectorIds = listOf("database"),
            documentationLink = DOC_LINK,
            evidence = RuleEvidence(
                supportingPoints = recent.size,
                totalPoints = QUERY_COUNT_THRESHOLD,
                consistency = 1.0,
            ),
        )
    }

    private companion object {
        private const val WINDOW_MILLIS = 60_000L
        private const val QUERY_COUNT_THRESHOLD = 150
        private const val DOC_LINK = "https://github.com/frozenstar80/android-app-doctor/blob/main/docs/DIAGNOSTICS.md#database-rules"
    }
}

public class HighDatabaseFailureRateRule : IssueRule {
    override fun evaluate(context: RuleContext): RuleMatch? {
        val samples = context.databaseSamples.take(LATEST_QUERIES)
        if (samples.size < MIN_QUERIES) return null
        val failures = samples.count { !it.success }
        val failureRate = failures.toDouble() / samples.size.toDouble()
        if (failures < MIN_FAILURES || failureRate < FAILURE_RATE_THRESHOLD) return null
        return RuleMatch(
            id = "database.failure_rate",
            title = "High database failure rate",
            description = "Recent database failures reached ${(failureRate * 100).toInt()}% ($failures/${samples.size}).",
            category = IssueCategory.DATABASE,
            severity = IssueSeverity.ERROR,
            collectorIds = listOf("database"),
            documentationLink = DOC_LINK,
            evidence = RuleEvidence(
                supportingPoints = failures,
                totalPoints = samples.size,
                consistency = failureRate,
            ),
        )
    }

    private companion object {
        private const val LATEST_QUERIES = 40
        private const val MIN_QUERIES = 12
        private const val MIN_FAILURES = 5
        private const val FAILURE_RATE_THRESHOLD = 0.2
        private const val DOC_LINK = "https://github.com/frozenstar80/android-app-doctor/blob/main/docs/DIAGNOSTICS.md#database-rules"
    }
}
