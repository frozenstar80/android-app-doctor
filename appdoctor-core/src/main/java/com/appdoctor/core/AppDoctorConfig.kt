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
 */
public data class AppDoctorConfig(
    public val startEnabled: Boolean = true,
    public val enabledInReleaseBuilds: Boolean = false,
    public val pollingIntervalMillis: Long = DEFAULT_POLLING_INTERVAL_MS,
    public val overlayFactory: OverlayFactory? = null,
    public val plugins: List<AppDoctorPlugin> = emptyList(),
) {
    public companion object {
        /** Default sample interval (1s) for memory & CPU monitors. */
        public const val DEFAULT_POLLING_INTERVAL_MS: Long = 1_000L
    }
}
