package com.appdoctor.core.internal.extension

import com.appdoctor.extension.ExtensionConfiguration
import com.appdoctor.extension.ExtensionLifecycle
import com.appdoctor.extension.ExtensionVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

public class ExtensionLifecycleTest {
    @Test
    public fun installEnableDisableDestroy_followsDeterministicLifecycle() {
        val runtime = AppDoctorExtensionRuntime(
            context = fakeContext(),
            sdkVersion = ExtensionVersion("10.0.0"),
            configuration = ExtensionConfiguration(enableExtensions = true),
            validator = DefaultExtensionValidator(),
        )
        val extension = FakeExtension(id = "lifecycle.extension")

        runtime.install(extension)
        assertEquals(ExtensionLifecycle.INITIALIZED, runtime.lifecycle("lifecycle.extension"))

        assertTrue(runtime.enable("lifecycle.extension"))
        assertEquals(ExtensionLifecycle.ENABLED, runtime.lifecycle("lifecycle.extension"))

        assertTrue(runtime.disable("lifecycle.extension"))
        assertEquals(ExtensionLifecycle.DISABLED, runtime.lifecycle("lifecycle.extension"))

        assertTrue(runtime.destroy("lifecycle.extension"))
        assertEquals(null, runtime.lifecycle("lifecycle.extension"))
    }
}
