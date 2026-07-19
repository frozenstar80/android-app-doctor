package com.appdoctor.ui.dashboard.timeline

import com.appdoctor.core.AppDoctor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal data class TimelineEventUiModel(
    val timestamp: Long,
    val sessionId: String,
    val source: String,
    val collectorId: String,
    val category: String,
    val title: String,
    val summary: String,
    val severity: String?,
    val relatedIssueId: String?,
    val metadata: Map<String, String>,
    val sourceMetric: String,
    val groupId: String?,
)

internal class TimelineReflectionAdapter {

    private val timelinePlugin: Any? get() = AppDoctor.plugin(TIMELINE_PLUGIN_ID)

    val eventsFlow: StateFlow<List<Any>>
        get() = timelinePlugin.readFlow("events", FALLBACK_EVENTS_FLOW)

    fun parseEvents(raw: List<Any>): List<TimelineEventUiModel> = raw.mapNotNull { event ->
        val metadataRaw = event.readAny("metadata") as? Map<*, *> ?: emptyMap<Any, Any>()
        TimelineEventUiModel(
            timestamp = event.readLong("timestamp") ?: 0L,
            sessionId = event.readString("sessionId") ?: "",
            source = event.readString("source") ?: "",
            collectorId = event.readString("collectorId") ?: "",
            category = event.readAny("category")?.toString() ?: "COLLECTOR",
            title = event.readString("title") ?: "",
            summary = event.readString("summary") ?: "",
            severity = event.readAny("severity")?.toString(),
            relatedIssueId = event.readString("relatedIssueId"),
            metadata = metadataRaw.entries.associate { (k, v) -> k.toString() to v.toString() },
            sourceMetric = event.readString("sourceMetric") ?: "",
            groupId = event.readString("groupId"),
        )
    }

    companion object {
        private const val TIMELINE_PLUGIN_ID = "timeline"
        private val FALLBACK_EVENTS_FLOW = MutableStateFlow(emptyList<Any>())
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

private fun Any.readLong(property: String): Long? = when (val value = readAny(property)) {
    is Int -> value.toLong()
    is Long -> value
    else -> null
}
