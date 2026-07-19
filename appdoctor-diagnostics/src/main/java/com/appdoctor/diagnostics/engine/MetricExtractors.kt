package com.appdoctor.diagnostics.engine

import com.appdoctor.core.metric.Metric
import com.appdoctor.core.monitor.fps.FpsInfo
import com.appdoctor.core.monitor.memory.MemoryInfo

internal object MetricExtractors {

    fun memory(metric: Metric?, nowMillis: Long): MemorySample? {
        val info = metric as? MemoryInfo ?: return null
        return MemorySample(
            timestampMillis = nowMillis,
            usagePercent = info.usagePercent,
            nativeAllocatedBytes = info.nativeAllocatedBytes,
        )
    }

    fun fps(metric: Metric?, nowMillis: Long): FpsSample? {
        val info = metric as? FpsInfo ?: return null
        return FpsSample(
            timestampMillis = nowMillis,
            averageFps = info.average,
        )
    }

    fun network(metric: Metric?): List<NetworkSample> {
        val requests = metric.readListProperty("requests")
        return requests.mapNotNull { item ->
            val id = item.readLong("id") ?: return@mapNotNull null
            val timestamp = item.readLong("timestampMillis") ?: return@mapNotNull null
            val responseTime = item.readLong("responseTimeMillis") ?: return@mapNotNull null
            val success = item.readBoolean("success") ?: return@mapNotNull null
            NetworkSample(
                id = id,
                timestampMillis = timestamp,
                latencyMillis = responseTime,
                success = success,
            )
        }
    }

    fun database(metric: Metric?): List<DatabaseSample> {
        val queries = metric.readListProperty("queries")
        return queries.mapNotNull { item ->
            val id = item.readLong("id") ?: return@mapNotNull null
            val timestamp = item.readLong("timestampMillis") ?: return@mapNotNull null
            val success = item.readBoolean("success") ?: return@mapNotNull null
            val durationNanos = item.readLong("durationNanos")
            val durationMillis = item.readDouble("durationMillis")
                ?: durationNanos?.toDouble()?.div(1_000_000.0)
                ?: return@mapNotNull null
            DatabaseSample(
                id = id,
                timestampMillis = timestamp,
                durationMillis = durationMillis,
                success = success,
            )
        }
    }

    fun compose(metric: Metric?): ComposeSample? {
        val timestamp = metric.readLong("timestampMillis") ?: return null
        val recompositionRate = metric.readDouble("recompositionRate") ?: return null
        val frameDropRate = metric.readDouble("frameDropRate") ?: return null
        return ComposeSample(
            timestampMillis = timestamp,
            recompositionRate = recompositionRate,
            frameDropRate = frameDropRate,
        )
    }

    private fun Metric?.readListProperty(name: String): List<Any> {
        val raw = this.readAny(name) as? List<*> ?: return emptyList()
        return raw.filterNotNull()
    }

    private fun Any?.readLong(name: String): Long? = when (val value = this.readAny(name)) {
        is Int -> value.toLong()
        is Long -> value
        else -> null
    }

    private fun Any?.readDouble(name: String): Double? = when (val value = this.readAny(name)) {
        is Double -> value
        is Float -> value.toDouble()
        is Int -> value.toDouble()
        is Long -> value.toDouble()
        else -> null
    }

    private fun Any?.readBoolean(name: String): Boolean? = this.readAny(name) as? Boolean

    private fun Any?.readAny(name: String): Any? {
        val target = this ?: return null
        val methodName = "get" + name.replaceFirstChar { it.uppercaseChar() }
        val method = target.javaClass.methods.firstOrNull { it.name == methodName && it.parameterCount == 0 }
            ?: return null
        return runCatching { method.invoke(target) }.getOrNull()
    }
}
