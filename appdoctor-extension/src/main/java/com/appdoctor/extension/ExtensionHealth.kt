package com.appdoctor.extension

/**
 * Runtime health signal for an extension.
 */
public data class ExtensionHealth(
    public val status: Status,
    public val message: String = "",
) {
    public enum class Status {
        HEALTHY,
        DEGRADED,
        UNHEALTHY,
        DISABLED,
    }
}
