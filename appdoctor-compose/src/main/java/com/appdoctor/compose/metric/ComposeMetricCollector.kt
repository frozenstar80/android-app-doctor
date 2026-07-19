package com.appdoctor.compose.metric

import com.appdoctor.compose.internal.runtime.ComposeRuntimeCollectorEngine
import com.appdoctor.compose.model.ComposeRuntimeSnapshot
import com.appdoctor.core.ids.CollectorIds
import com.appdoctor.core.metric.MetricCollector
import kotlinx.coroutines.flow.StateFlow

/**
 * Adapts the Compose runtime observation pipeline into the core [MetricCollector] contract
 * (stable id `"compose"`), so it lands in `AppDoctor.collectors` alongside `memory`, `cpu`,
 * `fps`, `network` and `database`.
 *
 * The collector only **exposes** the engine's hot [ComposeRuntimeSnapshot] stream — it
 * computes no analytics (that is the decoupled analytics component's job). Not public API.
 */
internal class ComposeMetricCollector(
    private val engine: ComposeRuntimeCollectorEngine,
) : MetricCollector<ComposeRuntimeSnapshot> {

    override val id: String = ID

    override val data: StateFlow<ComposeRuntimeSnapshot> get() = engine.data

    /** Point-in-time read of the latest runtime snapshot. */
    override fun snapshot(): ComposeRuntimeSnapshot = engine.data.value

    /** Alias of [snapshot] for call sites that read the current value directly. */
    fun current(): ComposeRuntimeSnapshot = engine.data.value

    internal companion object {
        const val ID: String = CollectorIds.COMPOSE
    }
}
