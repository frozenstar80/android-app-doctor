package com.appdoctor.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.appdoctor.core.MetricsProvider
import com.appdoctor.core.info.AppInfo
import com.appdoctor.core.info.DeviceInfo
import com.appdoctor.core.monitor.cpu.CpuInfo
import com.appdoctor.core.monitor.fps.FpsInfo
import com.appdoctor.core.monitor.memory.MemoryInfo
import kotlinx.coroutines.flow.StateFlow

/**
 * Exposes the read-only [MetricsProvider] to the dashboard as lifecycle-friendly state.
 *
 * The live values are already hot [StateFlow]s from the core engine that only run while
 * collected, so this ViewModel simply re-exposes them; it exists mainly to survive
 * configuration changes and to keep the composables free of any direct dependency on the
 * core singleton (Dependency Injection friendly — the [MetricsProvider] is passed in).
 */
internal class DashboardViewModel(
    private val metrics: MetricsProvider,
) : ViewModel() {

    val deviceInfo: DeviceInfo get() = metrics.deviceInfo
    val appInfo: AppInfo get() = metrics.appInfo

    val memory: StateFlow<MemoryInfo> get() = metrics.memory
    val cpu: StateFlow<CpuInfo> get() = metrics.cpu
    val fps: StateFlow<FpsInfo> get() = metrics.fps

    companion object {
        /** Builds a [ViewModelProvider.Factory] that injects the given [metrics]. */
        fun factory(metrics: MetricsProvider): ViewModelProvider.Factory = viewModelFactory {
            initializer { DashboardViewModel(metrics) }
        }
    }
}
