package com.appdoctor.core.internal.extension

import com.appdoctor.extension.ExtensionConfiguration
import com.appdoctor.extension.ExtensionVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

public class ExtensionRegistryTest {
    @Test
    public fun registryExposesInstalledMetadataCapabilitiesAndState() {
        val runtime = AppDoctorExtensionRuntime(
            context = fakeContext(),
            sdkVersion = ExtensionVersion("10.0.0"),
            configuration = ExtensionConfiguration(enableExtensions = true),
            validator = DefaultExtensionValidator(),
        )
        val extension = FakeExtension(id = "registry.extension")
        runtime.install(extension)

        assertEquals(1, runtime.installed.size)
        assertNotNull(runtime.metadata("registry.extension"))
        assertNotNull(runtime.capabilities("registry.extension"))
        assertEquals(false, runtime.isEnabled("registry.extension"))
    }
}
