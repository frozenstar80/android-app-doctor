package com.appdoctor.network.metric

import com.appdoctor.core.metric.MetricCollector
import com.appdoctor.network.repository.NetworkRequestRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Adapts the existing [NetworkRequestRepository] into a [MetricCollector]. The repository
 * is untouched; this only maps its hot list flow into [NetworkMetric].
 *
 * Not part of the public API.
 */
internal class NetworkMetricCollector(
    private val repository: NetworkRequestRepository,
    scope: CoroutineScope,
) : MetricCollector<NetworkMetric> {

    override val id: String = ID

    override val data: StateFlow<NetworkMetric> =
        repository.requests
            .map(::NetworkMetric)
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = NetworkMetric(repository.requests.value),
            )

    /** Reads the live repository directly so the snapshot is always current. */
    override fun snapshot(): NetworkMetric = NetworkMetric(repository.requests.value)

    internal companion object {
        const val ID: String = "network"
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
