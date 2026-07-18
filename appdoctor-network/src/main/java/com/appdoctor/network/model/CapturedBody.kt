package com.appdoctor.network.model

/**
 * Captured body preview metadata.
 *
 * The preview content is capped by configuration (`maxCapturedBodyBytes`).
 */
public data class CapturedBody(
    public val contentType: String?,
    public val contentLength: Long?,
    public val text: String?,
    public val isBinary: Boolean,
    public val truncated: Boolean,
)
