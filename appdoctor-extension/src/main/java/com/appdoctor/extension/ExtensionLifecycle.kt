package com.appdoctor.extension

/**
 * Deterministic lifecycle states for an installed extension.
 */
public enum class ExtensionLifecycle {
    CREATED,
    INSTALLED,
    INITIALIZED,
    ENABLED,
    DISABLED,
    UNLOADED,
    DESTROYED,
    FAILED,
}
