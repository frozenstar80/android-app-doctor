package com.appdoctor.session.engine

import android.os.Build
import com.appdoctor.core.AppDoctor
import com.appdoctor.core.AppDoctorConfig
import com.appdoctor.core.MetricsProvider
import com.appdoctor.core.monitor.memory.MemoryInfo
import com.appdoctor.session.model.DiagnosticsSection
import com.appdoctor.session.model.HealthSection
import com.appdoctor.session.model.SessionMetadata
import com.appdoctor.session.model.SessionReport
import com.appdoctor.session.model.TimelineSection
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
        )
    }

    private fun collectorSummaries(snapshots: Map<String, Any>): Map<String, Map<String, String>> {
        val summaries = LinkedHashMap<String, Map<String, String>>()
        snapshots.forEach { (id, metric) ->
            when {
                id == "memory" -> summaries["memory"] = memorySummary()
                id == "network" -> summaries["network"] = networkSummary(metric)
                id == "database" -> summaries["database"] = databaseSummary(metric)
                id == "compose" -> summaries["compose"] = composeSummary(metric)
                else -> summaries[id] = mapOf("metricType" to metric.javaClass.simpleName)
            }
        }
        return summaries
    }

    private fun memorySummary(): Map<String, String> {
        val history = recorder.history("memory").mapNotNull { it.metric as? MemoryInfo }
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
        val averageLatency = requests.mapNotNull { readLong(it, "responseTimeMillis") }.averageOrZero()
        return linkedMapOf(
            "requestCount" to requests.size.toString(),
            "failureCount" to failureCount.toString(),
            "averageLatencyMillis" to averageLatency.toLong().toString(),
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
            "failureRate" to "%.3f".format(failureRate),
        )
    }

    private fun composeSummary(metric: Any): Map<String, String> = linkedMapOf(
        "recompositionCount" to (readLong(metric, "recompositionCount") ?: 0L).toString(),
        "trackedComposables" to (readInt(metric, "activeComposables") ?: 0).toString(),
        "frameDrops" to (readLong(metric, "frameDrops") ?: 0L).toString(),
    )

    private fun timelineSection(events: List<Any>): TimelineSection? {
        if (events.isEmpty()) return null
        val grouped = events.mapNotNull { readString(it, "groupId") }.distinct().size
        val issues = events.count { readString(it, "relatedIssueId") != null }
        val payload = events.map { event ->
            linkedMapOf(
                "timestamp" to (readLong(event, "timestamp") ?: 0L).toString(),
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

    private fun diagnosticsSection(issues: List<Any>): DiagnosticsSection? {
        if (issues.isEmpty()) return null
        val openIssues = issues.count { readAny(it, "status")?.toString() == "OPEN" }
        val payload = issues.map { issue ->
            linkedMapOf(
                "id" to (readString(issue, "id") ?: ""),
                "title" to (readString(issue, "title") ?: ""),
                "severity" to (readAny(issue, "severity")?.toString() ?: "INFO"),
                "category" to (readAny(issue, "category")?.toString() ?: "UNKNOWN"),
                "confidence" to (readInt(issue, "confidence") ?: 0).toString(),
            )
        }
        return DiagnosticsSection(
            issueCount = issues.size,
            openIssueCount = openIssues,
            issues = payload,
        )
    }

    private fun healthSection(raw: Any?): HealthSection? = raw?.let {
        HealthSection(
            overallScore = readInt(it, "overallScore") ?: return null,
            performanceScore = readInt(it, "performanceScore") ?: return null,
            memoryScore = readInt(it, "memoryScore") ?: return null,
            networkScore = readInt(it, "networkScore") ?: return null,
            databaseScore = readInt(it, "databaseScore") ?: return null,
            composeScore = readInt(it, "composeScore") ?: return null,
            timestampMillis = readLong(it, "timestampMillis") ?: 0L,
        )
    }

    private fun analyticsSummaries(): Map<String, Map<String, String>> {
        val summaries = LinkedHashMap<String, Map<String, String>>()
        AppDoctor.plugin("database-inspector")?.let { database ->
            readFlowValue(database, "analytics")?.let { analytics ->
                summaries["database"] = linkedMapOf(
                    "totalQueries" to (readInt(analytics, "totalQueries") ?: 0).toString(),
                    "slowQueryCount" to (readInt(analytics, "slowQueryCount") ?: 0).toString(),
                    "failedQueryCount" to (readInt(analytics, "failedQueryCount") ?: 0).toString(),
                    "successRate" to (readDouble(analytics, "successRate") ?: 0.0).toString(),
                )
            }
        }
        AppDoctor.plugin("compose-inspector")?.let { compose ->
            readFlowValue(compose, "analytics")?.let { analytics ->
                val frame = readAny(analytics, "frame")
                summaries["compose"] = linkedMapOf(
                    "totalRecompositions" to (readLong(analytics, "totalRecompositions") ?: 0L).toString(),
                    "averageRecompositionsPerSecond" to (readDouble(analytics, "averageRecompositionsPerSecond") ?: 0.0).toString(),
                    "trackedComposables" to (readInt(analytics, "trackedComposables") ?: 0).toString(),
                    "frameDropRate" to (frame?.let { readDouble(it, "dropRate") } ?: 0.0).toString(),
                )
            }
        }
        return summaries
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
        "autoGenerateOnCrash" to config.autoGenerateOnCrash.toString(),
    )

    private fun buildInformation(): Map<String, String> = linkedMapOf(
        "buildVariant" to metrics.appInfo.buildType.name,
        "sdkInt" to Build.VERSION.SDK_INT.toString(),
        "release" to (Build.VERSION.RELEASE ?: "Unknown"),
        "device" to Build.DEVICE,
    )

    private fun timelineEvents(): List<Any> {
        val timelinePlugin = AppDoctor.plugin("timeline") ?: return emptyList()
        return readFlowValue(timelinePlugin, "events") as? List<Any> ?: emptyList()
    }

    private fun diagnosticsIssues(): List<Any> {
        val diagnosticsPlugin = AppDoctor.plugin("diagnostics") ?: return emptyList()
        return readFlowValue(diagnosticsPlugin, "issues") as? List<Any> ?: emptyList()
    }

    private fun healthReport(): Any? {
        val diagnosticsPlugin = AppDoctor.plugin("diagnostics") ?: return null
        return readFlowValue(diagnosticsPlugin, "healthReport")
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
