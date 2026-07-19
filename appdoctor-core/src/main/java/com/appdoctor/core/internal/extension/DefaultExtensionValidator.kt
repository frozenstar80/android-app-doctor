package com.appdoctor.core.internal.extension

import com.appdoctor.extension.Extension
import com.appdoctor.extension.ExtensionConfiguration
import com.appdoctor.extension.ExtensionValidator
import com.appdoctor.extension.ExtensionVersion

internal class DefaultExtensionValidator : ExtensionValidator {
    override fun validate(
        candidate: Extension,
        installed: List<com.appdoctor.extension.ExtensionMetadata>,
        sdkVersion: ExtensionVersion,
        configuration: ExtensionConfiguration,
    ): List<ExtensionValidator.ValidationIssue> {
        val issues = mutableListOf<ExtensionValidator.ValidationIssue>()
        val metadata = candidate.metadata
        val compatibility = metadata.compatibility
        val candidateVersion = metadata.version.semanticVersion
        val minVersion = compatibility.minimumSdkVersion.semanticVersion
        val maxVersion = compatibility.maximumSdkVersion.semanticVersion
        val sdk = sdkVersion.semanticVersion

        if (!configuration.enableExtensions) {
            issues += issue("config.disabled", "Extensions are disabled by configuration.")
        }
        if (metadata.id.isBlank()) {
            issues += issue("metadata.id.blank", "Extension id must not be blank.")
        }
        if (metadata.name.isBlank()) {
            issues += issue("metadata.name.blank", "Extension name must not be blank.")
        }
        if (!configuration.allowThirdPartyExtensions && metadata.isThirdParty) {
            issues += issue("security.thirdparty.blocked", "Third-party extensions are blocked by configuration.")
        }
        if (installed.any { it.id == metadata.id }) {
            issues += issue("duplicate.id", "An extension with id '${metadata.id}' is already installed.")
        }

        if (SemVerParser.parse(candidateVersion) == null) {
            issues += issue("version.extension.invalid", "Extension version '$candidateVersion' is not a valid semantic version.")
        }
        val min = SemVerParser.parse(minVersion)
        val max = SemVerParser.parse(maxVersion)
        val current = SemVerParser.parse(sdk)
        if (min == null) issues += issue("version.min.invalid", "Minimum SDK version '$minVersion' is invalid.")
        if (max == null) issues += issue("version.max.invalid", "Maximum SDK version '$maxVersion' is invalid.")
        if (current == null) issues += issue("version.sdk.invalid", "Host SDK version '$sdk' is invalid.")
        if (min != null && max != null && min > max) {
            issues += issue("version.range.invalid", "Minimum SDK version must be <= maximum SDK version.")
        }
        if (configuration.strictCompatibilityChecking && min != null && max != null && current != null) {
            if (current < min || current > max) {
                issues += issue(
                    "version.incompatible",
                    "Host SDK version '$sdk' is outside supported range [$minVersion, $maxVersion].",
                )
            }
        }

        if (candidate.capabilities.supported.isEmpty()) {
            issues += issue("capabilities.empty", "At least one capability must be declared.")
        }
        if (!compatibility.supportedCapabilities.containsAll(candidate.capabilities.supported)) {
            issues += issue(
                "capabilities.unsupported",
                "Extension declared capabilities not present in compatibility.supportedCapabilities.",
            )
        }
        if (!candidate.capabilities.supported.containsAll(candidate.capabilities.exclusive)) {
            issues += issue(
                "capabilities.exclusive.invalid",
                "Exclusive capabilities must be a subset of supported capabilities.",
            )
        }

        val installedCapabilities = installed.flatMap { it.compatibility.supportedCapabilities }.toSet()
        val conflictingExclusive = candidate.capabilities.exclusive.intersect(installedCapabilities)
        if (conflictingExclusive.isNotEmpty()) {
            issues += issue(
                "capabilities.conflict",
                "Exclusive capability conflict: $conflictingExclusive",
            )
        }

        val installedIds = installed.mapTo(mutableSetOf()) { it.id }
        val missingDependencies = compatibility.requiredExtensionIds.filterNot(installedIds::contains)
        if (missingDependencies.isNotEmpty()) {
            issues += issue(
                "dependencies.missing",
                "Missing required extension dependencies: $missingDependencies",
            )
        }

        return issues
    }

    private fun issue(code: String, message: String): ExtensionValidator.ValidationIssue =
        ExtensionValidator.ValidationIssue(code = code, message = message)
}
