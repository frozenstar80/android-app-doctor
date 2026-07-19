package com.appdoctor.ai

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.appdoctor.ai.engine.AiCache
import com.appdoctor.ai.engine.AiEngine
import com.appdoctor.ai.engine.AiExportFormatter
import com.appdoctor.ai.engine.AiHistoryRepository
import com.appdoctor.ai.provider.CustomEndpointProvider
import com.appdoctor.ai.provider.GeminiProvider
import com.appdoctor.ai.provider.LocalModelProvider
import com.appdoctor.ai.provider.OpenAiProvider
import com.appdoctor.ai.sanitize.BuiltInReportSanitizers
import com.appdoctor.ai.sanitize.CompositeReportSanitizer
import com.appdoctor.ai.sanitize.ReportSanitizer
import com.appdoctor.ai.ui.AiTabScreen
import com.appdoctor.core.AppDoctor
import com.appdoctor.core.AppDoctorConfig
import com.appdoctor.core.plugin.PluginContext
import com.appdoctor.session.AppDoctorSessionPlugin
import com.appdoctor.session.model.SessionReport
import com.appdoctor.ui.dashboard.plugin.DashboardTabPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

public data class AiUiState(
    val loading: Boolean = false,
    val latest: AiResponse? = null,
    val history: List<AiResponse> = emptyList(),
    val message: String? = null,
)

public class AppDoctorAiPlugin(
    private val appDoctorConfig: AppDoctorConfig = AppDoctorConfig(),
    private val providerOverride: AiProvider? = null,
    private val extraSanitizers: List<ReportSanitizer> = emptyList(),
) : DashboardTabPlugin {
    override val id: String = AI_PLUGIN_ID
    override val title: String = "AI Analysis"
    override val tabKey: String = "ai-analysis"
    override val tabTitle: String = "AI Analysis"

    private val configuration: AiConfiguration = AiConfiguration.from(appDoctorConfig)
    private val cache = AiCache(configuration.cacheSize)
    private val history = AiHistoryRepository(configuration.cacheSize)
    private val formatter = AiExportFormatter()
    private val state = MutableStateFlow(AiUiState())
    private val sanitizer = CompositeReportSanitizer(BuiltInReportSanitizers.defaults() + extraSanitizers)

    @Volatile
    private var engine: AiEngine? = null

    @Volatile
    private var scope: CoroutineScope? = null

    public val uiState: StateFlow<AiUiState> = state.asStateFlow()

    override fun onInstall(context: PluginContext) {
        scope = context.scope
        engine = AiEngine(
            configuration = configuration,
            providerResolver = { providerOverride ?: resolveProvider() },
            cache = cache,
            historyRepository = history,
            sanitizer = sanitizer,
        )
    }

    public fun generateAnalysis(forceRefresh: Boolean = false) {
        val engine = engine ?: return
        val scope = scope ?: CoroutineScope(Dispatchers.Default)
        state.value = state.value.copy(loading = true, message = null)
        scope.launch {
            val report = latestReport()
            if (report == null) {
                state.value = state.value.copy(
                    loading = false,
                    message = "Session Reports module is not active. Enable AppDoctorConfig(enableSessionReports = true).",
                )
                return@launch
            }
            val response = engine.analyze(report, forceRefresh)
            val message = when {
                response.error?.type == AiErrorType.CONFIGURATION && response.error.message == "No AI provider configured." ->
                    "No AI provider configured."
                response.error != null -> response.error.message
                response.fromCache -> "Loaded cached analysis."
                else -> "Analysis generated."
            }
            state.value = AiUiState(
                loading = false,
                latest = response,
                history = engine.history(),
                message = message,
            )
        }
    }

    public fun refresh(): Unit = generateAnalysis(forceRefresh = true)

    public fun exportMarkdown(): String? = state.value.latest?.let(formatter::toMarkdown)

    public fun exportJson(): String? = state.value.latest?.let(formatter::toJson)

    @Composable
    override fun DashboardTabContent(modifier: Modifier) {
        AiTabScreen(
            stateFlow = uiState,
            onGenerate = { generateAnalysis(forceRefresh = false) },
            onRefresh = ::refresh,
            onExportMarkdown = ::exportMarkdown,
            onExportJson = ::exportJson,
            modifier = modifier,
        )
    }

    private fun latestReport(): SessionReport? {
        val sessionPlugin = AppDoctor.plugin(AppDoctorSessionPlugin.SESSION_PLUGIN_ID) ?: return null
        val generate = sessionPlugin.javaClass.methods.firstOrNull {
            it.name == "generate" && it.parameterCount == 0
        } ?: return null
        return runCatching { generate.invoke(sessionPlugin) as? SessionReport }.getOrNull()
    }

    private fun resolveProvider(): AiProvider? = when (configuration.provider) {
        "openai" -> OpenAiProvider(configuration)
        "gemini" -> GeminiProvider(configuration)
        "local" -> LocalModelProvider()
        "custom" -> CustomEndpointProvider(configuration)
        else -> null
    }

    public companion object {
        public const val AI_PLUGIN_ID: String = "ai-analysis"
    }
}
