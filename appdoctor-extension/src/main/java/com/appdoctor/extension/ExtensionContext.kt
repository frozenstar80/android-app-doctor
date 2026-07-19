package com.appdoctor.extension

/**
 * Host context passed to extensions during installation/initialization.
 *
 * The host can expose additional objects via [service].
 */
public interface ExtensionContext {
    public val applicationContext: Any
    public val sdkVersion: ExtensionVersion
    public val configuration: ExtensionConfiguration
    public val logger: ExtensionLogger
    public fun <T : Any> service(key: String): T?
}
