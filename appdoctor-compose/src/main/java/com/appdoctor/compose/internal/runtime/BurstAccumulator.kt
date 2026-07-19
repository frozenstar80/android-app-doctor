package com.appdoctor.compose.internal.runtime

/** Aggregate of recomposer busy-burst timings. Durations are in nanoseconds. */
internal data class BurstStats(
    val count: Long,
    val totalNanos: Long,
    val longestNanos: Long,
) {
    /** Mean burst duration in nanoseconds, or `0.0` when no burst has completed. */
    val averageNanos: Double get() = if (count <= 0L) 0.0 else totalNanos.toDouble() / count

    companion object {
        val Empty: BurstStats = BurstStats(count = 0L, totalNanos = 0L, longestNanos = 0L)
    }
}

/**
 * Approximates recomposition **duration** from `Recomposer.State` transitions.
 *
 * A recomposer enters `PendingWork` when it has recompositions to apply and returns to
 * `Idle` when the batch is done; the wall-clock span between the two is one "busy burst" —
 * a stable-API proxy for how long a recomposition batch took. This is *not* per-composable
 * timing (no stable API exposes that); it is intentionally coarse and documented as
 * approximate.
 *
 * Each recomposer is tracked independently by an opaque key so concurrent windows don't
 * interfere. Thread-safe (state flows for different recomposers may be collected on
 * different coroutines); all mutation is guarded by the instance monitor. Not public API.
 */
internal class BurstAccumulator {

    private val busyStartNanos = HashMap<Any, Long>()
    private var count = 0L
    private var totalNanos = 0L
    private var longestNanos = 0L

    /** Records a state change for [key]; [busy] is `true` while the recomposer has pending work. */
    @Synchronized
    fun onState(key: Any, busy: Boolean, nowNanos: Long) {
        if (busy) {
            if (!busyStartNanos.containsKey(key)) busyStartNanos[key] = nowNanos
        } else {
            val startedAt = busyStartNanos.remove(key) ?: return
            val duration = (nowNanos - startedAt).coerceAtLeast(0L)
            count++
            totalNanos += duration
            if (duration > longestNanos) longestNanos = duration
        }
    }

    /** Closes any open burst for a recomposer that is no longer running. */
    @Synchronized
    fun forget(key: Any, nowNanos: Long): Unit = onState(key, busy = false, nowNanos = nowNanos)

    /** Immutable snapshot of the accumulated timings. */
    @Synchronized
    fun snapshot(): BurstStats = BurstStats(count = count, totalNanos = totalNanos, longestNanos = longestNanos)
}
