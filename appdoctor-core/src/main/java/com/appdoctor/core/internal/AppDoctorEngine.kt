package com.appdoctor.core.internal

import android.app.Application
import com.appdoctor.core.AppDoctorConfig
import com.appdoctor.core.MetricsProvider
import com.appdoctor.core.info.AppInfo
import com.appdoctor.core.info.AppInfoProvider
import com.appdoctor.core.info.DeviceInfo
import com.appdoctor.core.info.DeviceInfoProvider
import com.appdoctor.core.internal.collector.DefaultCollectorRegistry
import com.appdoctor.core.internal.collector.MonitorCollector
import com.appdoctor.core.internal.lifecycle.ActivityTracker
import com.appdoctor.core.internal.util.Logger
import com.appdoctor.core.metric.CollectorRegistry
import com.appdoctor.core.metric.MetricCollectorProvider
import com.appdoctor.core.monitor.cpu.CpuInfo
import com.appdoctor.core.monitor.cpu.CpuMonitor
import com.appdoctor.core.monitor.fps.FpsInfo
import com.appdoctor.core.monitor.fps.FpsMonitor
import com.appdoctor.core.monitor.memory.MemoryInfo
import com.appdoctor.core.monitor.memory.MemoryMonitor
import com.appdoctor.core.overlay.AppDoctorOverlay
import com.appdoctor.core.overlay.OverlayFactory
import com.appdoctor.core.plugin.AppDoctorPlugin
import com.appdoctor.core.plugin.AppDoctorPluginFactory
import com.appdoctor.core.plugin.PluginContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.ServiceLoader
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The heart of AppDoctor: owns the monitors, the activity/overlay coordination and the
 * plugin registry, and implements [MetricsProvider].
 *
 * Constructed only when AppDoctor is actually active (debug build or explicit opt-in), so
 * in release builds nothing here is ever instantiated.
 *
 * Not part of the public API.
 */
