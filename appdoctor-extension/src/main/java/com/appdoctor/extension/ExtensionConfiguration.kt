package com.appdoctor.extension

/**
 * Extension loading and safety controls.
 */
public data class ExtensionConfiguration(
    public val enableExtensions: Boolean = false,
    public val allowThirdPartyExtensions: Boolean = false,
    public val strictCompatibilityChecking: Boolean = true,
    public val extensionLoadingStrategy: LoadingStrategy = LoadingStrategy.AUTOMATIC,
    public val dependencyInjectedFactories: List<ExtensionFactory> = emptyList(),
    public val dependencyInjectedExtensions: List<Extension> = emptyList(),
) {
    public enum class LoadingStrategy {
        AUTOMATIC,
        SERVICE_LOADER,
        MANUAL,
        DEPENDENCY_INJECTION,
        PACKAGE_MANAGER,
    }
}
