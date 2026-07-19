package com.appdoctor.core

import android.app.Application
import androidx.annotation.AnyThread
import com.appdoctor.core.internal.AppDoctorEngine
import com.appdoctor.core.internal.util.BuildTypeDetector
import com.appdoctor.core.internal.util.Logger
import com.appdoctor.core.metric.CollectorRegistry
import com.appdoctor.core.plugin.AppDoctorPlugin
import com.appdoctor.extension.Extension
import com.appdoctor.extension.ExtensionConfiguration
import com.appdoctor.extension.ExtensionMetadata
import com.appdoctor.extension.ExtensionRegistry

/**
 * AppDoctor — a zero-config, debug-only diagnostics overlay for Android.
 *
 * ### Usage
 * Call [install] once from your [Application]:
 * ```kotlin
 * class MyApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         AppDoctor.install(this)
 *     }
 * }
 * ```
 * A draggable floating button then appears on every activity; tapping it opens a live
 * dashboard (device/app info, memory, FPS and CPU).
 *
 * ### Release safety
 * By default AppDoctor does **absolutely nothing** in release builds: [install] returns
 * immediately when the host app is not debuggable, so no lifecycle callbacks, monitors or
 * overlay are ever created. (Override via [AppDoctorConfig.enabledInReleaseBuilds] if you
 * really need it in a non-debuggable flavor.) For maximum stripping, depend on the
 * artifacts with `debugImplementation` — see the README.
 *
 * ### Thread-safety
 * All members are safe to call from any thread. [install] is idempotent.
 */
public object AppDoctor {

    @Volatile
    private var engine: AppDoctorEngine? = null

    private val installLock = Any()

    /**
     * Installs AppDoctor. Safe to call multiple times — only the first effective call does
     * anything. No-op in release builds unless
     * [AppDoctorConfig.enabledInReleaseBuilds] is set.
     *
     * @param application your [Application] instance.
     * @param config optional configuration; defaults are fine for most apps.
     */
    @AnyThread
    @JvmStatic
    @JvmOverloads
    public fun install(application: Application, config: AppDoctorConfig = AppDoctorConfig()) {
        install(application = application, config = config, extensionConfiguration = ExtensionConfiguration())
    }

    /**
     * Installs AppDoctor with optional Extension SDK configuration.
     *
     * Existing behaviour remains unchanged when [extensionConfiguration.enableExtensions] is
     * `false` (default).
     */
    @AnyThread
    @JvmStatic
    public fun install(
        application: Application,
        config: AppDoctorConfig,
        extensionConfiguration: ExtensionConfiguration,
    ) {
        if (!shouldActivate(application, config)) {
            Logger.i("Skipping install: host is not debuggable and release override is off.")
            return
        }
        val created: AppDoctorEngine
        synchronized(installLock) {
            if (engine != null) return
            created = AppDoctorEngine(application, config, extensionConfiguration)
            engine = created
        }
        created.start()
    }

    /**
     * Enables the overlay and monitoring after an earlier [install]. No-op if AppDoctor is
     * not installed or already enabled.
     */
    @AnyThread
    @JvmStatic
    public fun enable() {
        engine?.enable()
    }

    /**
     * Disables the overlay and monitoring (the install remains; can be re-[enable]d). No-op
     * if AppDoctor is not installed or already disabled.
     */
    @AnyThread
    @JvmStatic
    public fun disable() {
        engine?.disable()
    }

    /** `true` if AppDoctor is installed and currently enabled. */
    @AnyThread
    @JvmStatic
    public fun isEnabled(): Boolean = engine?.isEnabled == true

    /** `true` if [install] has taken effect (i.e. AppDoctor is active in this process). */
    @AnyThread
    @JvmStatic
    public fun isInstalled(): Boolean = engine != null

    /**
     * Registers an [AppDoctorPlugin] at runtime (extension point for future inspectors).
     * No-op if AppDoctor is not installed.
     */
    @AnyThread
    @JvmStatic
    public fun registerPlugin(plugin: AppDoctorPlugin) {
        engine?.registerPlugin(plugin)
    }

    /**
     * Registers and installs an [Extension] using deterministic lifecycle semantics.
     */
    @AnyThread
    @JvmStatic
    public fun registerExtension(extension: Extension) {
        engine?.registerExtension(extension)
    }

    /**
     * Enables a previously installed extension by id.
     */
    @AnyThread
    @JvmStatic
    public fun enableExtension(id: String): Boolean = engine?.enableExtension(id) == true

    /**
     * Disables a previously installed extension by id.
     */
    @AnyThread
    @JvmStatic
    public fun disableExtension(id: String): Boolean = engine?.disableExtension(id) == true

    /**
     * Snapshot of currently registered plugins.
     *
     * Returns an empty list when AppDoctor is not installed.
     */
    @get:AnyThread
    @get:JvmStatic
    public val plugins: List<AppDoctorPlugin>
        get() = engine?.pluginsSnapshot().orEmpty()

    /**
     * Returns a registered plugin by its stable [AppDoctorPlugin.id], or `null` if missing.
     */
    @AnyThread
    @JvmStatic
    public fun plugin(id: String): AppDoctorPlugin? = plugins.firstOrNull { it.id == id }

    /**
     * Live metrics, or `null` if AppDoctor is not active in this build. The UI layer and
     * plugins read diagnostics through this.
     */
    @get:AnyThread
    @get:JvmStatic
    public val metrics: MetricsProvider?
        get() = engine

    /**
     * Read-only registry of all metric collectors (core monitors plus any contributed by
     * plugins), or `null` if AppDoctor is not active in this build. Intended for future
     * Diagnostics / Timeline / Session Reports; the UI may also consume it.
     */
    @get:AnyThread
    @get:JvmStatic
    public val collectors: CollectorRegistry?
        get() = engine?.collectors

    /**
     * Read-only Extension SDK registry, or `null` when AppDoctor is inactive.
     */
    @get:AnyThread
    @get:JvmStatic
    public val extensionRegistry: ExtensionRegistry?
        get() = engine?.extensions

    /**
     * Snapshot of installed extension metadata.
     */
    @get:AnyThread
    @get:JvmStatic
    public val installedExtensions: List<ExtensionMetadata>
        get() = extensionRegistry?.installed.orEmpty()

    private fun shouldActivate(application: Application, config: AppDoctorConfig): Boolean =
        config.enabledInReleaseBuilds || BuildTypeDetector.isDebuggable(application)
}
