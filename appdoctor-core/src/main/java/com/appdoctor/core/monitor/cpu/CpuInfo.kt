package com.appdoctor.core.monitor.cpu

import com.appdoctor.core.metric.Metric

/**
 * Immutable snapshot of the current process's approximate CPU usage.
 *
 * @see CpuMonitor
 */
public data class CpuInfo(
    /**
     * Approximate share of total CPU capacity used by this process over the last
     * sample window, as a percentage in `0f..100f` (100 == all cores fully busy).
     */
    public val processCpuPercent: Float,
    /** Number of processors visible to the process. */
    public val coreCount: Int,
) : Metric {
    public companion object {
        /** Zeroed placeholder used as the initial value before the first delta. */
        public val Empty: CpuInfo = CpuInfo(
            processCpuPercent = 0f,
            coreCount = Runtime.getRuntime().availableProcessors(),
        )
    }
}
