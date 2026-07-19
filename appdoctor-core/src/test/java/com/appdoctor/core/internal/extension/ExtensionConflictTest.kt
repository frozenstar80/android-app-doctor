package com.appdoctor.core.internal.extension

import com.appdoctor.extension.ExtensionCapabilities
import com.appdoctor.extension.ExtensionConfiguration
import com.appdoctor.extension.ExtensionVersion
import org.junit.Assert.assertTrue
import org.junit.Test

public class ExtensionConflictTest {
    @Test
    public fun validatorDetectsExclusiveCapabilityConflicts() {
        val validator = DefaultExtensionValidator()
        val installed = FakeExtension(
            id = "installed.extension",
            supported = setOf(ExtensionCapabilities.Capability.EXPORTERS),
        )
        val candidate = FakeExtension(
            id = "candidate.extension",
            supported = setOf(ExtensionCapabilities.Capability.EXPORTERS),
            exclusive = setOf(ExtensionCapabilities.Capability.EXPORTERS),
        )
        val issues = validator.validate(
            candidate = candidate,
            installed = listOf(installed.metadata),
            sdkVersion = ExtensionVersion("10.0.0"),
            configuration = ExtensionConfiguration(enableExtensions = true),
        )

        assertTrue(issues.any { it.code == "capabilities.conflict" })
    }

    @Test
    public fun validatorDetectsMissingDependencies() {
        val validator = DefaultExtensionValidator()
        val candidate = FakeExtension(
            id = "dependency.extension",
            requiredExtensions = setOf("required.extension"),
        )
        val issues = validator.validate(
            candidate = candidate,
            installed = emptyList(),
            sdkVersion = ExtensionVersion("10.0.0"),
            configuration = ExtensionConfiguration(enableExtensions = true),
        )

        assertTrue(issues.any { it.code == "dependencies.missing" })
    }
}
