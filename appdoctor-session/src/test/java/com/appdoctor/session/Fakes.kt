package com.appdoctor.session

import com.appdoctor.core.MetricsProvider
import com.appdoctor.core.info.AppInfo
import com.appdoctor.core.info.BuildType
import com.appdoctor.core.info.DeviceInfo
import com.appdoctor.core.metric.CollectorRegistry
import com.appdoctor.core.metric.Metric
import com.appdoctor.core.metric.MetricCollector
import com.appdoctor.core.monitor.cpu.CpuInfo
import com.appdoctor.core.monitor.fps.FpsInfo
import com.appdoctor.core.monitor.memory.MemoryInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeCollector<T : Metric>(
    override val id: String,
    initial: T,
) : MetricCollector<T> {
    private val state = MutableStateFlow(initial)
    override val data: StateFlow<T> = state
    override fun snapshot(): T = state.value
    fun emit(value: T) {
        state.value = value
    }
}

internal class FakeRegistry(
    override val collectors: List<MetricCollector<Metric>>,
) : CollectorRegistry {
    override fun collector(id: String): MetricCollector<Metric>? = collectors.firstOrNull { it.id == id }
}

internal class FakeMetricsProvider : MetricsProvider {
    override val deviceInfo: DeviceInfo = DeviceInfo(
        androidVersion = "15",
        apiLevel = 35,
        manufacturer = "Google",
        model = "Pixel",
        brand = "google",
        device = "shiba",
        supportedAbis = listOf("arm64-v8a"),
    )
    override val appInfo: AppInfo = AppInfo(
        packageName = "com.example.app",
        versionName = "1.0.0",
        versionCode = 42L,
        buildType = BuildType.DEBUG,
        minSdk = 24,
        targetSdk = 37,
    )
    override val memory: StateFlow<MemoryInfo> = MutableStateFlow(MemoryInfo(1, 10, 9, 0))
    override val cpu: StateFlow<CpuInfo> = MutableStateFlow(CpuInfo(15f, 8))
    override val fps: StateFlow<FpsInfo> = MutableStateFlow(FpsInfo(60f, 58f, 45f))
}

internal data class FakeNetworkRequest(
    val id: Long,
    val responseTimeMillis: Long,
    val success: Boolean,
)

internal data class FakeNetworkMetric(
    val requests: List<FakeNetworkRequest>,
) : Metric

internal data class FakeDatabaseQuery(
    val id: Long,
    val durationNanos: Long,
    val success: Boolean,
)

internal data class FakeDatabaseMetric(
    val queries: List<FakeDatabaseQuery>,
) : Metric
