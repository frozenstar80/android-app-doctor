package com.appdoctor.compose.analytics

import com.appdoctor.compose.model.ComposeRuntimeSnapshot
import com.appdoctor.compose.model.TrackedComposable

/**
 * Pure, stateless aggregation of a runtime snapshot + tracked composables into
 * [ComposeAnalytics].
 *
 * Kept free of coroutines and of the collector so it is trivially testable and so new metrics
 * can be added without touching the collection path. **The collector never calls this** — the
 * analytics engine does, only when analytics are enabled. Not public API.
 */
internal object ComposeAnalyticsComputer {

    private const val TOP_N = 5

    fun compute(
        snapshot: ComposeRuntimeSnapshot,
        tracked: List<TrackedComposable>,
        elapsedMillis: Long,
    ): ComposeAnalytics {
        val elapsedSeconds = elapsedMillis / 1_000.0
        val averagePerSecond =
            if (elapsedSeconds > 0.0) snapshot.recompositionCount / elapsedSeconds else 0.0

        val frame = FrameStatistics(
            frameCount = snapshot.frameCount,
            frameDrops = snapshot.frameDrops,
            dropRate = snapshot.frameDropRate,
        )

        if (tracked.isEmpty()) {
            return ComposeAnalytics.Empty.copy(
                totalRecompositions = snapshot.recompositionCount,
                averageRecompositionsPerSecond = averagePerSecond,
                frame = frame,
            )
        }

        val mostRecomposed = tracked.sortedByDescending { it.recompositions }
            .take(TOP_N)
            .map { NamedCount(it.name, it.recompositions) }

        val leastRecomposed = tracked.sortedBy { it.recompositions }
            .take(TOP_N)
            .map { NamedCount(it.name, it.recompositions) }

        val longestLiving = tracked.sortedByDescending { it.lifetimeMillis }
            .take(TOP_N)
            .map { NamedDuration(it.name, it.lifetimeMillis) }

        val highestFrequency = tracked.sortedByDescending { it.recompositionFrequencyPerSecond }
            .take(TOP_N)
            .map { NamedFrequency(it.name, it.recompositionFrequencyPerSecond) }

        val screens = tracked.groupBy { it.screen ?: UNKNOWN_SCREEN }
            .map { (screen, group) ->
                ScreenStatistic(
                    screen = screen,
                    trackedComposables = group.size,
                    recompositions = group.sumOf { it.recompositions },
                )
            }
            .sortedByDescending { it.recompositions }

        return ComposeAnalytics(
            totalRecompositions = snapshot.recompositionCount,
            averageRecompositionsPerSecond = averagePerSecond,
            trackedComposables = tracked.size,
            mostRecomposed = mostRecomposed,
            leastRecomposed = leastRecomposed,
            longestLiving = longestLiving,
            highestFrequency = highestFrequency,
            frame = frame,
            screens = screens,
        )
    }

    private const val UNKNOWN_SCREEN = "(unknown)"
}
