package com.appdoctor.core.internal.extension

import com.appdoctor.extension.ExtensionConfiguration
import com.appdoctor.extension.ExtensionVersion
import org.junit.Assert.assertTrue
import org.junit.Test

public class ExtensionValidationTest {
    @Test
    public fun validatorRejectsDuplicateIds() {
        val validator = DefaultExtensionValidator()
        val existing = FakeExtension(id = "duplicate.extension")
        val candidate = FakeExtension(id = "duplicate.extension")
        val issues = validator.validate(
            candidate = candidate,
            installed = listOf(existing.metadata),
            sdkVersion = ExtensionVersion("10.0.0"),
            configuration = ExtensionConfiguration(enableExtensions = true),
        )
        assertTrue(issues.any { it.code == "duplicate.id" })
    }

    @Test
    public fun validatorRejectsInvalidSemVer() {
        val validator = DefaultExtensionValidator()
        val issues = validator.validate(
            candidate = FakeExtension(id = "semver.extension", semanticVersion = "v1"),
            installed = emptyList(),
            sdkVersion = ExtensionVersion("10.0.0"),
            configuration = ExtensionConfiguration(enableExtensions = true),
        )
        assertTrue(issues.any { it.code == "version.extension.invalid" })
    }
}
