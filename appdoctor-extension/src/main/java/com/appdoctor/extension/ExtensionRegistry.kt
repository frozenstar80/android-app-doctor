package com.appdoctor.extension

/**
 * Read-only runtime view of installed extensions.
 */
public interface ExtensionRegistry {
    public val installed: List<ExtensionMetadata>
    public fun metadata(id: String): ExtensionMetadata?
    public fun lifecycle(id: String): ExtensionLifecycle?
    public fun health(id: String): ExtensionHealth?
    public fun capabilities(id: String): ExtensionCapabilities?
    public fun isEnabled(id: String): Boolean
}
