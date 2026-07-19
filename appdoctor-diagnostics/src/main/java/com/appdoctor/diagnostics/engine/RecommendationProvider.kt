package com.appdoctor.diagnostics.engine

import com.appdoctor.diagnostics.model.IssueRecommendation

/**
 * Deterministic recommendation catalog keyed by rule id.
 */
public class RecommendationProvider {

    public fun recommendationFor(ruleId: String): IssueRecommendation = when (ruleId) {
        "memory.sustained_high_usage" -> IssueRecommendation(
            problem = "Sustained high heap utilization",
            reason = "Heap usage stayed above the configured threshold for multiple consecutive samples.",
            recommendation = "Reduce retained allocations, release caches when backgrounded, and move large objects off hot paths.",
            expectedImpact = "Lower memory pressure, fewer OOM risks, smoother foreground performance.",
        )
        "memory.rapid_growth" -> IssueRecommendation(
            problem = "Rapid memory growth",
            reason = "Heap usage increased sharply within a short analysis window.",
            recommendation = "Profile allocation spikes, inspect recent screen transitions, and eliminate short-lived object churn.",
            expectedImpact = "More stable memory curve and lower GC overhead.",
        )
        "network.high_latency" -> IssueRecommendation(
            problem = "High network latency",
            reason = "Observed requests consistently exceed the latency threshold.",
            recommendation = "Batch chatty calls, reduce payload size, and defer non-critical requests from UI-critical paths.",
            expectedImpact = "Faster request completion and improved perceived responsiveness.",
        )
        "network.failure_rate" -> IssueRecommendation(
            problem = "Repeated network failures",
            reason = "A significant fraction of recent requests failed.",
            recommendation = "Audit retry strategy, status-code handling, and connectivity fallback paths.",
            expectedImpact = "Lower failure rate and improved request reliability.",
        )
        "network.high_volume" -> IssueRecommendation(
            problem = "Excessive request volume",
            reason = "Observed request frequency is above the healthy threshold.",
            recommendation = "Debounce duplicate requests, cache repeat reads, and coalesce polling endpoints.",
            expectedImpact = "Lower network overhead and battery usage.",
        )
        "database.slow_queries" -> IssueRecommendation(
            problem = "Slow database queries",
            reason = "Recent query durations are consistently high.",
            recommendation = "Add/verify indexes, avoid full-table scans, and move expensive queries off the main interaction path.",
            expectedImpact = "Lower query latency and reduced UI jank.",
        )
        "database.high_frequency" -> IssueRecommendation(
            problem = "High query frequency",
            reason = "The query execution rate is above the healthy threshold.",
            recommendation = "Batch writes/reads, cache stable results, and avoid repeated identical queries per frame.",
            expectedImpact = "Lower DB contention and improved frame stability.",
        )
        "database.failure_rate" -> IssueRecommendation(
            problem = "High database failure rate",
            reason = "A notable share of recent queries failed.",
            recommendation = "Validate SQL construction, transaction boundaries, and migration/state assumptions.",
            expectedImpact = "Higher data-path reliability and fewer runtime errors.",
        )
        "compose.high_recomposition_rate" -> IssueRecommendation(
            problem = "High recomposition rate",
            reason = "Compose runtime reports sustained recomposition throughput above threshold.",
            recommendation = "Stabilize parameters, use remember/derivedStateOf appropriately, and reduce state fan-out.",
            expectedImpact = "Lower recomposition churn and improved frame consistency.",
        )
        "cross.fps_with_slow_db" -> IssueRecommendation(
            problem = "FPS drops with slow database queries",
            reason = "Frame-rate degradation overlaps with slow query activity.",
            recommendation = "Move heavy queries off critical UI interactions and optimize long-running statements.",
            expectedImpact = "Fewer frame drops during data-heavy screens.",
        )
        "cross.fps_with_slow_network" -> IssueRecommendation(
            problem = "FPS drops with slow network requests",
            reason = "Frame-rate degradation overlaps with slow network activity.",
            recommendation = "Decouple request completion from immediate UI work and defer expensive response parsing.",
            expectedImpact = "Smoother rendering during network-heavy operations.",
        )
        "cross.memory_with_db_activity" -> IssueRecommendation(
            problem = "High memory with repeated database activity",
            reason = "Memory pressure co-occurs with elevated DB operation rate.",
            recommendation = "Stream large query results and avoid retaining query-backed objects longer than needed.",
            expectedImpact = "Reduced memory footprint and lower GC pressure.",
        )
        else -> IssueRecommendation(
            problem = "Runtime health degradation detected",
            reason = "Observed metrics match a deterministic diagnostics rule.",
            recommendation = "Inspect related metrics and optimize the slowest hot path first.",
            expectedImpact = "Improved overall runtime stability.",
        )
    }
}
