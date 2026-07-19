package com.appdoctor.core.internal.extension

import com.appdoctor.extension.ExtensionCapabilities
import com.appdoctor.extension.ExtensionConfiguration
import com.appdoctor.extension.ExtensionVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

public class ExtensionIntegrationTest {
    @Test
    public fun runtimeInstallsAndTransitionsMultipleExtensions() {
        val runtime = AppDoctorExtensionRuntime(
            context = fakeContext(),
            sdkVersion = ExtensionVersion("10.0.0"),
            configuration = ExtensionConfiguration(enableExtensions = true),
            validator = DefaultExtensionValidator(),
        )
        runtime.install(FakeExtension(id = "integration.one"))
        runtime.install(
            FakeExtension(
                id = "integration.two",
                supported = setOf(ExtensionCapabilities.Capability.RECOMMENDATIONS),
            ),
        )

        runtime.enableAll()
        assertTrue(runtime.isEnabled("integration.one"))
        assertTrue(runtime.isEnabled("integration.two"))

        runtime.disableAll()
        assertEquals(false, runtime.isEnabled("integration.one"))
        assertEquals(false, runtime.isEnabled("integration.two"))
    }
}
