package com.appdoctor.extension

/**
 * Semantic version descriptor used by AppDoctor extensions.
 *
 * The expected format is `MAJOR.MINOR.PATCH` with optional pre-release/build metadata.
 * Validation is performed by the host runtime.
 */
public data class ExtensionVersion(
    public val semanticVersion: String,
)
