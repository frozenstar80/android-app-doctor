package com.appdoctor.compose.internal.runtime

/**
 * Turns repeated samples of per-`Recomposer` `changeCount` values into a **monotonic**
 * cumulative recomposition total.
 *
 * `RecomposerInfo.changeCount` is cumulative per recomposer, but the set of running
 * recomposers changes over the app's life (each window owns one, dialogs add more, etc.).
 * Naively summing the current set would make the total dip whenever a recomposer disappears.
 * This accumulator instead adds only the positive delta of each recomposer against its last
 * observed value, so the running total never decreases. A vanished recomposer keeps the
 * contribution it already made and is dropped from the tracking map to keep it bounded.
 *
 * Not thread-safe: call [update] from a single sampling coroutine. Pure and allocation-light
 * so the tricky delta logic is unit-testable without any Compose runtime. Not public API.
 */
internal class RecompositionCounter<K : Any> {

    private val lastSeen = HashMap<K, Long>()
    private var cumulative = 0L

    /** The running cumulative total across every recomposer ever observed. */
    val total: Long get() = cumulative

    /**
     * Folds the current `recomposer -> changeCount` snapshot into [total] and returns it.
     * Negative per-recomposer deltas are ignored (a fresh recomposer contributes its whole
     * current count once).
     */
    fun update(current: Map<K, Long>): Long {
        for ((key, count) in current) {
            val previous = lastSeen[key]
            cumulative += if (previous == null) {
                count.coerceAtLeast(0L)
            } else {
                (count - previous).coerceAtLeast(0L)
            }
            lastSeen[key] = count
        }
        if (lastSeen.size > current.size) {
            lastSeen.keys.retainAll(current.keys)
        }
        return cumulative
    }
}
