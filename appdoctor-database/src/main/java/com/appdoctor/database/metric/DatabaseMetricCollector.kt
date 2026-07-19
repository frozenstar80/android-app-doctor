package com.appdoctor.database.metric

import com.appdoctor.core.metric.MetricCollector
import com.appdoctor.core.ids.CollectorIds
import com.appdoctor.database.model.DatabaseMetric
import com.appdoctor.database.repository.DatabaseQueryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Adapts the [DatabaseQueryRepository] into a [MetricCollector]. The collector only maps
 * the repository's hot list flow into [DatabaseMetric]; it computes no analytics.
 *
 * Not part of the public API.
 */
internal class DatabaseMetricCollector(
    private val repository: DatabaseQueryRepository,
    scope: CoroutineScope,
) : MetricCollector<DatabaseMetric> {

    override val id: String = ID

    override val data: StateFlow<DatabaseMetric> =
        repository.queries
            .map(::DatabaseMetric)
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = DatabaseMetric(repository.queries.value),
            )

    /** Reads the live repository directly so the snapshot is always current. */
    override fun snapshot(): DatabaseMetric = DatabaseMetric(repository.queries.value)

    internal companion object {
        const val ID: String = CollectorIds.DATABASE
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
