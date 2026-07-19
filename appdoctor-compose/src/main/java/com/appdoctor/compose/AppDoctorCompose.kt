package com.appdoctor.compose

import com.appdoctor.compose.tracking.ComposableTracker
import com.appdoctor.core.AppDoctor

/**
 * Entry point and process-wide seam for the Compose runtime inspector.
 *
 * The public `TrackRecompositions(...)` composables write to a sink resolved here, exactly
 * like the database module's `AppDoctorDatabase`. This keeps the tracking call sites a true
 * no-op when AppDoctor is not installed or component tracking is disabled (the sink is
 * `null`), so composables can be annotated once and safely left in place — including in
 * release builds.
 *
 * The optional [currentScreen] is a plain string with no `Activity`/`Context` behind it, so
 * setting it can never leak a screen.
 */
public object AppDoctorCompose {

    /**
     * Active per-composable tracking sink, or `null` when tracking is off / AppDoctor is not
     * installed. Set by [AppDoctorComposePlugin] only when
     * [com.appdoctor.core.AppDoctorConfig.enableComposableTracking] is `true`.
     */
    @Volatile
    internal var activeTracker: ComposableTracker? = null
        private set

    /**
     * Optional current screen name surfaced in [com.appdoctor.compose.model.ComposeRuntimeSnapshot]
     * and used to group screen statistics. Defaults to `null`; set it from your navigation
     * layer, or use the `TrackScreen(name)` composable. Holds only a string — never an
     * `Activity` or `Context`.
     */
    @Volatile
    public var currentScreen: String? = null

    internal fun attach(tracker: ComposableTracker) {
        activeTracker = tracker
    }

    internal fun detach(tracker: ComposableTracker) {
        if (activeTracker === tracker) activeTracker = null
    }

    /** The installed Compose inspector plugin instance, if present. */
    @JvmStatic
    public fun installed(): AppDoctorComposePlugin? =
        AppDoctor.plugin(AppDoctorComposePlugin.COMPOSE_PLUGIN_ID) as? AppDoctorComposePlugin
}
