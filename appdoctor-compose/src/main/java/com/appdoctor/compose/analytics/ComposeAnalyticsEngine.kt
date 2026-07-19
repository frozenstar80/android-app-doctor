package com.appdoctor.compose.analytics

import com.appdoctor.compose.model.ComposeRuntimeSnapshot
import com.appdoctor.compose.model.TrackedComposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * Live analytics: continuously recomputes [ComposeAnalytics] from the collector's runtime
 * snapshot stream and the tracker's composable list while observed.
 *
 * Recomputation runs on [Dispatchers.Default] and coalesces bursts with `mapLatest` (which
 * cancels superseded computations), so it never blocks the sampling coroutine. `StateFlow` is
 * already conflated. This is a **fully independent consumer** of the collector — the collector
 * has no knowledge of it. Created only when `enableComposeAnalytics = true`. Not public API.
 */
internal class ComposeAnalyticsEngine(
    runtime: StateFlow<ComposeRuntimeSnapshot>,
    tracked: StateFlow<List<TrackedComposable>>,
    scope: CoroutineScope,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    private val startMillis: Long = clockMillis()

    @OptIn(ExperimentalCoroutinesApi::class)
    val analytics: StateFlow<ComposeAnalytics> =
        combine(runtime, tracked) { snapshot, list -> snapshot to list }
            .mapLatest { (snapshot, list) ->
                ComposeAnalyticsComputer.compute(snapshot, list, clockMillis() - startMillis)
            }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = ComposeAnalytics.Empty,
            )

    private companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
