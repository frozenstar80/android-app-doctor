package com.appdoctor.database.analytics

import com.appdoctor.database.repository.DatabaseQueryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * Live analytics: continuously recomputes [DatabaseAnalytics] from the repository's query
 * stream while observed. Recomputation runs on [Dispatchers.Default] and coalesces bursts
 * with `mapLatest` (which cancels superseded computations), so it never blocks the threads
 * that collect queries. StateFlow is already conflated, so no explicit `conflate` is needed.
 *
 * Created only when analytics are enabled. Not part of the public API.
 */
internal class DatabaseAnalyticsEngine(
    repository: DatabaseQueryRepository,
    slowThresholdMillis: Long,
    scope: CoroutineScope,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val analytics: StateFlow<DatabaseAnalytics> =
    repository.queries
            .mapLatest { DatabaseAnalyticsComputer.compute(it, slowThresholdMillis) }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = DatabaseAnalytics.Empty,
            )

    private companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
