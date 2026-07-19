package com.appdoctor.extension

/**
 * ServiceLoader/DI factory contract for constructing an [Extension].
 */
public interface ExtensionFactory {
    public val id: String
    public val priority: Int
        get() = 0
    public fun create(context: ExtensionContext): Extension?
}
