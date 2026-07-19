package com.appdoctor.core

import com.appdoctor.core.overlay.OverlayFactory
import com.appdoctor.core.plugin.AppDoctorPlugin

/**
 * Immutable configuration for [AppDoctor.install].
 *
 * Sensible defaults mean most apps never need to touch this — a bare
 * `AppDoctor.install(this)` is enough.
 *
 * @property startEnabled whether the overlay/monitoring is active immediately after
 *   install. Defaults to `true`; set `false` to start hidden and call [AppDoctor.enable]
 *   later (e.g. behind a developer setting).
 * @property enabledInReleaseBuilds **escape hatch.** By default AppDoctor is a complete
 *   no-op unless the host app is debuggable. Set `true` only if you deliberately want it
 *   active in a non-debuggable build (e.g. an internal QA/release flavor). Use with care.
 * @property pollingIntervalMillis sample interval for the memory and CPU monitors.
 *   Defaults to one second, as specified for Phase 1.
 * @property overlayFactory optional custom [OverlayFactory] (Dependency Injection). When
 *   `null`, AppDoctor reflectively loads the `appdoctor-ui` overlay if present.
 * @property plugins plugins to register during install; see [AppDoctorPlugin].
 * @property captureNetwork enables automatic installation of built-in network inspection
 *   plugins found on the classpath (currently `appdoctor-network`).
 * @property captureRequestBody whether request bodies should be captured by network
 *   inspectors.
 * @property captureResponseBody whether response bodies should be captured by network
 *   inspectors.
 * @property maxCapturedBodyBytes max bytes captured per request/response body.
 * @property maxRequests max requests retained in-memory by built-in network inspectors.
 * @property captureDatabase enables automatic installation of built-in database inspection
 *   plugins found on the classpath (currently `appdoctor-database`).
 * @property maxDatabaseQueries max SQL queries retained in-memory by the database inspector.
 * @property slowQueryThresholdMillis a query at or above this duration (ms) is flagged as
 *   "slow" by database analytics. Defaults to the ~16ms frame budget.
 * @property enableDatabaseAnalytics whether the database inspector continuously computes
 *   aggregated runtime statistics. Off by default — when disabled only query history is
 *   collected.
 * @property captureCompose enables automatic installation of built-in Compose runtime
 *   inspection plugins found on the classpath (currently `appdoctor-compose`). The always-on
 *   runtime metrics are idle-cost-zero (only sampled while the dashboard observes them).
 * @property enableComposeAnalytics whether the Compose inspector continuously computes
 *   aggregated runtime statistics. Off by default — when disabled only live metrics and
 *   (optionally) tracked composables are collected.
 * @property enableComposableTracking whether individual composables that opt in via
 *   `TrackRecompositions(...)` are recorded (name, recomposition count, lifetime, …). Off by
 *   default; global runtime metrics do not require it.
 * @property trackedComposableLimit max number of distinct tracked composables retained
 *   in-memory by the Compose inspector when [enableComposableTracking] is on.
 * @property enableDiagnostics enables automatic installation of the optional diagnostics
 *   analysis plugin found on the classpath (`appdoctor-diagnostics`).
 * @property analysisInterval interval at which diagnostics evaluates collector metrics.
 * @property maximumIssueHistory max number of diagnostics issue records retained in-memory.
 * @property minimumConfidence minimum confidence (`0..100`) required to publish an issue.
 * @property enableTimeline enables automatic installation of the optional timeline module
 *   found on the classpath (`appdoctor-timeline`).
 * @property maximumTimelineEvents max timeline events retained in-memory.
 * @property timelineGroupingWindowMillis time window used to group nearby events.
 * @property enableSessionReports enables automatic installation of the optional session
 *   reports module found on the classpath (`appdoctor-session`).
 * @property maximumStoredReports max generated session reports retained in-memory by the
 *   session module repository.
 * @property autoGenerateOnCrash placeholder for future crash-triggered auto-generation.
 */
