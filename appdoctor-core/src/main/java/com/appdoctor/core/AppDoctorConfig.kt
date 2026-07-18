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
    }
}
