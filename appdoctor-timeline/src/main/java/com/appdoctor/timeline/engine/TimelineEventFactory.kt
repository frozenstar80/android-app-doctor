package com.appdoctor.timeline.engine

import com.appdoctor.core.metric.Metric
import com.appdoctor.core.monitor.cpu.CpuInfo
import com.appdoctor.core.monitor.fps.FpsInfo
import com.appdoctor.core.monitor.memory.MemoryInfo
import com.appdoctor.timeline.model.RuntimeTimelineEvent
import com.appdoctor.timeline.model.TimelineCategory
import com.appdoctor.timeline.model.TimelineSeverity

public data class CollectorEventResult(
    public val signature: String,
    public val events: List<RuntimeTimelineEvent>,
)

public class TimelineEventFactory(
    private val formatter: TimelineFormatter = TimelineFormatter(),
) {
    private var lastNetworkRequestId: Long = Long.MIN_VALUE
    private var lastDatabaseQueryId: Long = Long.MIN_VALUE
    private val issueDigests = HashMap<String, String>()

    public fun collectorEvents(
        sessionId: String,
        collectorId: String,
        metric: Metric,
        timestampMillis: Long,
        previousSignature: String?,
    ): CollectorEventResult {
        return when (collectorId) {
            "memory" -> simpleEvent(
                sessionId = sessionId,
                collectorId = collectorId,
                category = TimelineCategory.MEMORY,
                metric = metric,
                timestampMillis = timestampMillis,
                previousSignature = previousSignature,
                metadata = memoryMetadata(metric),
            )
            "cpu" -> simpleEvent(
                sessionId = sessionId,
                collectorId = collectorId,
                category = TimelineCategory.CPU,
                metric = metric,
                timestampMillis = timestampMillis,
                previousSignature = previousSignature,
                metadata = cpuMetadata(metric),
            )
            "fps" -> simpleEvent(
                sessionId = sessionId,
                collectorId = collectorId,
                category = TimelineCategory.FPS,
                metric = metric,
                timestampMillis = timestampMillis,
                previousSignature = previousSignature,
                metadata = fpsMetadata(metric),
            )
            "compose" -> simpleEvent(
                sessionId = sessionId,
                collectorId = collectorId,
                category = TimelineCategory.COMPOSE,
                metric = metric,
                timestampMillis = timestampMillis,
                previousSignature = previousSignature,
                metadata = composeMetadata(metric),
            )
            "network" -> networkEvents(sessionId, metric, timestampMillis, previousSignature)
            "database" -> databaseEvents(sessionId, metric, timestampMillis, previousSignature)
            else -> simpleEvent(
                sessionId = sessionId,
                collectorId = collectorId,
                category = TimelineCategory.COLLECTOR,
                metric = metric,
                timestampMillis = timestampMillis,
                previousSignature = previousSignature,
                metadata = mapOf(
                    "metricType" to metric.javaClass.simpleName,
                    "snapshot" to metric.toString(),
                ),
            )
        }
    }

    public fun diagnosticEvents(
        sessionId: String,
        diagnosticIssues: List<Any>,
    ): List<RuntimeTimelineEvent> = diagnosticIssues.mapNotNull { issue ->
        val id = issue.readString("id") ?: return@mapNotNull null
        val status = issue.readAny("status")?.toString() ?: "OPEN"
        val confidence = issue.readAny("confidence")?.toString() ?: "0"
        val digest = "$status|$confidence|${issue.readAny("timestampMillis")}"
        if (issueDigests[id] == digest) return@mapNotNull null
        issueDigests[id] = digest
        val timestamp = issue.readLong("timestampMillis") ?: System.currentTimeMillis()
        val severity = parseSeverity(issue.readAny("severity")?.toString())
        RuntimeTimelineEvent(
            timestamp = timestamp,
            sessionId = sessionId,
            source = "diagnostics",
            collectorId = "diagnostics",
            category = TimelineCategory.DIAGNOSTICS,
            title = "Diagnostic issue $status",
            summary = issue.readString("title") ?: "Diagnostics issue update",
            severity = severity,
            relatedIssueId = id,
            metadata = buildMap {
                put("status", status)
                put("confidence", confidence)
                put("category", issue.readAny("category")?.toString() ?: "UNKNOWN")
            },
            sourceMetric = "DiagnosticIssue",
        )
    }

    private fun simpleEvent(
        sessionId: String,
        collectorId: String,
        category: TimelineCategory,
        metric: Metric,
        timestampMillis: Long,
        previousSignature: String?,
        metadata: Map<String, Any>,
    ): CollectorEventResult {
        val signature = "$collectorId:${metric.toString()}"
        if (signature == previousSignature) return CollectorEventResult(signature, emptyList())
        val severity = inferSeverity(metadata)
        val title = formatter.titleFor(collectorId)
        val summary = formatter.summary(metadata)
        return CollectorEventResult(
            signature = signature,
            events = listOf(
                RuntimeTimelineEvent(
                    timestamp = timestampMillis,
                    sessionId = sessionId,
                    source = collectorId,
                    collectorId = collectorId,
                    category = category,
                    title = title,
                    summary = summary,
                    severity = severity,
                    relatedIssueId = null,
                    metadata = metadata,
                    sourceMetric = metric.javaClass.simpleName,
                ),
            ),
        )
    }

    private fun networkEvents(
        sessionId: String,
        metric: Metric,
        timestampMillis: Long,
        previousSignature: String?,
    ): CollectorEventResult {
        val requests = metric.readList("requests")
        val signature = "network:${requests.size}:${requests.firstOrNull()?.readLong("id") ?: -1L}"
        if (signature == previousSignature) return CollectorEventResult(signature, emptyList())

        val newEvents = requests
            .mapNotNull { request ->
                val id = request.readLong("id") ?: return@mapNotNull null
                if (id <= lastNetworkRequestId) return@mapNotNull null
                val success = request.readBoolean("success") ?: true
                val latency = request.readLong("responseTimeMillis") ?: 0L
                val url = request.readString("url") ?: "(unknown)"
                RuntimeTimelineEvent(
                    timestamp = request.readLong("timestampMillis") ?: timestampMillis,
                    sessionId = sessionId,
                    source = "network",
                    collectorId = "network",
                    category = TimelineCategory.NETWORK,
                    title = "Network request",
                    summary = "${request.readString("method") ?: "?"} $url (${latency}ms)",
                    severity = when {
                        !success -> TimelineSeverity.ERROR
                        latency >= 1_000L -> TimelineSeverity.WARNING
                        else -> TimelineSeverity.INFO
                    },
                    relatedIssueId = null,
                    metadata = buildMap {
                        put("id", id)
                        put("url", url)
                        put("latencyMillis", latency)
                        put("success", success)
                        put("statusCode", request.readLong("statusCode") ?: -1L)
                    },
                    sourceMetric = metric.javaClass.simpleName,
                )
            }
            .sortedBy { it.timestamp }

        if (newEvents.isNotEmpty()) {
            lastNetworkRequestId = maxOf(lastNetworkRequestId, newEvents.maxOf { it.metadata["id"] as Long })
        }
        return CollectorEventResult(signature, newEvents)
    }

    private fun databaseEvents(
        sessionId: String,
        metric: Metric,
        timestampMillis: Long,
        previousSignature: String?,
    ): CollectorEventResult {
        val queries = metric.readList("queries")
        val signature = "database:${queries.size}:${queries.firstOrNull()?.readLong("id") ?: -1L}"
        if (signature == previousSignature) return CollectorEventResult(signature, emptyList())

        val newEvents = queries
            .mapNotNull { query ->
                val id = query.readLong("id") ?: return@mapNotNull null
                if (id <= lastDatabaseQueryId) return@mapNotNull null
                val success = query.readBoolean("success") ?: true
                val duration = query.readDouble("durationMillis")
                    ?: (query.readLong("durationNanos")?.toDouble()?.div(1_000_000.0) ?: 0.0)
                RuntimeTimelineEvent(
                    timestamp = query.readLong("timestampMillis") ?: timestampMillis,
                    sessionId = sessionId,
                    source = "database",
                    collectorId = "database",
                    category = TimelineCategory.DATABASE,
                    title = "Database query",
                    summary = "${query.readAny("type") ?: "SQL"} (${duration.toInt()}ms)",
                    severity = when {
                        !success -> TimelineSeverity.ERROR
                        duration >= 40.0 -> TimelineSeverity.WARNING
                        else -> TimelineSeverity.INFO
                    },
                    relatedIssueId = null,
                    metadata = buildMap {
                        put("id", id)
                        put("durationMillis", duration)
                        put("success", success)
                        put("thread", query.readString("threadName") ?: "(unknown)")
                        put("type", query.readAny("type")?.toString() ?: "UNKNOWN")
                    },
                    sourceMetric = metric.javaClass.simpleName,
                )
            }
            .sortedBy { it.timestamp }

        if (newEvents.isNotEmpty()) {
            lastDatabaseQueryId = maxOf(lastDatabaseQueryId, newEvents.maxOf { it.metadata["id"] as Long })
        }
        return CollectorEventResult(signature, newEvents)
    }

    private fun memoryMetadata(metric: Metric): Map<String, Any> {
        val info = metric as? MemoryInfo ?: return mapOf("value" to metric.toString())
        return mapOf(
            "usagePercent" to info.usagePercent,
            "usedBytes" to info.usedBytes,
            "nativeAllocatedBytes" to info.nativeAllocatedBytes,
        )
    }

    private fun cpuMetadata(metric: Metric): Map<String, Any> {
        val info = metric as? CpuInfo ?: return mapOf("value" to metric.toString())
        return mapOf(
            "processCpuPercent" to info.processCpuPercent,
            "coreCount" to info.coreCount,
        )
    }

    private fun fpsMetadata(metric: Metric): Map<String, Any> {
        val info = metric as? FpsInfo ?: return mapOf("value" to metric.toString())
        return mapOf(
            "currentFps" to info.current,
            "averageFps" to info.average,
            "lowestFps" to info.lowest,
        )
    }

    private fun composeMetadata(metric: Metric): Map<String, Any> = buildMap {
        put("metricType", metric.javaClass.simpleName)
        metric.readDouble("recompositionRate")?.let { put("recompositionRate", it) }
        metric.readDouble("frameDropRate")?.let { put("frameDropRate", it) }
    }

    private fun inferSeverity(metadata: Map<String, Any>): TimelineSeverity? {
        val cpu = metadata["processCpuPercent"] as? Float
        if (cpu != null && cpu >= 85f) return TimelineSeverity.WARNING
        val memory = metadata["usagePercent"] as? Float
        if (memory != null && memory >= 90f) return TimelineSeverity.WARNING
        val fps = metadata["averageFps"] as? Float
        if (fps != null && fps <= 45f) return TimelineSeverity.WARNING
        return TimelineSeverity.INFO
    }

    private fun parseSeverity(raw: String?): TimelineSeverity? = when (raw) {
        "INFO" -> TimelineSeverity.INFO
        "WARNING" -> TimelineSeverity.WARNING
        "ERROR" -> TimelineSeverity.ERROR
        "CRITICAL" -> TimelineSeverity.CRITICAL
        else -> null
    }
}

private fun Any?.readAny(property: String): Any? {
    val target = this ?: return null
    val getter = target.javaClass.methods.firstOrNull {
        it.name == "get${property.replaceFirstChar { ch -> ch.uppercaseChar() }}" && it.parameterCount == 0
    } ?: return null
    return runCatching { getter.invoke(target) }.getOrNull()
}

private fun Any?.readString(property: String): String? = this.readAny(property) as? String

private fun Any?.readLong(property: String): Long? = when (val value = this.readAny(property)) {
    is Int -> value.toLong()
    is Long -> value
    else -> null
}

private fun Any?.readDouble(property: String): Double? = when (val value = this.readAny(property)) {
    is Double -> value
    is Float -> value.toDouble()
    is Int -> value.toDouble()
    is Long -> value.toDouble()
    else -> null
}

private fun Any?.readBoolean(property: String): Boolean? = this.readAny(property) as? Boolean

private fun Metric.readList(property: String): List<Any> =
    (readAny(property) as? List<*>)?.mapNotNull { it } ?: emptyList()
