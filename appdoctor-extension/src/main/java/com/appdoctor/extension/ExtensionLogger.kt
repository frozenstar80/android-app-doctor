package com.appdoctor.extension

/**
 * Logging contract exposed to extensions.
 */
public interface ExtensionLogger {
    public fun debug(message: String)
    public fun info(message: String)
    public fun warn(message: String, throwable: Throwable? = null)
    public fun error(message: String, throwable: Throwable? = null)
}
