package com.appdoctor.core.monitor.fps

/**
 * Immutable snapshot of frame-rate statistics.
 *
 * @see FpsMonitor
 */
public data class FpsInfo(
    /** Instantaneous frames-per-second from the most recent frame interval. */
    public val current: Float,
    /** Rolling average FPS over the monitor's sliding window. */
    public val average: Float,
    /** Lowest instantaneous FPS observed since collection started (worst jank). */
    public val lowest: Float,
) {
    public companion object {
        /** Zeroed placeholder used as the initial value before the first frame. */
        public val Empty: FpsInfo = FpsInfo(
            current = 0f,
            average = 0f,
            lowest = 0f,
        )
    }
}