internal class AppDoctorEngine(
    private val application: Application,
    private val config: AppDoctorConfig,
) : MetricsProvider {

    /** Background scope for polling monitors and plugin work. */
    private val engineScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Main-thread scope for the Choreographer FPS monitor and overlay reconciliation. */
    private val mainScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // ---- MetricsProvider -----------------------------------------------------------

    override val deviceInfo: DeviceInfo = DeviceInfoProvider.capture()

    override val appInfo: AppInfo =
        runCatching { AppInfoProvider(application).capture() }
            .getOrElse {
                Logger.w("Failed to read app info; using placeholder.", it)
                AppInfo.Unknown
            }

    private val memoryMonitor = MemoryMonitor(engineScope, config.pollingIntervalMillis)
    private val cpuMonitor = CpuMonitor(engineScope, config.pollingIntervalMillis)
    private val fpsMonitor = FpsMonitor(mainScope)

    override val memory: kotlinx.coroutines.flow.StateFlow<MemoryInfo> get() = memoryMonitor.data
    override val cpu: kotlinx.coroutines.flow.StateFlow<CpuInfo> get() = cpuMonitor.data
    override val fps: kotlinx.coroutines.flow.StateFlow<FpsInfo> get() = fpsMonitor.data

    // ---- Overlay + lifecycle -------------------------------------------------------

    private val overlay: AppDoctorOverlay? = resolveOverlay()
    private val coordinator = OverlayCoordinator(overlay)
    private val activityTracker = ActivityTracker(coordinator)

    // ---- Enabled state + plugins ---------------------------------------------------

    private val stateLock = Any()

    @Volatile
    var isEnabled: Boolean = false
        private set

    private val plugins = CopyOnWriteArrayList<AppDoctorPlugin>()

    private val collectorRegistry = DefaultCollectorRegistry()

    /** Read-only registry of all metric collectors (core monitors + plugin-provided). */
    val collectors: CollectorRegistry get() = collectorRegistry

    private val pluginContext: PluginContext = object : PluginContext {
        override val application: Application get() = this@AppDoctorEngine.application
        override val metrics: MetricsProvider get() = this@AppDoctorEngine
        override val scope: CoroutineScope get() = engineScope
    }

    // ---- Lifecycle -----------------------------------------------------------------

    /** Registers lifecycle callbacks, installs configured plugins and applies start state. */
    fun start() {
        application.registerActivityLifecycleCallbacks(activityTracker)
        registerCoreCollectors()
        config.plugins.forEach(::registerPlugin)
        loadBuiltinPlugins().forEach(::registerPlugin)
        if (config.startEnabled) enable()
        Logger.i("Installed (overlay=${overlay != null}, startEnabled=${config.startEnabled}).")
    }

    fun enable() {
        synchronized(stateLock) {
            if (isEnabled) return
            isEnabled = true
        }
        reconcileOverlay(enabled = true)
        plugins.forEach { runPluginCallback { it.onEnable() } }
    }

    fun disable() {
        synchronized(stateLock) {
            if (!isEnabled) return
            isEnabled = false
        }
        reconcileOverlay(enabled = false)
        plugins.forEach { runPluginCallback { it.onDisable() } }
    }

    fun registerPlugin(plugin: AppDoctorPlugin) {
        if (plugins.any { it.id == plugin.id }) return
        plugins.add(plugin)
        runPluginCallback { plugin.onInstall(pluginContext) }
        if (plugin is MetricCollectorProvider) {
            runPluginCallback { plugin.collectors.forEach(collectorRegistry::register) }
        }
        if (isEnabled) runPluginCallback { plugin.onEnable() }
    }

    fun pluginsSnapshot(): List<AppDoctorPlugin> = plugins.toList()

    /** Tear everything down. Reserved for a future public `uninstall()`. */
    fun shutdown() {
        application.unregisterActivityLifecycleCallbacks(activityTracker)
        mainScope.launch { coordinator.shutdown() }
        collectorRegistry.clear()
        engineScope.cancel()
        mainScope.cancel()
    }

    // ---- Internals -----------------------------------------------------------------

    private fun registerCoreCollectors() {
        collectorRegistry.register(MonitorCollector(MEMORY_ID, memoryMonitor))
        collectorRegistry.register(MonitorCollector(CPU_ID, cpuMonitor))
        collectorRegistry.register(MonitorCollector(FPS_ID, fpsMonitor))
    }

    private fun reconcileOverlay(enabled: Boolean) {
        // Overlay/WindowManager work must happen on the main thread.
        mainScope.launch {
            coordinator.setEnabled(enabled, activityTracker.currentActivity)
        }
    }

    private inline fun runPluginCallback(block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            Logger.w("Plugin callback threw; continuing.", t)
        }
    }

    private fun resolveOverlay(): AppDoctorOverlay? {
        val factory = config.overlayFactory ?: loadDefaultOverlayFactory()
        if (factory == null) {
            Logger.i("No overlay available; running headless (metrics only).")
            return null
        }
        return try {
            factory.create(application)
        } catch (t: Throwable) {
            Logger.w("Overlay creation failed; running headless.", t)
            null
        }
    }

    private fun loadDefaultOverlayFactory(): OverlayFactory? = try {
        ServiceLoader.load(OverlayFactory::class.java, OverlayFactory::class.java.classLoader)
            .firstOrNull()
            .also { if (it == null) Logger.i("No overlay factory on classpath; running headless.") }
    } catch (t: Throwable) {
        Logger.w("Failed to load default overlay factory.", t)
        null
    }

    private fun loadBuiltinPlugins(): List<AppDoctorPlugin> = try {
        ServiceLoader.load(AppDoctorPluginFactory::class.java, AppDoctorPluginFactory::class.java.classLoader)
            .toList()
            .mapNotNull(::createBuiltinPlugin)
    } catch (t: Throwable) {
        Logger.w("Built-in plugin discovery failed.", t)
        emptyList()
    }

    private fun createBuiltinPlugin(factory: AppDoctorPluginFactory): AppDoctorPlugin? = try {
        factory.create(config)
    } catch (t: Throwable) {
        Logger.w("Failed to create built-in plugin from ${factory::class.java.name}.", t)
        null
    }

    private companion object {
        private const val MEMORY_ID = "memory"
        private const val CPU_ID = "cpu"
        private const val FPS_ID = "fps"
    }
}
