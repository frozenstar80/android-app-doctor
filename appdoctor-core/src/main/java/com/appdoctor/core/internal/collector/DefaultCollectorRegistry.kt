package com.appdoctor.core.internal.collector

import com.appdoctor.core.metric.CollectorRegistry
import com.appdoctor.core.metric.Metric
import com.appdoctor.core.metric.MetricCollector
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe [CollectorRegistry]. Mirrors the concurrency idiom already used for the
 * plugin registry (`CopyOnWriteArrayList`, dedup by id, insertion order preserved).
 *
 * The mutating methods are intentionally not part of the public [CollectorRegistry] API —
 * registration is driven by [com.appdoctor.core.internal.AppDoctorEngine].
 *
 * Not part of the public API.
 */
internal class DefaultCollectorRegistry : CollectorRegistry {

    private val registered = CopyOnWriteArrayList<MetricCollector<Metric>>()

    override val collectors: List<MetricCollector<Metric>>
        get() = registered.toList()

    override fun collector(id: String): MetricCollector<Metric>? =
        registered.firstOrNull { it.id == id }

    fun register(collector: MetricCollector<Metric>) {
        if (registered.any { it.id == collector.id }) return
        registered.add(collector)
    }

    fun unregister(id: String) {
        registered.removeAll { it.id == id }
    }

    fun clear() {
        registered.clear()
    }
}
