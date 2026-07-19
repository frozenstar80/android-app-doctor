package com.appdoctor.extension

/**
 * Declares AppDoctor compatibility and dependency constraints for an extension.
 */
public data class ExtensionCompatibility(
    public val minimumSdkVersion: ExtensionVersion,
    public val maximumSdkVersion: ExtensionVersion,
    public val supportedCapabilities: Set<ExtensionCapabilities.Capability>,
    public val requiredExtensionIds: Set<String> = emptySet(),
)
