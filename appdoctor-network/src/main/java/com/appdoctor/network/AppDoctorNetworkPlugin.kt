package com.appdoctor.network

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.appdoctor.core.AppDoctor
import com.appdoctor.core.AppDoctorConfig
import com.appdoctor.core.ids.PluginIds
import com.appdoctor.core.metric.Metric
import com.appdoctor.core.metric.MetricCollector
import com.appdoctor.core.metric.MetricCollectorProvider
import com.appdoctor.core.plugin.PluginContext
import com.appdoctor.network.metric.NetworkMetricCollector
import com.appdoctor.network.model.NetworkRequestRecord
import com.appdoctor.network.okhttp.AppDoctorNetworkInterceptor
import com.appdoctor.network.repository.InMemoryNetworkRequestRepository
import com.appdoctor.network.repository.NetworkRequestRepository
import com.appdoctor.network.ui.NetworkTabScreen
import com.appdoctor.ui.dashboard.plugin.DashboardTabPlugin
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * AppDoctor Phase 2 network inspector plugin.
 *
 * Add this module to your app, then add [createInterceptor] to your OkHttp client(s).
 */
public class AppDoctorNetworkPlugin(
    private val config: AppDoctorConfig = AppDoctorConfig(),
) : DashboardTabPlugin, MetricCollectorProvider {

    private val captureEnabled = AtomicBoolean(false)
    private val requestId = AtomicLong(0L)

    private val repository: NetworkRequestRepository =
        InMemoryNetworkRequestRepository(maxRequests = config.maxRequests)

    override val id: String = NETWORK_PLUGIN_ID
    override val title: String = "Network Inspector"
    override val tabKey: String = "network"
    override val tabTitle: String = "Network"

    @Volatile
    private var metricCollector: NetworkMetricCollector? = null

    /** Read-only latest-first list of captured requests. */
    public val requests: StateFlow<List<NetworkRequestRecord>> get() = repository.requests

    /** Collectors this plugin contributes to the [com.appdoctor.core.metric.CollectorRegistry]. */
    override val collectors: List<MetricCollector<Metric>>
        get() = listOfNotNull(metricCollector)

    override fun onInstall(context: PluginContext) {
        metricCollector = NetworkMetricCollector(repository, context.scope)
    }

    override fun onEnable(): Unit {
        captureEnabled.set(true)
    }

    override fun onDisable(): Unit {
        captureEnabled.set(false)
    }

    /**
     * Creates an interceptor tied to this plugin's in-memory request store.
     */
    public fun createInterceptor(): AppDoctorNetworkInterceptor = AppDoctorNetworkInterceptor(
        repository = repository,
        captureRequestBody = config.captureRequestBody,
        captureResponseBody = config.captureResponseBody,
        maxCapturedBodyBytes = config.maxCapturedBodyBytes,
        enabled = captureEnabled,
        requestId = requestId,
    )

    /** Clears all captured requests. */
    public fun clear(): Unit = repository.clear()

    @Composable
    override fun DashboardTabContent(modifier: Modifier) {
        NetworkTabScreen(
            requestsFlow = requests,
            onClear = ::clear,
            modifier = modifier,
        )
    }

    public companion object {
        public const val NETWORK_PLUGIN_ID: String = PluginIds.NETWORK_INSPECTOR

        /**
         * Returns the currently installed network plugin instance, if present.
         */
        @JvmStatic
        public fun installed(): AppDoctorNetworkPlugin? =
            AppDoctor.plugin(NETWORK_PLUGIN_ID) as? AppDoctorNetworkPlugin
    }
}
