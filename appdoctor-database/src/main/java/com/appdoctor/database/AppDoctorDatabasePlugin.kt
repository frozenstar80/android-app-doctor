package com.appdoctor.database

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.appdoctor.core.AppDoctorConfig
import com.appdoctor.core.ids.PluginIds
import com.appdoctor.core.metric.Metric
import com.appdoctor.core.metric.MetricCollector
import com.appdoctor.core.metric.MetricCollectorProvider
import com.appdoctor.core.plugin.PluginContext
import com.appdoctor.database.analytics.DatabaseAnalytics
import com.appdoctor.database.analytics.DatabaseAnalyticsEngine
import com.appdoctor.database.metric.DatabaseMetricCollector
import com.appdoctor.database.model.DatabaseQuery
import com.appdoctor.database.recorder.DefaultDatabaseQueryRecorder
import com.appdoctor.database.repository.DatabaseQueryRepository
import com.appdoctor.database.repository.InMemoryDatabaseQueryRepository
import com.appdoctor.database.ui.DatabaseTabScreen
import com.appdoctor.ui.dashboard.plugin.DashboardTabPlugin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AppDoctor Phase 3 database inspector plugin.
 *
 * Add this module, then call `.enableAppDoctor()` on your `RoomDatabase.Builder`. Queries
 * appear live under the Database tab; analytics are computed only when
 * [AppDoctorConfig.enableDatabaseAnalytics] is set.
 */
public class AppDoctorDatabasePlugin(
    private val config: AppDoctorConfig = AppDoctorConfig(),
) : DashboardTabPlugin, MetricCollectorProvider {

    private val recording = AtomicBoolean(false)

    private val repository: DatabaseQueryRepository =
        InMemoryDatabaseQueryRepository(maxQueries = config.maxDatabaseQueries)

    private val recorder = DefaultDatabaseQueryRecorder(repository, recording)

    override val id: String = DATABASE_PLUGIN_ID
    override val title: String = "Database Inspector"
    override val tabKey: String = "database"
    override val tabTitle: String = "Database"

    /** Whether aggregated analytics are computed and shown. */
    public val analyticsEnabled: Boolean = config.enableDatabaseAnalytics

    @Volatile
    private var metricCollector: DatabaseMetricCollector? = null

    @Volatile
    private var analyticsEngine: DatabaseAnalyticsEngine? = null

    private val noAnalytics = MutableStateFlow(DatabaseAnalytics.Empty)

    /** Read-only latest-first list of captured queries. */
    public val queries: StateFlow<List<DatabaseQuery>> get() = repository.queries

    /** Live analytics, or a constant empty value when analytics are disabled. */
    public val analytics: StateFlow<DatabaseAnalytics> get() = analyticsEngine?.analytics ?: noAnalytics

    override val collectors: List<MetricCollector<Metric>>
        get() = listOfNotNull(metricCollector)

    override fun onInstall(context: PluginContext) {
        metricCollector = DatabaseMetricCollector(repository, context.scope)
        if (config.enableDatabaseAnalytics) {
            analyticsEngine = DatabaseAnalyticsEngine(
                repository = repository,
                slowThresholdMillis = config.slowQueryThresholdMillis,
                scope = context.scope,
            )
        }
        AppDoctorDatabase.attach(recorder)
    }

    override fun onEnable() {
        recording.set(true)
    }

    override fun onDisable() {
        recording.set(false)
    }

    /** Clears all captured queries. */
    public fun clear(): Unit = repository.clear()

    @Composable
    override fun DashboardTabContent(modifier: Modifier) {
        DatabaseTabScreen(
            queriesFlow = queries,
            analyticsFlow = analytics,
            analyticsEnabled = analyticsEnabled,
            slowThresholdMillis = config.slowQueryThresholdMillis,
            onClear = ::clear,
            modifier = modifier,
        )
    }

    public companion object {
        public const val DATABASE_PLUGIN_ID: String = PluginIds.DATABASE_INSPECTOR
    }
}
