package com.appdoctor.session.engine

import android.os.Build
import com.appdoctor.core.AppDoctor
import com.appdoctor.core.AppDoctorConfig
import com.appdoctor.core.MetricsProvider
import com.appdoctor.core.ids.CollectorIds
import com.appdoctor.core.ids.PluginIds
import com.appdoctor.core.monitor.memory.MemoryInfo
import com.appdoctor.diagnostics.api.DiagnosticsReadApi
import com.appdoctor.diagnostics.model.DiagnosticIssue
import com.appdoctor.diagnostics.model.HealthReport
import com.appdoctor.session.model.DiagnosticsSection
import com.appdoctor.session.model.HealthSection
import com.appdoctor.session.model.SessionMetadata
import com.appdoctor.session.model.SessionReport
import com.appdoctor.session.model.TimelineSection
import com.appdoctor.timeline.api.TimelineReadApi
import com.appdoctor.timeline.model.TimelineEvent
import kotlinx.coroutines.flow.StateFlow

public class SessionBuilder(
    private val config: AppDoctorConfig,
    private val metrics: MetricsProvider,
    private val recorder: SessionRecorder,
    private val metadataProvider: (String, Long, Long) -> SessionMetadata,
) {
    public fun build(
        sessionId: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
    ): SessionReport {
        val metadata = metadataProvider(sessionId, startTimeMillis, endTimeMillis)
        val timelineEvents = timelineEvents()
        val diagnosticsIssues = diagnosticsIssues()
        val health = healthReport()
        val snapshots = recorder.latestSnapshots()

        return SessionReport(
            sessionId = sessionId,
            metadata = metadata,
            generatedAtMillis = endTimeMillis,
            durationMillis = metadata.durationMillis,
            timeline = timelineSection(timelineEvents),
            diagnostics = diagnosticsSection(diagnosticsIssues),
            healthReport = healthSection(health),
            collectorSummaries = collectorSummaries(snapshots),
            analyticsSummaries = analyticsSummaries(),
            deviceInformation = deviceInformation(),
            applicationInformation = applicationInformation(),
            configuration = configurationSummary(),
            buildInformation = buildInformation(),
            detailedPayloads = detailedPayloads(snapshots),
        )
    }

    private fun collectorSummaries(snapshots: Map<String, Any>): Map<String, Map<String, String>> {
        val summaries = LinkedHashMap<String, Map<String, String>>()
        snapshots.forEach { (id, metric) ->
            when {
                id == CollectorIds.MEMORY -> summaries[CollectorIds.MEMORY] = memorySummary()
                id == CollectorIds.NETWORK -> summaries[CollectorIds.NETWORK] = networkSummary(metric)
                id == CollectorIds.DATABASE -> summaries[CollectorIds.DATABASE] = databaseSummary(metric)
                id == CollectorIds.COMPOSE -> summaries[CollectorIds.COMPOSE] = composeSummary(metric)
                else -> summaries[id] = mapOf("metricType" to metric.javaClass.simpleName)
            }
        }
        return summaries
    }

    private fun memorySummary(): Map<String, String> {
        val history = recorder.history(CollectorIds.MEMORY).mapNotNull { it.metric as? MemoryInfo }
        if (history.isEmpty()) {
            return mapOf("samples" to "0")
        }
        val used = history.map { it.usedBytes.toDouble() }
        return linkedMapOf(
            "peakMemoryBytes" to used.maxOrNull()!!.toLong().toString(),
            "averageMemoryBytes" to used.average().toLong().toString(),
            "minimumMemoryBytes" to used.minOrNull()!!.toLong().toString(),
            "samples" to history.size.toString(),
        )
    }

    private fun networkSummary(metric: Any): Map<String, String> {
        val requests = readList(metric, "requests")
        val failureCount = requests.count { readBoolean(it, "success") == false }
        val latencies = requests.mapNotNull { readLong(it, "responseTimeMillis") }
        val averageLatency = latencies.averageOrZero()
        val slowestLatency = latencies.maxOrNull() ?: 0L
        return linkedMapOf(
            "requestCount" to requests.size.toString(),
            "failureCount" to failureCount.toString(),
            "averageLatencyMillis" to averageLatency.toLong().toString(),
            "slowestLatencyMillis" to slowestLatency.toString(),
        )
    }

    private fun databaseSummary(metric: Any): Map<String, String> {
        val queries = readList(metric, "queries")
        val failureCount = queries.count { readBoolean(it, "success") == false }
        val slowCount = queries.count {
            val nanos = readLong(it, "durationNanos") ?: return@count false
            nanos / 1_000_000L >= config.slowQueryThresholdMillis
        }
        val failureRate = if (queries.isEmpty()) 0.0 else failureCount.toDouble() / queries.size.toDouble()
        return linkedMapOf(
            "totalQueries" to queries.size.toString(),
            "slowQueries" to slowCount.toString(),
            "failedQueries" to failureCount.toString(),
            "failureRate" to "%.3f".format(failureRate),
        )
    }

    private fun composeSummary(metric: Any): Map<String, String> = linkedMapOf(
        "recompositionCount" to (readLong(metric, "recompositionCount") ?: 0L).toString(),
        "trackedComposables" to (readInt(metric, "activeComposables") ?: 0).toString(),
        "frameDrops" to (readLong(metric, "frameDrops") ?: 0L).toString(),
    )

    private fun timelineSection(events: List<TimelineEvent>): TimelineSection? {
        if (events.isEmpty()) return null
        val grouped = events.mapNotNull { readString(it, "groupId") }.distinct().size
        val issues = events.count { readString(it, "relatedIssueId") != null }
        val payload = events.map { event ->
            linkedMapOf(
                "timestamp" to event.timestamp.toString(),
                "collectorId" to (readString(event, "collectorId") ?: ""),
                "category" to (readAny(event, "category")?.toString() ?: ""),
                "title" to (readString(event, "title") ?: ""),
                "summary" to (readString(event, "summary") ?: ""),
            )
        }
        return TimelineSection(
            totalEvents = events.size,
            groupedEvents = grouped,
            issueReferences = issues,
            events = payload,
        )
    }

    private fun diagnosticsSection(issues: List<DiagnosticIssue>): DiagnosticsSection? {
        if (issues.isEmpty()) return null
        val openIssues = issues.count { it.status.name == "OPEN" }
        val payload = issues.map { issue ->
            linkedMapOf(
                "id" to issue.id,
                "title" to issue.title,
                "severity" to issue.severity.name,
                "category" to issue.category.name,
                "confidence" to issue.confidence.toString(),
            )
        }
        return DiagnosticsSection(
            issueCount = issues.size,
            openIssueCount = openIssues,
            issues = payload,
        )
    }

    private fun healthSection(raw: HealthReport?): HealthSection? = raw?.let {
        HealthSection(
            overallScore = it.overallScore,
            performanceScore = it.performanceScore,
            memoryScore = it.memoryScore,
            networkScore = it.networkScore,
            databaseScore = it.databaseScore,
            composeScore = it.composeScore,
            timestampMillis = it.timestampMillis,
        )
    }

    private fun analyticsSummaries(): Map<String, Map<String, String>> {
        val summaries = LinkedHashMap<String, Map<String, String>>()
        AppDoctor.plugin(PluginIds.DATABASE_INSPECTOR)?.let { database ->
            readFlowValue(database, "analytics")?.let { analytics ->
                summaries[CollectorIds.DATABASE] = linkedMapOf(
                    "totalQueries" to (readInt(analytics, "totalQueries") ?: 0).toString(),
                    "slowQueryCount" to (readInt(analytics, "slowQueryCount") ?: 0).toString(),
                    "failedQueryCount" to (readInt(analytics, "failedQueryCount") ?: 0).toString(),
                    "successRate" to (readDouble(analytics, "successRate") ?: 0.0).toString(),
                )
            }
        }
        AppDoctor.plugin(PluginIds.COMPOSE_INSPECTOR)?.let { compose ->
            readFlowValue(compose, "analytics")?.let { analytics ->
                val frame = readAny(analytics, "frame")
                summaries[CollectorIds.COMPOSE] = linkedMapOf(
                    "totalRecompositions" to (readLong(analytics, "totalRecompositions") ?: 0L).toString(),
                    "averageRecompositionsPerSecond" to (readDouble(analytics, "averageRecompositionsPerSecond") ?: 0.0).toString(),
                    "trackedComposables" to (readInt(analytics, "trackedComposables") ?: 0).toString(),
                    "frameDropRate" to (frame?.let { readDouble(it, "dropRate") } ?: 0.0).toString(),
                )
            }
        }
        return summaries
    }

    private fun detailedPayloads(snapshots: Map<String, Any>): Map<String, List<Map<String, String>>> {
        if (!config.includeFullSessionPayloads) return emptyMap()
        val payloads = LinkedHashMap<String, List<Map<String, String>>>()
        snapshots[CollectorIds.NETWORK]?.let { metric ->
            payloads[CollectorIds.NETWORK] = readList(metric, "requests").map { request ->
                linkedMapOf(
                    "id" to ((readLong(request, "id") ?: -1L).toString()),
                    "timestampMillis" to ((readLong(request, "timestampMillis") ?: 0L).toString()),
                    "method" to (readString(request, "method") ?: ""),
                    "url" to (readString(request, "url") ?: ""),
                    "success" to ((readBoolean(request, "success") ?: true).toString()),
                    "statusCode" to ((readInt(request, "statusCode") ?: -1).toString()),
                    "responseTimeMillis" to ((readLong(request, "responseTimeMillis") ?: 0L).toString()),
                    "requestBody" to bodyText(readAny(request, "requestBody")),
                    "responseBody" to bodyText(readAny(request, "responseBody")),
                    "failureMessage" to (readString(request, "failureMessage") ?: ""),
                )
            }
        }
        snapshots[CollectorIds.DATABASE]?.let { metric ->
            payloads[CollectorIds.DATABASE] = readList(metric, "queries").map { query ->
                linkedMapOf(
                    "id" to ((readLong(query, "id") ?: -1L).toString()),
                    "timestampMillis" to ((readLong(query, "timestampMillis") ?: 0L).toString()),
                    "type" to (readAny(query, "type")?.toString() ?: ""),
                    "sql" to (readString(query, "sql") ?: ""),
                    "durationNanos" to ((readLong(query, "durationNanos") ?: 0L).toString()),
                    "success" to ((readBoolean(query, "success") ?: true).toString()),
                    "error" to (readString(query, "error") ?: ""),
                    "threadName" to (readString(query, "threadName") ?: ""),
                )
            }
        }
        return payloads
    }

    private fun bodyText(body: Any?): String = when {
        body == null -> ""
        readBoolean(body, "isBinary") == true -> "[binary content omitted]"
        else -> readString(body, "text") ?: ""
    }

    private fun deviceInformation(): Map<String, String> = linkedMapOf(
        "androidVersion" to metrics.deviceInfo.androidVersion,
        "apiLevel" to metrics.deviceInfo.apiLevel.toString(),
        "manufacturer" to metrics.deviceInfo.manufacturer,
        "model" to metrics.deviceInfo.model,
        "brand" to metrics.deviceInfo.brand,
        "device" to metrics.deviceInfo.device,
        "supportedAbis" to metrics.deviceInfo.supportedAbis.joinToString(),
    )

    private fun applicationInformation(): Map<String, String> = linkedMapOf(
        "packageName" to metrics.appInfo.packageName,
        "versionName" to metrics.appInfo.versionName,
        "versionCode" to metrics.appInfo.versionCode.toString(),
        "buildType" to metrics.appInfo.buildType.name,
        "minSdk" to metrics.appInfo.minSdk.toString(),
        "targetSdk" to metrics.appInfo.targetSdk.toString(),
    )

    private fun configurationSummary(): Map<String, String> = linkedMapOf(
        "captureNetwork" to config.captureNetwork.toString(),
        "captureDatabase" to config.captureDatabase.toString(),
        "captureCompose" to config.captureCompose.toString(),
        "enableDiagnostics" to config.enableDiagnostics.toString(),
        "enableTimeline" to config.enableTimeline.toString(),
        "enableSessionReports" to config.enableSessionReports.toString(),
        "maximumStoredReports" to config.maximumStoredReports.toString(),
        "includeFullSessionPayloads" to config.includeFullSessionPayloads.toString(),
        "autoGenerateOnCrash" to config.autoGenerateOnCrash.toString(),
    )

    private fun buildInformation(): Map<String, String> = linkedMapOf(
        "buildVariant" to metrics.appInfo.buildType.name,
        "sdkInt" to Build.VERSION.SDK_INT.toString(),
        "release" to (Build.VERSION.RELEASE ?: "Unknown"),
        "device" to Build.DEVICE,
    )

    private fun timelineEvents(): List<TimelineEvent> {
        val timelinePlugin = AppDoctor.plugin(PluginIds.TIMELINE) ?: return emptyList()
        if (timelinePlugin is TimelineReadApi) {
            return timelinePlugin.events().value
        }
        val fallback = readFlowValue(timelinePlugin, "events")
        return (fallback as? List<*>)?.mapNotNull { it as? TimelineEvent }.orEmpty()
    }

    private fun diagnosticsIssues(): List<DiagnosticIssue> {
        val diagnosticsPlugin = AppDoctor.plugin(PluginIds.DIAGNOSTICS) ?: return emptyList()
        if (diagnosticsPlugin is DiagnosticsReadApi) {
            return diagnosticsPlugin.currentIssues()
        }
        val fallback = readFlowValue(diagnosticsPlugin, "issues")
        return (fallback as? List<*>)?.mapNotNull { it as? DiagnosticIssue }.orEmpty()
    }

    private fun healthReport(): HealthReport? {
        val diagnosticsPlugin = AppDoctor.plugin(PluginIds.DIAGNOSTICS) ?: return null
        if (diagnosticsPlugin is DiagnosticsReadApi) {
            return diagnosticsPlugin.currentHealthReport()
        }
        return readFlowValue(diagnosticsPlugin, "healthReport") as? HealthReport
    }

    private fun readFlowValue(source: Any, property: String): Any? {
        val flow = readAny(source, property) as? StateFlow<*> ?: return null
        return flow.value
    }

    private fun readAny(source: Any, property: String): Any? {
        val getter = source.javaClass.methods.firstOrNull {
            it.name == "get${property.replaceFirstChar { ch -> ch.uppercaseChar() }}" && it.parameterCount == 0
        } ?: return null
        return runCatching { getter.invoke(source) }.getOrNull()
    }

    private fun readList(source: Any, property: String): List<Any> =
        (readAny(source, property) as? List<*>)?.filterNotNull().orEmpty()

    private fun readString(source: Any, property: String): String? = readAny(source, property) as? String

    private fun readBoolean(source: Any, property: String): Boolean? = readAny(source, property) as? Boolean

    private fun readInt(source: Any, property: String): Int? = when (val value = readAny(source, property)) {
        is Int -> value
        is Long -> value.toInt()
        else -> null
    }

    private fun readLong(source: Any, property: String): Long? = when (val value = readAny(source, property)) {
        is Int -> value.toLong()
        is Long -> value
        else -> null
    }

    private fun readDouble(source: Any, property: String): Double? = when (val value = readAny(source, property)) {
        is Float -> value.toDouble()
        is Double -> value
        is Int -> value.toDouble()
        is Long -> value.toDouble()
        else -> null
    }
}

private fun List<Long>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()
