package com.appdoctor.core.plugin

/**
 * Extension point for future AppDoctor features.
 *
 * Phase 1 ships the core metrics + dashboard. Later capabilities — a **Network
 * Inspector**, **Room Inspector**, **Compose Inspector** or a full third-party **plugin
 * system** — are expected to implement this interface and register via
 * [com.appdoctor.core.AppDoctor.registerPlugin] (or
 * [com.appdoctor.core.AppDoctorConfig.plugins]).
 *
 * Lifecycle: [onInstall] is called once when the plugin is registered; [onEnable] /
 * [onDisable] mirror AppDoctor's enabled state and may be called multiple times.
 *
 * Implementations must be thread-safe; callbacks arrive on the caller's thread.
 */
public interface AppDoctorPlugin {

    /** Stable unique identifier, e.g. `"network-inspector"`. */
    public val id: String

    /** Human-readable title for display in the dashboard, e.g. `"Network"`. */
    public val title: String

    /** Called once when the plugin is registered. Wire up long-lived collaborators here. */
    public fun onInstall(context: PluginContext)

    /** Called when AppDoctor becomes enabled. Start doing work here. */
    public fun onEnable() {}

    /** Called when AppDoctor becomes disabled. Stop/pause work here. */
    public fun onDisable() {}
}
