package com.appdoctor.extension

/**
 * Validation contract used before an extension is loaded.
 */
public interface ExtensionValidator {
    public fun validate(
        candidate: Extension,
        installed: List<ExtensionMetadata>,
        sdkVersion: ExtensionVersion,
        configuration: ExtensionConfiguration,
    ): List<ValidationIssue>

    public data class ValidationIssue(
        public val code: String,
        public val message: String,
    )
}
