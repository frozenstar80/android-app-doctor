package com.appdoctor.core.metric

import kotlinx.coroutines.flow.StateFlow

/**
 * A named live source of a single [Metric] of type [T].
 *
 * This is the uniform contract future modules (Diagnostics, Timeline, Session Reports)
 * use to observe or pull any metric without knowing the producing module. It deliberately
 * mirrors the existing internal [com.appdoctor.core.monitor.Monitor] shape so Phase 1/2
 * monitors can be *adapted* rather than rewritten.
 */
public interface MetricCollector<out T : Metric> {

    /** Stable, unique identifier, e.g. `"memory"`, `"cpu"`, `"fps"`, `"network"`. */
    public val id: String

    /**
     * The live value as a hot [StateFlow]; the primary streaming API. Collectors backed by
     * `WhileSubscribed` monitors stay idle-cost-zero when nothing observes.
     */
    public val data: StateFlow<T>

    /**
     * The most recent value as a point-in-time read, for non-streaming consumers
     * (Timeline / Reports / Diagnostics). Defaults to the latest [data] value; collectors
     * with a cheaper direct source may override to return a fresher read.
     */
    public fun snapshot(): T = data.value
}
