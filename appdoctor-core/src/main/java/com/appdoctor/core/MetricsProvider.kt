package com.appdoctor.core

import com.appdoctor.core.info.AppInfo
import com.appdoctor.core.info.DeviceInfo
import com.appdoctor.core.monitor.cpu.CpuInfo
import com.appdoctor.core.monitor.fps.FpsInfo
import com.appdoctor.core.monitor.memory.MemoryInfo
import kotlinx.coroutines.flow.StateFlow

/**
 * Read-only aggregate of everything AppDoctor measures.
 *
 * This is the single dependency the UI layer (or any consumer / plugin) needs in order
 * to render live diagnostics. Static values are plain snapshots; live values are
 * [StateFlow]s that only do work while collected.
 *
 * Obtain an instance via [AppDoctor.metrics].
 */
public interface MetricsProvider {

    /** Static device characteristics. */
    public val deviceInfo: DeviceInfo

    /** Static host-app identity and build metadata. */
    public val appInfo: AppInfo

    /** Live JVM/native memory usage. */
    public val memory: StateFlow<MemoryInfo>

    /** Live approximate process CPU usage. */
    public val cpu: StateFlow<CpuInfo>

    /** Live frame-rate statistics. */
    public val fps: StateFlow<FpsInfo>
}
