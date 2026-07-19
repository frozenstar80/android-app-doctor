package com.appdoctor.extension

/**
 * Lifecycle controller contract for extension management.
 */
public interface ExtensionInstaller {
    public fun install(extension: Extension): ExtensionMetadata
    public fun enable(id: String): Boolean
    public fun disable(id: String): Boolean
    public fun unload(id: String): Boolean
    public fun destroy(id: String): Boolean
}
