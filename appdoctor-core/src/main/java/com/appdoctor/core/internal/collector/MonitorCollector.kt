package com.appdoctor.core.internal.collector

import com.appdoctor.core.metric.Metric
import com.appdoctor.core.metric.MetricCollector
import com.appdoctor.core.monitor.Monitor
import kotlinx.coroutines.flow.StateFlow

/**
 * Adapts an existing [Monitor] into a [MetricCollector] without touching the monitor.
 *
 * The monitor's own hot `StateFlow` is re-exposed verbatim, so there is zero extra
 * sampling, no additional coroutine scope, and identical `WhileSubscribed` idle behaviour.
 *
 * Not part of the public API.
 */
internal class MonitorCollector<T : Metric>(
    override val id: String,
    private val monitor: Monitor<T>,
) : MetricCollector<T> {

    override val data: StateFlow<T> get() = monitor.data
    // snapshot() inherits the default (monitor.data.value): no fresh sampling, so existing
    // runtime cost/behaviour is unchanged.
}
