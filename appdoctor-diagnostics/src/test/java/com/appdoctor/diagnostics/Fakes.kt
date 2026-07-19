package com.appdoctor.diagnostics

import com.appdoctor.core.metric.CollectorRegistry
import com.appdoctor.core.metric.Metric
import com.appdoctor.core.metric.MetricCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeCollector<T : Metric>(
    override val id: String,
    initial: T,
) : MetricCollector<T> {
    private val state = MutableStateFlow(initial)
    override val data: StateFlow<T> = state
    override fun snapshot(): T = state.value
    fun emit(value: T) {
        state.value = value
    }
}

internal class FakeCollectorRegistry(
    private val source: List<MetricCollector<Metric>>,
) : CollectorRegistry {
    override val collectors: List<MetricCollector<Metric>> get() = source
    override fun collector(id: String): MetricCollector<Metric>? = source.firstOrNull { it.id == id }
}

internal data class FakeNetworkRequest(
    val id: Long,
    val timestampMillis: Long,
    val responseTimeMillis: Long,
    val success: Boolean,
)

internal data class FakeNetworkMetric(
    val requests: List<FakeNetworkRequest>,
) : Metric

internal data class FakeDatabaseQuery(
    val id: Long,
    val timestampMillis: Long,
    val durationNanos: Long,
    val success: Boolean,
) {
    val durationMillis: Double get() = durationNanos / 1_000_000.0
}

internal data class FakeDatabaseMetric(
    val queries: List<FakeDatabaseQuery>,
) : Metric

internal data class FakeComposeMetric(
    val recompositionRate: Double,
    val frameDropRate: Double,
    val timestampMillis: Long,
) : Metric
