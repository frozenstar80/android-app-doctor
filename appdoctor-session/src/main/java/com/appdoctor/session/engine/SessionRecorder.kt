package com.appdoctor.session.engine

import com.appdoctor.core.metric.CollectorRegistry
import com.appdoctor.core.metric.Metric
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

public data class MetricSample(
    public val timestampMillis: Long,
    public val metric: Metric,
)

public class SessionRecorder(
    private val collectorRegistry: CollectorRegistry,
    private val scope: CoroutineScope,
    private val sampleIntervalMillis: Long = 1_000L,
    private val maximumSamplesPerCollector: Int = 300,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    private val lock = Any()
    private val history = HashMap<String, ArrayDeque<MetricSample>>()

    @Volatile
    private var job: Job? = null

    public fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                capture()
                delay(sampleIntervalMillis.coerceAtLeast(250L))
            }
        }
    }

    public fun stop() {
        job?.cancel()
        job = null
    }

    public fun latestSnapshots(): Map<String, Metric> = collectorRegistry.collectors.associate { collector ->
        collector.id to collector.snapshot()
    }

    public fun history(collectorId: String): List<MetricSample> = synchronized(lock) {
        history[collectorId]?.toList().orEmpty()
    }

    private fun capture() {
        val now = clockMillis()
        collectorRegistry.collectors.forEach { collector ->
            val sample = MetricSample(timestampMillis = now, metric = collector.snapshot())
            synchronized(lock) {
                val queue = history.getOrPut(collector.id) { ArrayDeque() }
                queue.addLast(sample)
                while (queue.size > maximumSamplesPerCollector.coerceAtLeast(1)) {
                    queue.removeFirst()
                }
            }
        }
    }
}
