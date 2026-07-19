package com.appdoctor.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.appdoctor.compose.analytics.ComposeAnalytics
import com.appdoctor.compose.analytics.ComposeAnalyticsEngine
import com.appdoctor.compose.internal.runtime.ComposeRuntimeCollectorEngine
import com.appdoctor.compose.metric.ComposeMetricCollector
import com.appdoctor.compose.model.ComposeRuntimeSnapshot
import com.appdoctor.compose.model.TrackedComposable
import com.appdoctor.compose.tracking.ComposableTracker
import com.appdoctor.compose.tracking.InMemoryComposableTracker
import com.appdoctor.compose.ui.ComposeTabScreen
import com.appdoctor.core.AppDoctorConfig
import com.appdoctor.core.metric.Metric
import com.appdoctor.core.metric.MetricCollector
import com.appdoctor.core.metric.MetricCollectorProvider
import com.appdoctor.core.plugin.PluginContext
import com.appdoctor.ui.dashboard.plugin.DashboardTabPlugin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * AppDoctor Phase 4 Compose runtime inspector plugin.
 *
 * Add this module and live runtime metrics (recompositions, rate, burst durations, frames)
 * appear automatically under the **Compose** tab — no changes to your composables required.
 * Opt-in per-composable tracking and aggregate analytics are enabled through
 * [AppDoctorConfig]:
 *
 *  - [AppDoctorConfig.enableComposableTracking] — record composables that call
 *    `TrackRecompositions(name)`,
 *  - [AppDoctorConfig.enableComposeAnalytics] — compute the aggregate statistics tab.
 *
 * The plugin exposes its collector through [MetricCollectorProvider], so it lands in
 * `AppDoctor.collectors` automatically (stable id `"compose"`), and renders a tab through
 * [DashboardTabPlugin]. It is discovered via `ServiceLoader`; `appdoctor-core` is untouched.
 */
public class AppDoctorComposePlugin(
    private val config: AppDoctorConfig = AppDoctorConfig(),
) : DashboardTabPlugin, MetricCollectorProvider {

    override val id: String = COMPOSE_PLUGIN_ID
    override val title: String = "Compose Inspector"
    override val tabKey: String = "compose"
    override val tabTitle: String = "Compose"

    /** Whether opt-in per-composable tracking is active. */
    public val trackingEnabled: Boolean = config.enableComposableTracking

    /** Whether aggregate analytics are computed and shown. */
    public val analyticsEnabled: Boolean = config.enableComposeAnalytics

    // The tracker always exists (so the collector can read active/disposal counts as 0 when
    // tracking is off); it only becomes the process-wide sink when tracking is enabled.
    private val tracker: ComposableTracker =
        InMemoryComposableTracker(limit = config.trackedComposableLimit)

    @Volatile
    private var engine: ComposeRuntimeCollectorEngine? = null

    @Volatile
    private var metricCollector: ComposeMetricCollector? = null

    @Volatile
    private var analyticsEngine: ComposeAnalyticsEngine? = null

    private val noRuntime = MutableStateFlow(ComposeRuntimeSnapshot.Empty)
    private val noAnalytics = MutableStateFlow(ComposeAnalytics.Empty)

    /** Live runtime snapshot stream (idle-cost-zero; only sampled while observed). */
    public val runtime: StateFlow<ComposeRuntimeSnapshot>
        get() = engine?.data ?: noRuntime

    /** Live tracked-composable list, empty when tracking is disabled. */
    public val trackedComposables: StateFlow<List<TrackedComposable>>
        get() = tracker.tracked

    /** Live analytics, or a constant empty value when analytics are disabled. */
    public val analytics: StateFlow<ComposeAnalytics>
        get() = analyticsEngine?.analytics ?: noAnalytics

    override val collectors: List<MetricCollector<Metric>>
        get() = listOfNotNull(metricCollector)

    override fun onInstall(context: PluginContext) {
        val runtimeEngine = ComposeRuntimeCollectorEngine(scope = context.scope, tracker = tracker)
        engine = runtimeEngine
        metricCollector = ComposeMetricCollector(runtimeEngine)

        if (config.enableComposableTracking) {
            AppDoctorCompose.attach(tracker)
        }
        if (config.enableComposeAnalytics) {
            analyticsEngine = ComposeAnalyticsEngine(
                runtime = runtimeEngine.data,
                tracked = tracker.tracked,
                scope = context.scope,
            )
        }
    }

    override fun onEnable() {
        if (config.enableComposableTracking) tracker.setEnabled(true)
    }

    override fun onDisable() {
        if (config.enableComposableTracking) tracker.setEnabled(false)
    }

    /** Clears all tracked composables. */
    public fun clear(): Unit = tracker.clear()

    @Composable
    override fun DashboardTabContent(modifier: Modifier) {
        ComposeTabScreen(
            runtimeFlow = runtime,
            trackedFlow = trackedComposables,
            analyticsFlow = analytics,
            trackingEnabled = trackingEnabled,
            analyticsEnabled = analyticsEnabled,
            onClear = ::clear,
            modifier = modifier,
        )
    }

    public companion object {
        public const val COMPOSE_PLUGIN_ID: String = "compose-inspector"
    }
}
