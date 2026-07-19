package com.appdoctor.diagnostics.engine

import com.appdoctor.core.metric.CollectorRegistry
import com.appdoctor.core.ids.CollectorIds
import com.appdoctor.diagnostics.model.DiagnosticIssue
import com.appdoctor.diagnostics.model.HealthReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Asynchronous diagnostics runtime that consumes collector snapshots and emits health + issues.
 */
public class DiagnosticsEngine(
    private val collectorRegistry: CollectorRegistry,
    private val issueDetector: IssueDetector,
    private val issueRepository: IssueRepository,
    private val healthEngine: HealthEngine,
    private val scope: CoroutineScope,
    private val analysisIntervalMillis: Long,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {

    private val memoryHistory = ArrayDeque<MemorySample>()
    private val fpsHistory = ArrayDeque<FpsSample>()
    private val composeHistory = ArrayDeque<ComposeSample>()

    private val _healthReport = MutableStateFlow(HealthReport.Empty)
    public val healthReport: StateFlow<HealthReport> = _healthReport
    public val issues: StateFlow<List<DiagnosticIssue>> = issueRepository.issues

    @Volatile
    private var analysisJob: Job? = null

    public fun start() {
        if (analysisJob?.isActive == true) return
        analysisJob = scope.launch {
            while (isActive) {
                analyzeOnce()
                delay(analysisIntervalMillis.coerceAtLeast(MIN_INTERVAL_MS))
            }
        }
    }

    public fun stop() {
        analysisJob?.cancel()
        analysisJob = null
    }

    public fun dismissIssue(issueId: String): Unit = issueRepository.ignoreIssue(issueId)

    private fun analyzeOnce() {
        val now = clockMillis()
        val snapshots = collectorRegistry.collectors.associate { collector ->
            collector.id to collector.snapshot()
        }

        MetricExtractors.memory(snapshots[CollectorIds.MEMORY], now)?.let {
            memoryHistory.addLast(it)
            trimHistory(memoryHistory, now)
        }
        MetricExtractors.fps(snapshots[CollectorIds.FPS], now)?.let {
            fpsHistory.addLast(it)
            trimHistory(fpsHistory, now)
        }
        MetricExtractors.compose(snapshots[CollectorIds.COMPOSE])?.let {
            composeHistory.addLast(it)
            trimHistory(composeHistory, now)
        }

        val context = RuleContext(
            nowMillis = now,
            memorySamples = memoryHistory.toList(),
            fpsSamples = fpsHistory.toList(),
            networkSamples = MetricExtractors.network(snapshots[CollectorIds.NETWORK]),
            databaseSamples = MetricExtractors.database(snapshots[CollectorIds.DATABASE]),
            composeSamples = composeHistory.toList(),
        )

        val detected = issueDetector.detect(context)
        issueRepository.reconcile(detected)
        _healthReport.value = healthEngine.compute(
            context = context,
            openIssues = issueRepository.openIssues(),
            timestampMillis = now,
        )
    }

    private fun <T : Any> trimHistory(history: ArrayDeque<T>, nowMillis: Long) {
        while (history.isNotEmpty() && sampleTimestamp(history.first()) < nowMillis - HISTORY_WINDOW_MILLIS) {
            history.removeFirst()
        }
    }

    private fun sampleTimestamp(value: Any): Long = when (value) {
        is MemorySample -> value.timestampMillis
        is FpsSample -> value.timestampMillis
        is ComposeSample -> value.timestampMillis
        else -> 0L
    }

    private companion object {
        private const val HISTORY_WINDOW_MILLIS = 5 * 60_000L
        private const val MIN_INTERVAL_MS = 250L
    }
}
