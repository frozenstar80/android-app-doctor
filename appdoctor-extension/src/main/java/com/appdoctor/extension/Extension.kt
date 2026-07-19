package com.appdoctor.extension

/**
 * Core extension contract.
 */
public interface Extension {
    public val metadata: ExtensionMetadata
    public val capabilities: ExtensionCapabilities

    public fun install(context: ExtensionContext)
    public fun initialize(context: ExtensionContext)
    public fun enable()
    public fun disable()
    public fun unload()
    public fun destroy()
}
