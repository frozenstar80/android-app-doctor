package com.appdoctor.timeline

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
    override val collectors: List<MetricCollector<Metric>>,
) : CollectorRegistry {
    override fun collector(id: String): MetricCollector<Metric>? = collectors.firstOrNull { it.id == id }
}

internal data class FakeNetworkRequest(
    val id: Long,
    val timestampMillis: Long,
    val responseTimeMillis: Long,
    val success: Boolean,
    val method: String = "GET",
    val url: String = "https://example.com",
    val statusCode: Int? = 200,
)

internal data class FakeNetworkMetric(
    val requests: List<FakeNetworkRequest>,
) : Metric

internal data class FakeDatabaseQuery(
    val id: Long,
    val timestampMillis: Long,
    val durationNanos: Long,
    val success: Boolean,
    val type: String = "SELECT",
    val threadName: String = "main",
)

internal data class FakeDatabaseMetric(
    val queries: List<FakeDatabaseQuery>,
) : Metric
