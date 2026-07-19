package com.appdoctor.compose.internal.runtime

import com.appdoctor.compose.model.ComposeRuntimeSnapshot
import com.appdoctor.compose.tracking.ComposableTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive

/**
 * Combines the recomposition and frame probes with the (optional) composable tracker into a
 * single hot [StateFlow] of [ComposeRuntimeSnapshot].
 *
 * ### Idle-cost-zero
 * The flow is shared with `WhileSubscribed`, so the probes are only activated while the
 * dashboard (or an analytics engine) actually observes it; when the last collector leaves,
 * the `Choreographer` callback and the `Recomposer.state` collectors are cancelled. An idle
 * app therefore pays nothing — matching the core monitors.
 *
 * ### No recomposition pressure
 * Everything runs on background coroutines and reads Compose state through plain
 * `StateFlow.value` / `Long` getters; the engine never participates in composition and holds
 * no `Composition`/`Context`/`Activity`. Recomposition **rate** is derived from cumulative
 * count deltas between samples (the collector measures — it does not aggregate analytics).
 *
 * Not public API.
 */
internal class ComposeRuntimeCollectorEngine(
    scope: CoroutineScope,
    private val tracker: ComposableTracker,
    private val recomposerProbe: RecomposerRuntimeProbe = DefaultRecomposerRuntimeProbe(),
    private val frameProbe: FrameStatsProbe = ChoreographerFrameStatsProbe(),
    private val currentScreenProvider: () -> String? = { com.appdoctor.compose.AppDoctorCompose.currentScreen },
    private val sampleIntervalMillis: Long = DEFAULT_SAMPLE_INTERVAL_MS,
    private val clockMillis: () -> Long = System::currentTimeMillis,
    private val clockNanos: () -> Long = System::nanoTime,
) {

    val data: StateFlow<ComposeRuntimeSnapshot> =
        channelFlow {
            recomposerProbe.activate(this)
            frameProbe.activate(this)

            var previous = recomposerProbe.sample()
            var previousNanos = clockNanos()
            emit(previous, frameProbe.snapshot(), rate = 0.0)

            while (isActive) {
                delay(sampleIntervalMillis)
                val current = recomposerProbe.sample()
                val nowNanos = clockNanos()
                val elapsedSeconds = (nowNanos - previousNanos) / NANOS_PER_SECOND
                val rate = if (elapsedSeconds > 0.0) {
                    ((current.cumulativeRecompositions - previous.cumulativeRecompositions) / elapsedSeconds)
                        .coerceAtLeast(0.0)
                } else {
                    0.0
                }
                emit(current, frameProbe.snapshot(), rate)
                previous = current
                previousNanos = nowNanos
            }
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = ComposeRuntimeSnapshot.Empty,
        )

    private suspend fun ProducerScope<ComposeRuntimeSnapshot>.emit(
        recomposer: RecomposerStats,
        frame: FrameStats,
        rate: Double,
    ) {
        send(
            ComposeRuntimeSnapshot(
                activeComposables = tracker.activeCount,
                recompositionCount = recomposer.cumulativeRecompositions,
                recompositionRate = rate,
                skippedRecompositions = 0L,
                compositionCount = recomposer.activeRecomposers,
                compositionDisposalCount = tracker.disposalCount,
                averageRecompositionDurationNanos = recomposer.burst.averageNanos,
                longestRecompositionNanos = recomposer.burst.longestNanos,
                frameCount = frame.frameCount,
                frameDrops = frame.frameDrops,
                activeAnimations = 0,
                currentScreen = currentScreenProvider(),
                timestampMillis = clockMillis(),
            ),
        )
    }

    internal companion object {
        /** Default runtime sampling cadence (1s), matching the core memory/CPU monitors. */
        const val DEFAULT_SAMPLE_INTERVAL_MS: Long = 1_000L
        private const val STOP_TIMEOUT_MS = 2_000L
        private const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
