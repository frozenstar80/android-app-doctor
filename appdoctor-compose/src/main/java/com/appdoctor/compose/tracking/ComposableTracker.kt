package com.appdoctor.compose.tracking

import com.appdoctor.compose.model.TrackedComposable
import kotlinx.coroutines.flow.StateFlow

/**
 * Sink for opt-in per-composable tracking. Implementations receive one [onCommit] per
 * committed (re)composition of a `TrackRecompositions(name)` call site and one [onDisposed]
 * when that call site leaves the composition.
 *
 * Contract:
 *  - callbacks arrive on the **main thread** (Compose applies effects there) and must be
 *    cheap and non-blocking so they never slow composition,
 *  - implementations must be safe to read from other threads (the collector samples
 *    [activeCount] / [disposalCount] and the UI collects [tracked]),
 *  - implementations retain only names/primitives — never a `Composition`/`Context`.
 */
public interface ComposableTracker {

    /** Latest snapshot of tracked composables (unordered; the UI sorts/filters). */
    public val tracked: StateFlow<List<TrackedComposable>>

    /** Number of currently-alive tracked composable instances. */
    public val activeCount: Int

    /** Cumulative number of tracked composable disposals observed. */
    public val disposalCount: Long

    /** Whether tracking is currently recording (mirrors AppDoctor's enabled state). */
    public val isEnabled: Boolean

    /**
     * Records a committed composition for [name].
     *
     * @param initialComposition `true` for the call site's first commit (initial
     *   composition), `false` for a subsequent recomposition.
     * @param depth optional hierarchy depth, or [TrackedComposable.UNKNOWN_DEPTH].
     * @param screen optional screen name captured at commit time.
     */
    public fun onCommit(name: String, initialComposition: Boolean, depth: Int, screen: String?)

    /** Records that a tracked instance of [name] left the composition. */
    public fun onDisposed(name: String)

    /** Enables or pauses recording. */
    public fun setEnabled(enabled: Boolean)

    /** Clears all tracked composables (history only; counters reset). */
    public fun clear()
}
