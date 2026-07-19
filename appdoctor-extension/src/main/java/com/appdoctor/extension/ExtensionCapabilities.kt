package com.appdoctor.extension

/**
 * Capability surface declared by an extension.
 *
 * Extensions must declare capabilities explicitly so the host can validate and safely
 * compose contributions from multiple extensions.
 */
public data class ExtensionCapabilities(
    public val supported: Set<Capability>,
    public val exclusive: Set<Capability> = emptySet(),
) {
    public enum class Capability {
        COLLECTORS,
        DASHBOARD_TABS,
        DIAGNOSTICS_RULES,
        TIMELINE_EVENT_ENRICHERS,
        SESSION_REPORT_ENRICHERS,
        AI_PROMPT_ENRICHERS,
        REMOTE_INSPECTOR_COMMANDS,
        EXPORTERS,
        FORMATTERS,
        RECOMMENDATIONS,
    }
}
