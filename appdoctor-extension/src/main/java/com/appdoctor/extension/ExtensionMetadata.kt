package com.appdoctor.extension

/**
 * Stable extension identity and publish-time metadata.
 */
public data class ExtensionMetadata(
    public val id: String,
    public val name: String,
    public val description: String,
    public val author: String,
    public val version: ExtensionVersion,
    public val compatibility: ExtensionCompatibility,
    public val website: String? = null,
    public val isThirdParty: Boolean = true,
)
