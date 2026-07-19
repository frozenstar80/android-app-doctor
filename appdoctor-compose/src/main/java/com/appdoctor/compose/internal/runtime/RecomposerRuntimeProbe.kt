package com.appdoctor.compose.internal.runtime

/**
 * Raw sample of Compose recomposition activity, read entirely from **stable** public APIs
 * (`Recomposer.runningRecomposers` + `RecomposerInfo`). Not public API.
 */
internal data class RecomposerStats(
    val cumulativeRecompositions: Long,
    val activeRecomposers: Int,
    val pendingWork: Boolean,
    val burst: BurstStats,
) {
    companion object {
        val Empty: RecomposerStats = RecomposerStats(
            cumulativeRecompositions = 0L,
            activeRecomposers = 0,
            pendingWork = false,
            burst = BurstStats.Empty,
        )
    }
}

/**
 * Observes recomposition activity for the whole process.
 *
 * Split behind an interface so [ComposeRuntimeCollectorEngine] can be unit-tested with a
 * scripted fake, while [DefaultRecomposerRuntimeProbe] wires the real, stable Compose APIs.
 * Not public API.
 */
internal interface RecomposerRuntimeProbe {

    /**
     * Starts continuous burst tracking bound to [scope]; collection is cancelled when the
     * scope is. Called each time the collector's flow becomes active (idle-cost-zero).
     */
    fun activate(scope: kotlinx.coroutines.CoroutineScope)

    /** Reads the current counters. Cheap; safe to call from the sampling coroutine. */
    fun sample(): RecomposerStats
}
