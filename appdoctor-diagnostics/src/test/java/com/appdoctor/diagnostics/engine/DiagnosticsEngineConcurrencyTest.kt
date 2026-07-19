package com.appdoctor.diagnostics.engine

import com.appdoctor.core.metric.Metric
import com.appdoctor.core.monitor.fps.FpsInfo
import com.appdoctor.core.monitor.memory.MemoryInfo
import com.appdoctor.diagnostics.FakeCollector
import com.appdoctor.diagnostics.FakeCollectorRegistry
import com.appdoctor.diagnostics.FakeDatabaseMetric
import com.appdoctor.diagnostics.FakeDatabaseQuery
import com.appdoctor.diagnostics.FakeNetworkMetric
import com.appdoctor.diagnostics.FakeNetworkRequest
import com.appdoctor.diagnostics.model.IssueStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosticsEngineConcurrencyTest {

    @Test
    fun `analysis runs asynchronously without collector coupling`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val memory = FakeCollector("memory", MemoryInfo(usedBytes = 95, maxBytes = 100, freeBytes = 5, nativeAllocatedBytes = 10))
        val fps = FakeCollector("fps", FpsInfo(current = 35f, average = 35f, lowest = 30f))
        val network = FakeCollector<Metric>(
            "network",
            FakeNetworkMetric(
                requests = List(30) { index ->
                    FakeNetworkRequest(
                        id = index.toLong(),
                        timestampMillis = 60_000L + index,
                        responseTimeMillis = 1_000L,
                        success = index % 3 != 0,
                    )
                },
            ),
        )
        val database = FakeCollector<Metric>(
            "database",
            FakeDatabaseMetric(
                queries = List(30) { index ->
                    FakeDatabaseQuery(
                        id = index.toLong(),
                        timestampMillis = 60_000L + index,
                        durationNanos = 60_000_000L,
                        success = index % 4 != 0,
                    )
                },
            ),
        )

        val registry = FakeCollectorRegistry(
            listOf(
                memory as com.appdoctor.core.metric.MetricCollector<Metric>,
                fps as com.appdoctor.core.metric.MetricCollector<Metric>,
                network,
                database,
            ),
        )

        val engine = DiagnosticsEngine(
            collectorRegistry = registry,
            issueDetector = IssueDetector(
                rules = listOf(
                    com.appdoctor.diagnostics.engine.rules.SustainedHighMemoryUsageRule(),
                    com.appdoctor.diagnostics.engine.rules.RepeatedNetworkFailuresRule(),
                ),
                recommendationProvider = RecommendationProvider(),
                confidenceCalculator = ConfidenceCalculator(),
                minimumConfidence = 55,
            ),
            issueRepository = IssueRepository(maximumIssueHistory = 100) { testScheduler.currentTime },
            healthEngine = HealthEngine(),
            scope = scope,
            analysisIntervalMillis = 500L,
            clockMillis = { testScheduler.currentTime },
        )

        engine.start()
        advanceTimeBy(3_000L)
        engine.stop()

        assertTrue(engine.healthReport.value.overallScore in 0..100)
        assertTrue(engine.issues.value.any { it.status == IssueStatus.OPEN || it.status == IssueStatus.RESOLVED || it.status == IssueStatus.IGNORED })
    }
}
