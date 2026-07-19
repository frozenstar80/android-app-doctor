package com.appdoctor.core.internal.extension

import com.appdoctor.extension.ExtensionConfiguration
import com.appdoctor.extension.ExtensionVersion
import org.junit.Assert.assertTrue
import org.junit.Test

public class ExtensionCompatibilityTest {
    @Test
    public fun strictCompatibilityRejectsOutOfRangeSdk() {
        val validator = DefaultExtensionValidator()
        val extension = FakeExtension(
            id = "compat.extension",
            minSdk = "11.0.0",
            maxSdk = "11.9.9",
        )
        val issues = validator.validate(
            candidate = extension,
            installed = emptyList(),
            sdkVersion = ExtensionVersion("10.0.0"),
            configuration = ExtensionConfiguration(
                enableExtensions = true,
                strictCompatibilityChecking = true,
            ),
        )

        assertTrue(issues.any { it.code == "version.incompatible" })
    }
}
