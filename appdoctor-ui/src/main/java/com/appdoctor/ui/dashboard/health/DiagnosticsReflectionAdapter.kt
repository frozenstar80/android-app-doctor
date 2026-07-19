package com.appdoctor.ui.dashboard.health

import com.appdoctor.core.AppDoctor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal data class HealthUiModel(
    val overallScore: Int,
    val performanceScore: Int,
    val memoryScore: Int,
    val networkScore: Int,
    val databaseScore: Int,
    val composeScore: Int,
)

internal data class RecommendationUiModel(
    val problem: String,
    val reason: String,
    val recommendation: String,
    val expectedImpact: String,
)

internal data class IssueUiModel(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val severity: String,
    val confidence: Int,
    val timestampMillis: Long,
    val collectorIds: List<String>,
    val recommendation: RecommendationUiModel,
    val documentationLink: String?,
    val status: String,
)

internal class DiagnosticsReflectionAdapter {

    private val diagnosticsPlugin: Any? get() = AppDoctor.plugin(DIAGNOSTICS_PLUGIN_ID)

    val healthFlow: StateFlow<Any?>
        get() = diagnosticsPlugin.readFlow("healthReport", FALLBACK_HEALTH_FLOW)

    val issuesFlow: StateFlow<List<Any>>
        get() = diagnosticsPlugin.readFlow("issues", FALLBACK_ISSUES_FLOW)

    fun dismissIssue(id: String) {
        val plugin = diagnosticsPlugin ?: return
        val method = plugin.javaClass.methods.firstOrNull {
            it.name == "dismissIssue" && it.parameterCount == 1 && it.parameterTypes[0] == String::class.java
        } ?: return
        runCatching { method.invoke(plugin, id) }
    }

    fun parseHealth(raw: Any?): HealthUiModel? {
        val source = raw ?: return null
        val overall = source.readInt("overallScore") ?: return null
        val performance = source.readInt("performanceScore") ?: return null
        val memory = source.readInt("memoryScore") ?: return null
        val network = source.readInt("networkScore") ?: return null
        val database = source.readInt("databaseScore") ?: return null
        val compose = source.readInt("composeScore") ?: return null
        return HealthUiModel(
            overallScore = overall,
            performanceScore = performance,
            memoryScore = memory,
            networkScore = network,
            databaseScore = database,
            composeScore = compose,
        )
    }

    fun parseIssues(raw: List<Any>): List<IssueUiModel> = raw.mapNotNull { parseIssue(it) }

    private fun parseIssue(source: Any): IssueUiModel? {
        val recommendation = source.readAny("recommendation") ?: return null
        return IssueUiModel(
            id = source.readString("id") ?: return null,
            title = source.readString("title") ?: return null,
            description = source.readString("description") ?: return null,
            category = source.readEnumName("category") ?: "UNKNOWN",
            severity = source.readEnumName("severity") ?: "INFO",
            confidence = source.readInt("confidence") ?: 0,
            timestampMillis = source.readLong("timestampMillis") ?: 0L,
            collectorIds = source.readStringList("collectorIds"),
            recommendation = RecommendationUiModel(
                problem = recommendation.readString("problem") ?: "",
                reason = recommendation.readString("reason") ?: "",
                recommendation = recommendation.readString("recommendation") ?: "",
                expectedImpact = recommendation.readString("expectedImpact") ?: "",
            ),
            documentationLink = source.readString("documentationLink"),
            status = source.readEnumName("status") ?: "OPEN",
        )
    }

    companion object {
        private const val DIAGNOSTICS_PLUGIN_ID = "diagnostics"
        private val FALLBACK_HEALTH_FLOW = MutableStateFlow<Any?>(null)
        private val FALLBACK_ISSUES_FLOW = MutableStateFlow(emptyList<Any>())
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T> Any?.readFlow(property: String, fallback: StateFlow<T>): StateFlow<T> {
    val source = this ?: return fallback
    val getter = source.javaClass.methods.firstOrNull {
        it.name == "get${property.replaceFirstChar { ch -> ch.uppercaseChar() }}" && it.parameterCount == 0
    } ?: return fallback
    val flow = runCatching { getter.invoke(source) }.getOrNull() as? StateFlow<*>
    return flow as? StateFlow<T> ?: fallback
}

private fun Any.readAny(property: String): Any? {
    val getter = javaClass.methods.firstOrNull {
        it.name == "get${property.replaceFirstChar { ch -> ch.uppercaseChar() }}" && it.parameterCount == 0
    } ?: return null
    return runCatching { getter.invoke(this) }.getOrNull()
}

private fun Any.readString(property: String): String? = readAny(property) as? String

private fun Any.readInt(property: String): Int? = when (val value = readAny(property)) {
    is Int -> value
    is Long -> value.toInt()
    else -> null
}

private fun Any.readLong(property: String): Long? = when (val value = readAny(property)) {
    is Int -> value.toLong()
    is Long -> value
    else -> null
}

private fun Any.readEnumName(property: String): String? = readAny(property)?.toString()

private fun Any.readStringList(property: String): List<String> =
    (readAny(property) as? List<*>)?.mapNotNull { it as? String }.orEmpty()
