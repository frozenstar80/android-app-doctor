package com.appdoctor.diagnostics

import com.appdoctor.core.AppDoctorConfig
import com.appdoctor.core.plugin.AppDoctorPlugin
import com.appdoctor.core.plugin.PluginContext
import com.appdoctor.diagnostics.engine.ConfidenceCalculator
import com.appdoctor.diagnostics.engine.DiagnosticsEngine
import com.appdoctor.diagnostics.engine.HealthEngine
import com.appdoctor.diagnostics.engine.IssueDetector
import com.appdoctor.diagnostics.engine.IssueRepository
import com.appdoctor.diagnostics.engine.RecommendationProvider
import com.appdoctor.diagnostics.engine.rules.ExcessiveNetworkRequestVolumeRule
import com.appdoctor.diagnostics.engine.rules.FpsDropWithSlowDatabaseRule
import com.appdoctor.diagnostics.engine.rules.FpsDropWithSlowNetworkRule
import com.appdoctor.diagnostics.engine.rules.HighAverageNetworkLatencyRule
import com.appdoctor.diagnostics.engine.rules.HighComposeRecompositionRateRule
import com.appdoctor.diagnostics.engine.rules.HighDatabaseFailureRateRule
import com.appdoctor.diagnostics.engine.rules.HighDatabaseQueryFrequencyRule
import com.appdoctor.diagnostics.engine.rules.HighMemoryWithDatabaseActivityRule
import com.appdoctor.diagnostics.engine.rules.RapidMemoryGrowthRule
import com.appdoctor.diagnostics.engine.rules.RepeatedNetworkFailuresRule
import com.appdoctor.diagnostics.engine.rules.SlowDatabaseQueriesRule
import com.appdoctor.diagnostics.engine.rules.SustainedHighMemoryUsageRule
import com.appdoctor.diagnostics.model.DiagnosticIssue
import com.appdoctor.diagnostics.model.HealthReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Phase 5 diagnostics plugin. Consumes metrics from [PluginContext.collectors] asynchronously.
 */
public class AppDoctorDiagnosticsPlugin(
    private val config: AppDoctorConfig = AppDoctorConfig(),
) : AppDoctorPlugin {

    override val id: String = DIAGNOSTICS_PLUGIN_ID
    override val title: String = "Diagnostics"

    private val emptyHealth = MutableStateFlow(HealthReport.Empty)
    private val emptyIssues = MutableStateFlow<List<DiagnosticIssue>>(emptyList())

    @Volatile
    private var engine: DiagnosticsEngine? = null

    public val healthReport: StateFlow<HealthReport>
        get() = engine?.healthReport ?: emptyHealth

    public val issues: StateFlow<List<DiagnosticIssue>>
        get() = engine?.issues ?: emptyIssues

    override fun onInstall(context: PluginContext) {
        val issueRepository = IssueRepository(maximumIssueHistory = config.maximumIssueHistory)
        val issueDetector = IssueDetector(
            rules = listOf(
                SustainedHighMemoryUsageRule(),
                RapidMemoryGrowthRule(),
                HighAverageNetworkLatencyRule(),
                RepeatedNetworkFailuresRule(),
                ExcessiveNetworkRequestVolumeRule(),
                SlowDatabaseQueriesRule(),
                HighDatabaseQueryFrequencyRule(),
                HighDatabaseFailureRateRule(),
                HighComposeRecompositionRateRule(),
                FpsDropWithSlowDatabaseRule(),
                FpsDropWithSlowNetworkRule(),
                HighMemoryWithDatabaseActivityRule(),
            ),
            recommendationProvider = RecommendationProvider(),
            confidenceCalculator = ConfidenceCalculator(),
            minimumConfidence = config.minimumConfidence.coerceIn(0, 100),
        )
        engine = DiagnosticsEngine(
            collectorRegistry = context.collectors,
            issueDetector = issueDetector,
            issueRepository = issueRepository,
            healthEngine = HealthEngine(),
            scope = context.scope,
            analysisIntervalMillis = config.analysisInterval,
        )
    }

    override fun onEnable() {
        engine?.start()
    }

    override fun onDisable() {
        engine?.stop()
    }

    public fun dismissIssue(issueId: String): Unit = engine?.dismissIssue(issueId) ?: Unit

    public companion object {
        public const val DIAGNOSTICS_PLUGIN_ID: String = "diagnostics"
    }
}
