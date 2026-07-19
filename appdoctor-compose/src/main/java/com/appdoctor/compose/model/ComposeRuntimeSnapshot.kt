package com.appdoctor.compose.model

import com.appdoctor.core.metric.Metric

/**
 * Immutable point-in-time snapshot of Compose **runtime** behaviour, produced by
 * `ComposeMetricCollector`.
 *
 * Like the core [com.appdoctor.core.monitor.memory.MemoryInfo] / `CpuInfo` / `FpsInfo`
 * gauges, this is a single struct of counters and implements [Metric] directly (there is no
 * "stream of many" to wrap). Every value is derived from **stable, public** Compose APIs
 * (`Recomposer.runningRecomposers` / `RecomposerInfo`) or the platform `Choreographer`; no
 * experimental or internal Compose APIs are used.
 *
 * ### Reliability of each field
 * Some Compose runtime facts are simply not exposed by stable APIs. Rather than reflect into
 * internals, those fields are reported best-effort and documented here (and in
 * `docs/COMPOSE.md`):
 *
 * | Field | Source | Reliability |
 * |---|---|---|
 * | [recompositionCount] | Σ `RecomposerInfo.changeCount` | reliable (cumulative) |
 * | [recompositionRate] | Δ count / Δ time | reliable |
 * | [averageRecompositionDurationNanos] / [longestRecompositionNanos] | recomposer busy-burst (`state`) timing | approximate |
 * | [compositionCount] | active `Recomposer` roots | coarse proxy |
 * | [activeComposables] | alive **tracked** composables | approximate (needs tracking) |
 * | [compositionDisposalCount] | disposals of **tracked** composables | reliable when tracking on |
 * | [frameCount] / [frameDrops] | `Choreographer` | reliable since first observed |
 * | [skippedRecompositions] | — | not observable via stable APIs → `0` |
 * | [activeAnimations] | — | not observable via stable APIs → `0` |
 * | [currentScreen] | optional sink | optional |
 */
public data class ComposeRuntimeSnapshot(
    /** Approx. count of currently-alive composables that opted into tracking (0 if tracking off). */
    public val activeComposables: Int,
    /** Cumulative recomposition passes applied since process start (Σ `RecomposerInfo.changeCount`). */
    public val recompositionCount: Long,
    /** Recompositions per second over the most recent sampling window. */
    public val recompositionRate: Double,
    /** Recompositions the runtime skipped. Not observable via stable APIs → always `0`. */
    public val skippedRecompositions: Long,
    /** Number of active recomposition roots (running `Recomposer`s) — a coarse composition proxy. */
    public val compositionCount: Int,
    /** Cumulative disposals of tracked composables (reliable only while tracking is enabled). */
    public val compositionDisposalCount: Long,
    /** Mean recomposer busy-burst duration in nanoseconds (approximate recomposition cost). */
    public val averageRecompositionDurationNanos: Double,
    /** Longest recomposer busy-burst duration in nanoseconds seen so far. */
    public val longestRecompositionNanos: Long,
    /** Frames observed via `Choreographer` since the collector was first subscribed to. */
    public val frameCount: Long,
    /** Frames whose interval exceeded the drop threshold (jank), since first observed. */
    public val frameDrops: Long,
    /** Active animations. Not observable via stable APIs → always `0`. */
    public val activeAnimations: Int,
    /** Optional current screen name, when supplied via `AppDoctorCompose.currentScreen`. */
    public val currentScreen: String?,
    /** Wall-clock time this snapshot was produced. */
    public val timestampMillis: Long,
) : Metric {

    /** Fraction (`0..1`) of observed frames that were dropped; `0` when no frames seen yet. */
    public val frameDropRate: Double
        get() = if (frameCount <= 0L) 0.0 else frameDrops.toDouble() / frameCount.toDouble()

    /** Mean recomposition burst duration expressed in milliseconds. */
    public val averageRecompositionDurationMillis: Double
        get() = averageRecompositionDurationNanos / NANOS_PER_MILLI

    /** Longest recomposition burst duration expressed in milliseconds. */
    public val longestRecompositionMillis: Double
        get() = longestRecompositionNanos / NANOS_PER_MILLI

    public companion object {
        private const val NANOS_PER_MILLI = 1_000_000.0

        /** The zero value shown before any sample is produced. */
        public val Empty: ComposeRuntimeSnapshot = ComposeRuntimeSnapshot(
            activeComposables = 0,
            recompositionCount = 0L,
            recompositionRate = 0.0,
            skippedRecompositions = 0L,
            compositionCount = 0,
            compositionDisposalCount = 0L,
            averageRecompositionDurationNanos = 0.0,
            longestRecompositionNanos = 0L,
            frameCount = 0L,
            frameDrops = 0L,
            activeAnimations = 0,
            currentScreen = null,
            timestampMillis = 0L,
        )
    }
}
