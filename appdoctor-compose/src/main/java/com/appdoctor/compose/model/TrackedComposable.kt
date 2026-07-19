package com.appdoctor.compose.model

/**
 * Immutable record of a single composable that opted into tracking via
 * `TrackRecompositions(name)`.
 *
 * Tracking is **off by default** and never required for the global runtime metrics. Records
 * hold only primitives and strings — never a `Composition`, `Composer`, `Context` or
 * `Activity` reference — so they can be retained safely.
 *
 * @property name developer-supplied identifier for the tracked composable.
 * @property recompositions number of recompositions observed (excludes the initial
 *   composition, so a freshly composed node reports `0`).
 * @property firstComposedAtMillis wall-clock time the composable first composed.
 * @property lastRecomposedAtMillis wall-clock time of the most recent (re)composition.
 * @property depth optional hierarchy depth from `LocalComposeTrackingDepth`, or
 *   [UNKNOWN_DEPTH] when depth was not provided.
 * @property screen optional screen name captured at first composition.
 * @property disposed whether the composable has left the composition (its scope disposed).
 */
public data class TrackedComposable(
    public val name: String,
    public val recompositions: Long,
    public val firstComposedAtMillis: Long,
    public val lastRecomposedAtMillis: Long,
    public val depth: Int = UNKNOWN_DEPTH,
    public val screen: String? = null,
    public val disposed: Boolean = false,
) {

    /**
     * Elapsed time between the first and most recent (re)composition. For a still-alive
     * composable this grows with each recomposition; for a disposed one it is frozen at the
     * last activity.
     */
    public val lifetimeMillis: Long
        get() = (lastRecomposedAtMillis - firstComposedAtMillis).coerceAtLeast(0L)

    /** Recompositions per second across this composable's [lifetimeMillis]; `0` if instantaneous. */
    public val recompositionFrequencyPerSecond: Double
        get() {
            val seconds = lifetimeMillis / 1_000.0
            return if (seconds <= 0.0) 0.0 else recompositions / seconds
        }

    public companion object {
        /** Sentinel used when a tracked composable did not report a hierarchy depth. */
        public const val UNKNOWN_DEPTH: Int = -1
    }
}
