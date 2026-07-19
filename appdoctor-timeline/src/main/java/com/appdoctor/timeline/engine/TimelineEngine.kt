package com.appdoctor.timeline.engine

import com.appdoctor.core.AppDoctor
import com.appdoctor.core.metric.CollectorRegistry
import com.appdoctor.timeline.model.RuntimeTimelineEvent
import com.appdoctor.timeline.model.TimelineFilter
import com.appdoctor.timeline.model.TimelineSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

public class TimelineEngine(
    private val collectorRegistry: CollectorRegistry,
    private val repository: TimelineRepository,
    private val factory: TimelineEventFactory,
    private val exporter: TimelineExporter,
    private val scope: CoroutineScope,
    private val groupingWindowMillis: Long,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    private val signatureByCollectorId = HashMap<String, String>()
    private var lastGroupedTimestamp: Long? = null
    private var currentGroupNumber: Long = 0L

    @Volatile
    private var job: Job? = null

    public val events: StateFlow<List<RuntimeTimelineEvent>> = repository.events

    public fun session(): TimelineSession = repository.session()

    public fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                collectOnce()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    public fun stop() {
        job?.cancel()
        job = null
    }

    public fun search(filter: TimelineFilter, query: String): List<RuntimeTimelineEvent> =
        repository.search(filter, query)

    public fun filter(filter: TimelineFilter): List<RuntimeTimelineEvent> = repository.filtered(filter)

    public fun exportJson(filter: TimelineFilter = TimelineFilter()): String =
        exporter.exportJson(repository.filtered(filter).sortedBy { it.timestamp })

    public fun exportMarkdown(filter: TimelineFilter = TimelineFilter()): String =
        exporter.exportMarkdown(repository.filtered(filter).sortedBy { it.timestamp })

    private fun collectOnce() {
        val sessionId = repository.session().id
        val now = clockMillis()
        val emitted = ArrayList<RuntimeTimelineEvent>()

        collectorRegistry.collectors.forEach { collector ->
            val snapshot = collector.snapshot()
            val previousSignature = signatureByCollectorId[collector.id]
            val result = factory.collectorEvents(
                sessionId = sessionId,
                collectorId = collector.id,
                metric = snapshot,
                timestampMillis = now,
                previousSignature = previousSignature,
            )
            signatureByCollectorId[collector.id] = result.signature
            emitted.addAll(result.events)
        }

        emitted.addAll(factory.diagnosticEvents(sessionId = sessionId, diagnosticIssues = diagnosticsIssues()))
        val grouped = applyGrouping(emitted.sortedBy { it.timestamp })
        repository.appendAll(grouped)
    }

    private fun diagnosticsIssues(): List<Any> {
        val diagnosticsPlugin = AppDoctor.plugin("diagnostics") ?: return emptyList()
        val getter = diagnosticsPlugin.javaClass.methods.firstOrNull {
            it.name == "getIssues" && it.parameterCount == 0
        } ?: return emptyList()
        val flow = runCatching { getter.invoke(diagnosticsPlugin) }.getOrNull() as? StateFlow<*>
            ?: return emptyList()
        return (flow.value as? List<*>)?.filterNotNull().orEmpty()
    }

    private fun applyGrouping(events: List<RuntimeTimelineEvent>): List<RuntimeTimelineEvent> {
        if (events.isEmpty()) return emptyList()
        val grouped = ArrayList<RuntimeTimelineEvent>(events.size)
        events.forEach { event ->
            val previousTimestamp = lastGroupedTimestamp
            val groupId = if (previousTimestamp != null &&
                event.timestamp - previousTimestamp <= groupingWindowMillis.coerceAtLeast(0L)
            ) {
                "group-$currentGroupNumber"
            } else {
                currentGroupNumber += 1L
                "group-$currentGroupNumber"
            }
            lastGroupedTimestamp = event.timestamp
            grouped += event.copy(groupId = groupId)
        }
        return grouped
    }

    private companion object {
        private const val POLL_INTERVAL_MS = 1_000L
    }
}
