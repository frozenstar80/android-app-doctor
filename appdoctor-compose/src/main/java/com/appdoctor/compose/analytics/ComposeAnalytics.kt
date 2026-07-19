package com.appdoctor.compose.analytics

/** A composable name paired with a count (e.g. recompositions). */
public data class NamedCount(public val name: String, public val count: Long)

/** A composable name paired with a duration in milliseconds (e.g. lifetime). */
public data class NamedDuration(public val name: String, public val millis: Long)

/** A composable name paired with a per-second frequency. */
public data class NamedFrequency(public val name: String, public val perSecond: Double)

/** Aggregate frame health for the observed window. */
public data class FrameStatistics(
    public val frameCount: Long,
    public val frameDrops: Long,
    public val dropRate: Double,
) {
    public companion object {
        public val Empty: FrameStatistics = FrameStatistics(0L, 0L, 0.0)
    }
}

/** Per-screen tracked-composable statistics. */
public data class ScreenStatistic(
    public val screen: String,
    public val trackedComposables: Int,
    public val recompositions: Long,
)

/**
 * Immutable aggregate of Compose runtime statistics, computed from a runtime snapshot plus
 * the tracked-composable list by [ComposeAnalyticsComputer]. Adding a new metric is a purely
 * additive change here and never touches the collector.
 */
public data class ComposeAnalytics(
    public val totalRecompositions: Long,
    public val averageRecompositionsPerSecond: Double,
    public val trackedComposables: Int,
    public val mostRecomposed: List<NamedCount>,
    public val leastRecomposed: List<NamedCount>,
    public val longestLiving: List<NamedDuration>,
    public val highestFrequency: List<NamedFrequency>,
    public val frame: FrameStatistics,
    public val screens: List<ScreenStatistic>,
) {
    public companion object {
        /** The zero value shown before any sample is produced or when analytics are disabled. */
        public val Empty: ComposeAnalytics = ComposeAnalytics(
            totalRecompositions = 0L,
            averageRecompositionsPerSecond = 0.0,
            trackedComposables = 0,
            mostRecomposed = emptyList(),
            leastRecomposed = emptyList(),
            longestLiving = emptyList(),
            highestFrequency = emptyList(),
            frame = FrameStatistics.Empty,
            screens = emptyList(),
        )
    }
}
