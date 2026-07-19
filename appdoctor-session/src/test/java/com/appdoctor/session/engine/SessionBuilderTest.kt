package com.appdoctor.session.engine

import com.appdoctor.core.AppDoctorConfig
import com.appdoctor.core.metric.Metric
import com.appdoctor.core.monitor.memory.MemoryInfo
import com.appdoctor.session.FakeCollector
import com.appdoctor.session.FakeDatabaseMetric
import com.appdoctor.session.FakeDatabaseQuery
import com.appdoctor.session.FakeMetricsProvider
import com.appdoctor.session.FakeNetworkMetric
import com.appdoctor.session.FakeNetworkRequest
import com.appdoctor.session.FakeRegistry
import com.appdoctor.session.model.SessionMetadata
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionBuilderTest {
    @Test
    fun `builds report from collector snapshots`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val memory = FakeCollector("memory", MemoryInfo(100, 200, 100, 10))
        val network = FakeCollector<Metric>(
            "network",
            FakeNetworkMetric(
                requests = listOf(
                    FakeNetworkRequest(1, 40, true),
                    FakeNetworkRequest(2, 120, false),
                ),
            ),
        )
        val database = FakeCollector<Metric>(
            "database",
            FakeDatabaseMetric(
                queries = listOf(
                    FakeDatabaseQuery(1, 10_000_000, true),
                    FakeDatabaseQuery(2, 40_000_000, false),
                ),
            ),
        )
        val registry = FakeRegistry(
            listOf(
                memory as com.appdoctor.core.metric.MetricCollector<Metric>,
                network,
                database,
            ),
        )
        val recorder = SessionRecorder(
            collectorRegistry = registry,
            scope = scope,
            sampleIntervalMillis = 250L,
            clockMillis = { testScheduler.currentTime },
        )
        recorder.start()
        advanceTimeBy(750L)
        recorder.stop()

        val builder = SessionBuilder(
            config = AppDoctorConfig(enableSessionReports = true, slowQueryThresholdMillis = 16L),
            metrics = FakeMetricsProvider(),
            recorder = recorder,
            metadataProvider = { id, start, end ->
                SessionMetadata(
                    sessionId = id,
                    startTimeMillis = start,
                    endTimeMillis = end,
                    durationMillis = end - start,
                    appVersion = "1.0",
                    versionCode = 1L,
                    buildVariant = "DEBUG",
                    packageName = "com.example.app",
                    deviceModel = "Pixel",
                    androidVersion = "15",
                    apiLevel = 35,
                    manufacturer = "Google",
                    screenSize = "1080x2400",
                    orientation = "PORTRAIT",
                )
            },
        )

        val report = builder.build(
            sessionId = "session-1",
            startTimeMillis = 0L,
            endTimeMillis = 2_000L,
        )

        assertEquals("session-1", report.sessionId)
        assertTrue(report.collectorSummaries.containsKey("memory"))
        assertEquals("2", report.collectorSummaries.getValue("network").getValue("requestCount"))
        assertEquals("1", report.collectorSummaries.getValue("database").getValue("slowQueries"))
    }
}