public data class AppDoctorConfig(
    public val startEnabled: Boolean = true,
    public val enabledInReleaseBuilds: Boolean = false,
    public val pollingIntervalMillis: Long = DEFAULT_POLLING_INTERVAL_MS,
    public val overlayFactory: OverlayFactory? = null,
    public val plugins: List<AppDoctorPlugin> = emptyList(),
    public val captureNetwork: Boolean = true,
    public val captureRequestBody: Boolean = true,
    public val captureResponseBody: Boolean = true,
    public val maxCapturedBodyBytes: Long = DEFAULT_MAX_CAPTURED_BODY_BYTES,
    public val maxRequests: Int = DEFAULT_MAX_REQUESTS,
    public val captureDatabase: Boolean = true,
    public val maxDatabaseQueries: Int = DEFAULT_MAX_DATABASE_QUERIES,
    public val slowQueryThresholdMillis: Long = DEFAULT_SLOW_QUERY_THRESHOLD_MS,
    public val enableDatabaseAnalytics: Boolean = false,
    public val captureCompose: Boolean = true,
    public val enableComposeAnalytics: Boolean = false,
    public val enableComposableTracking: Boolean = false,
    public val trackedComposableLimit: Int = DEFAULT_TRACKED_COMPOSABLE_LIMIT,
    public val enableDiagnostics: Boolean = false,
    public val analysisInterval: Long = DEFAULT_ANALYSIS_INTERVAL_MS,
    public val maximumIssueHistory: Int = DEFAULT_MAXIMUM_ISSUE_HISTORY,
    public val minimumConfidence: Int = DEFAULT_MINIMUM_CONFIDENCE,
    public val enableTimeline: Boolean = false,
    public val maximumTimelineEvents: Int = DEFAULT_MAXIMUM_TIMELINE_EVENTS,
    public val timelineGroupingWindowMillis: Long = DEFAULT_TIMELINE_GROUPING_WINDOW_MS,
    public val enableSessionReports: Boolean = false,
    public val maximumStoredReports: Int = DEFAULT_MAXIMUM_STORED_REPORTS,
    public val autoGenerateOnCrash: Boolean = false,
) {
    public companion object {
        /** Default sample interval (1s) for memory & CPU monitors. */
        public const val DEFAULT_POLLING_INTERVAL_MS: Long = 1_000L
        /** Default per-body capture cap (256 KiB). */
        public const val DEFAULT_MAX_CAPTURED_BODY_BYTES: Long = 262_144L
        /** Default number of retained network requests. */
        public const val DEFAULT_MAX_REQUESTS: Int = 100
        /** Default number of retained database queries. */
        public const val DEFAULT_MAX_DATABASE_QUERIES: Int = 100
        /** Default slow-query threshold (~one dropped frame). */
        public const val DEFAULT_SLOW_QUERY_THRESHOLD_MS: Long = 16L
        /** Default number of retained tracked composables. */
        public const val DEFAULT_TRACKED_COMPOSABLE_LIMIT: Int = 200
        /** Default diagnostics analysis interval (2s). */
        public const val DEFAULT_ANALYSIS_INTERVAL_MS: Long = 2_000L
        /** Default diagnostics issue history bound. */
        public const val DEFAULT_MAXIMUM_ISSUE_HISTORY: Int = 200
        /** Default diagnostics confidence gate. */
        public const val DEFAULT_MINIMUM_CONFIDENCE: Int = 55
        /** Default max timeline events retained in-memory. */
        public const val DEFAULT_MAXIMUM_TIMELINE_EVENTS: Int = 1_000
        /** Default temporal window for event grouping. */
        public const val DEFAULT_TIMELINE_GROUPING_WINDOW_MS: Long = 2_000L
        /** Default max generated reports retained in-memory by session reports. */
        public const val DEFAULT_MAXIMUM_STORED_REPORTS: Int = 10
    }
}
