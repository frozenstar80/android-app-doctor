package com.appdoctor.core.internal.extension

import com.appdoctor.extension.ExtensionFactory
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ServiceLoader

public class ExtensionServiceLoaderTest {
    @Test
    public fun serviceLoaderFindsExtensionFactories() {
        val factories = ServiceLoader.load(ExtensionFactory::class.java, ExtensionFactory::class.java.classLoader).toList()
        assertTrue(factories.any { it.id == "test-serviceloader-factory" })
    }
}
