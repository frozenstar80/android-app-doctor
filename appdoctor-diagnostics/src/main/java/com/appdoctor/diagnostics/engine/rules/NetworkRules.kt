package com.appdoctor.diagnostics.engine.rules

import com.appdoctor.diagnostics.engine.IssueRule
import com.appdoctor.diagnostics.engine.RuleContext
import com.appdoctor.diagnostics.engine.RuleEvidence
import com.appdoctor.diagnostics.engine.RuleMatch
import com.appdoctor.diagnostics.model.IssueCategory
import com.appdoctor.diagnostics.model.IssueSeverity

public class HighAverageNetworkLatencyRule : IssueRule {
    override fun evaluate(context: RuleContext): RuleMatch? {
        val samples = context.networkSamples.take(LATEST_REQUESTS)
        if (samples.size < MIN_REQUESTS) return null
        val avgLatency = samples.map { it.latencyMillis }.average()
        if (avgLatency < LATENCY_THRESHOLD_MS) return null
        val breaches = samples.count { it.latencyMillis >= LATENCY_THRESHOLD_MS }
        return RuleMatch(
            id = "network.high_latency",
            title = "High average network latency",
            description = "Average request latency is ${avgLatency.toInt()}ms across ${samples.size} recent requests.",
            category = IssueCategory.NETWORK,
            severity = IssueSeverity.WARNING,
            collectorIds = listOf("network"),
            documentationLink = DOC_LINK,
            evidence = RuleEvidence(
                supportingPoints = breaches.coerceAtLeast(1),
                totalPoints = samples.size,
                consistency = breaches.toDouble() / samples.size.toDouble(),
            ),
        )
    }

    private companion object {
        private const val LATEST_REQUESTS = 40
        private const val MIN_REQUESTS = 12
        private const val LATENCY_THRESHOLD_MS = 800L
        private const val DOC_LINK = "https://github.com/frozenstar80/android-app-doctor/blob/main/docs/DIAGNOSTICS.md#network-rules"
    }
}

public class RepeatedNetworkFailuresRule : IssueRule {
    override fun evaluate(context: RuleContext): RuleMatch? {
        val samples = context.networkSamples.take(LATEST_REQUESTS)
        if (samples.size < MIN_REQUESTS) return null
        val failures = samples.count { !it.success }
        val failureRate = failures.toDouble() / samples.size.toDouble()
        if (failures < MIN_FAILURES || failureRate < FAILURE_RATE_THRESHOLD) return null
        return RuleMatch(
            id = "network.failure_rate",
            title = "Repeated network failures",
            description = "Recent network failures reached ${(failureRate * 100).toInt()}% ($failures/${samples.size}).",
            category = IssueCategory.NETWORK,
            severity = IssueSeverity.ERROR,
            collectorIds = listOf("network"),
            documentationLink = DOC_LINK,
            evidence = RuleEvidence(
                supportingPoints = failures,
                totalPoints = samples.size,
                consistency = failureRate,
            ),
        )
    }

    private companion object {
        private const val LATEST_REQUESTS = 30
        private const val MIN_REQUESTS = 10
        private const val MIN_FAILURES = 5
        private const val FAILURE_RATE_THRESHOLD = 0.25
        private const val DOC_LINK = "https://github.com/frozenstar80/android-app-doctor/blob/main/docs/DIAGNOSTICS.md#network-rules"
    }
}

public class ExcessiveNetworkRequestVolumeRule : IssueRule {
    override fun evaluate(context: RuleContext): RuleMatch? {
        if (context.networkSamples.isEmpty()) return null
        val windowStart = context.nowMillis - WINDOW_MILLIS
        val recent = context.networkSamples.filter { it.timestampMillis >= windowStart }
        if (recent.size < REQUEST_COUNT_THRESHOLD) return null
        return RuleMatch(
            id = "network.high_volume",
            title = "Excessive network request volume",
            description = "Observed ${recent.size} requests in the last 60 seconds.",
            category = IssueCategory.NETWORK,
            severity = IssueSeverity.INFO,
            collectorIds = listOf("network"),
            documentationLink = DOC_LINK,
            evidence = RuleEvidence(
                supportingPoints = recent.size,
                totalPoints = REQUEST_COUNT_THRESHOLD,
                consistency = 1.0,
            ),
        )
    }

    private companion object {
        private const val WINDOW_MILLIS = 60_000L
        private const val REQUEST_COUNT_THRESHOLD = 120
        private const val DOC_LINK = "https://github.com/frozenstar80/android-app-doctor/blob/main/docs/DIAGNOSTICS.md#network-rules"
    }
}
