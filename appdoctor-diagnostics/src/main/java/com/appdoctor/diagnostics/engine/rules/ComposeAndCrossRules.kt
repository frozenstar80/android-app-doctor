package com.appdoctor.diagnostics.engine.rules

import com.appdoctor.core.ids.CollectorIds
import com.appdoctor.diagnostics.engine.IssueRule
import com.appdoctor.diagnostics.engine.RuleContext
import com.appdoctor.diagnostics.engine.RuleEvidence
import com.appdoctor.diagnostics.engine.RuleMatch
import com.appdoctor.diagnostics.model.IssueCategory
import com.appdoctor.diagnostics.model.IssueSeverity

public class HighComposeRecompositionRateRule : IssueRule {
    override fun evaluate(context: RuleContext): RuleMatch? {
        val samples = context.composeSamples.takeLast(SAMPLE_COUNT)
        if (samples.size < SAMPLE_COUNT) return null
        val avgRate = samples.map { it.recompositionRate }.average()
        val breaches = samples.count { it.recompositionRate >= RATE_THRESHOLD }
        if (avgRate < AVG_RATE_THRESHOLD || breaches < REQUIRED_BREACHES) return null
        return RuleMatch(
            id = "compose.high_recomposition_rate",
            title = "High recomposition rate",
            description = "Compose recomposition rate averaged ${avgRate.toInt()}/s with $breaches high-rate samples.",
            category = IssueCategory.COMPOSE,
            severity = IssueSeverity.WARNING,
            collectorIds = listOf(CollectorIds.COMPOSE),
            documentationLink = DOC_LINK_COMPOSE,
            evidence = RuleEvidence(
                supportingPoints = breaches,
                totalPoints = samples.size,
                consistency = breaches.toDouble() / samples.size.toDouble(),
            ),
        )
    }

    private companion object {
        private const val SAMPLE_COUNT = 5
        private const val RATE_THRESHOLD = 90.0
        private const val AVG_RATE_THRESHOLD = 80.0
        private const val REQUIRED_BREACHES = 3
        private const val DOC_LINK_COMPOSE = "https://github.com/frozenstar80/android-app-doctor/blob/main/docs/DIAGNOSTICS.md#compose-rules"
    }
}

public class FpsDropWithSlowDatabaseRule : IssueRule {
    override fun evaluate(context: RuleContext): RuleMatch? {
        if (context.fpsSamples.isEmpty() || context.databaseSamples.isEmpty()) return null
        val fps = context.fpsSamples.takeLast(FPS_SAMPLES).map { it.averageFps }.average()
        if (fps > FPS_THRESHOLD) return null
        val recentSlowQueries = context.databaseSamples
            .filter { it.timestampMillis >= context.nowMillis - WINDOW_MILLIS && it.durationMillis >= SLOW_QUERY_MS }
        if (recentSlowQueries.size < MIN_SLOW_QUERIES) return null
        return RuleMatch(
            id = "cross.fps_with_slow_db",
            title = "FPS drops alongside slow database queries",
            description = "Average FPS is ${fps.toInt()} while ${recentSlowQueries.size} slow queries were captured in the same window.",
            category = IssueCategory.CROSS_COLLECTOR,
            severity = IssueSeverity.ERROR,
            collectorIds = listOf(CollectorIds.FPS, CollectorIds.DATABASE),
            documentationLink = DOC_LINK_CROSS,
            evidence = RuleEvidence(
                supportingPoints = recentSlowQueries.size,
                totalPoints = MIN_SLOW_QUERIES,
                consistency = 1.0,
            ),
        )
    }

    private companion object {
        private const val FPS_SAMPLES = 5
        private const val FPS_THRESHOLD = 50.0
        private const val WINDOW_MILLIS = 60_000L
        private const val SLOW_QUERY_MS = 40.0
        private const val MIN_SLOW_QUERIES = 5
        private const val DOC_LINK_CROSS = "https://github.com/frozenstar80/android-app-doctor/blob/main/docs/DIAGNOSTICS.md#cross-collector-rules"
    }
}

public class FpsDropWithSlowNetworkRule : IssueRule {
    override fun evaluate(context: RuleContext): RuleMatch? {
        if (context.fpsSamples.isEmpty() || context.networkSamples.isEmpty()) return null
        val fps = context.fpsSamples.takeLast(FPS_SAMPLES).map { it.averageFps }.average()
        if (fps > FPS_THRESHOLD) return null
        val recentSlowRequests = context.networkSamples
            .filter { it.timestampMillis >= context.nowMillis - WINDOW_MILLIS && it.latencyMillis >= SLOW_REQUEST_MS }
        if (recentSlowRequests.size < MIN_SLOW_REQUESTS) return null
        return RuleMatch(
            id = "cross.fps_with_slow_network",
            title = "FPS drops alongside slow network requests",
            description = "Average FPS is ${fps.toInt()} while ${recentSlowRequests.size} slow requests were captured in the same window.",
            category = IssueCategory.CROSS_COLLECTOR,
            severity = IssueSeverity.ERROR,
            collectorIds = listOf(CollectorIds.FPS, CollectorIds.NETWORK),
            documentationLink = DOC_LINK_CROSS,
            evidence = RuleEvidence(
                supportingPoints = recentSlowRequests.size,
                totalPoints = MIN_SLOW_REQUESTS,
                consistency = 1.0,
            ),
        )
    }

    private companion object {
        private const val FPS_SAMPLES = 5
        private const val FPS_THRESHOLD = 50.0
        private const val WINDOW_MILLIS = 60_000L
        private const val SLOW_REQUEST_MS = 800L
        private const val MIN_SLOW_REQUESTS = 5
        private const val DOC_LINK_CROSS = "https://github.com/frozenstar80/android-app-doctor/blob/main/docs/DIAGNOSTICS.md#cross-collector-rules"
    }
}

public class HighMemoryWithDatabaseActivityRule : IssueRule {
    override fun evaluate(context: RuleContext): RuleMatch? {
        val memory = context.memorySamples.takeLast(3)
        if (memory.size < 3) return null
        val avgUsage = memory.map { it.usagePercent }.average()
        if (avgUsage < MEMORY_THRESHOLD) return null
        val dbRecent = context.databaseSamples.count { it.timestampMillis >= context.nowMillis - WINDOW_MILLIS }
        if (dbRecent < MIN_DB_ACTIVITY) return null
        return RuleMatch(
            id = "cross.memory_with_db_activity",
            title = "High memory usage with repeated database activity",
            description = "Average memory usage is ${avgUsage.toInt()}% with $dbRecent database operations in the same time window.",
            category = IssueCategory.CROSS_COLLECTOR,
            severity = IssueSeverity.WARNING,
            collectorIds = listOf(CollectorIds.MEMORY, CollectorIds.DATABASE),
            documentationLink = DOC_LINK_CROSS,
            evidence = RuleEvidence(
                supportingPoints = dbRecent,
                totalPoints = MIN_DB_ACTIVITY,
                consistency = 1.0,
            ),
        )
    }

    private companion object {
        private const val MEMORY_THRESHOLD = 80.0
        private const val WINDOW_MILLIS = 60_000L
        private const val MIN_DB_ACTIVITY = 100
        private const val DOC_LINK_CROSS = "https://github.com/frozenstar80/android-app-doctor/blob/main/docs/DIAGNOSTICS.md#cross-collector-rules"
    }
}
