package com.appdoctor.core.internal.extension

import com.appdoctor.extension.Extension
import com.appdoctor.extension.ExtensionCapabilities
import com.appdoctor.extension.ExtensionCompatibility
import com.appdoctor.extension.ExtensionConfiguration
import com.appdoctor.extension.ExtensionContext
import com.appdoctor.extension.ExtensionFactory
import com.appdoctor.extension.ExtensionLogger
import com.appdoctor.extension.ExtensionMetadata
import com.appdoctor.extension.ExtensionVersion

internal fun fakeContext(
    configuration: ExtensionConfiguration = ExtensionConfiguration(enableExtensions = true),
): ExtensionContext = CoreExtensionContext(
    applicationContext = Any(),
    sdkVersion = ExtensionVersion("10.0.0"),
    configuration = configuration,
    logger = object : ExtensionLogger {
        override fun debug(message: String) = Unit
        override fun info(message: String) = Unit
        override fun warn(message: String, throwable: Throwable?) = Unit
        override fun error(message: String, throwable: Throwable?) = Unit
    },
    services = emptyMap(),
)

internal open class FakeExtension(
    id: String = "fake.extension",
    supported: Set<ExtensionCapabilities.Capability> = setOf(ExtensionCapabilities.Capability.COLLECTORS),
    exclusive: Set<ExtensionCapabilities.Capability> = emptySet(),
    requiredExtensions: Set<String> = emptySet(),
    minSdk: String = "10.0.0",
    maxSdk: String = "10.99.99",
    semanticVersion: String = "1.0.0",
    isThirdParty: Boolean = false,
) : Extension {
    override val metadata: ExtensionMetadata = ExtensionMetadata(
        id = id,
        name = id,
        description = id,
        author = "test",
        version = ExtensionVersion(semanticVersion),
        compatibility = ExtensionCompatibility(
            minimumSdkVersion = ExtensionVersion(minSdk),
            maximumSdkVersion = ExtensionVersion(maxSdk),
            supportedCapabilities = supported,
            requiredExtensionIds = requiredExtensions,
        ),
        isThirdParty = isThirdParty,
    )

    override val capabilities: ExtensionCapabilities = ExtensionCapabilities(
        supported = supported,
        exclusive = exclusive,
    )

    override fun install(context: ExtensionContext) = Unit
    override fun initialize(context: ExtensionContext) = Unit
    override fun enable() = Unit
    override fun disable() = Unit
    override fun unload() = Unit
    override fun destroy() = Unit
}

public class ServiceLoaderTestExtensionFactory : ExtensionFactory {
    override val id: String = "test-serviceloader-factory"
    override val priority: Int = 10
    override fun create(context: ExtensionContext): Extension = FakeExtension(
        id = "serviceloader.extension",
        supported = setOf(ExtensionCapabilities.Capability.RECOMMENDATIONS),
    )
}
