package com.appdoctor.timeline.engine

import com.appdoctor.core.metric.Metric
import com.appdoctor.core.monitor.memory.MemoryInfo
import com.appdoctor.timeline.FakeCollector
import com.appdoctor.timeline.FakeCollectorRegistry
import com.appdoctor.timeline.FakeNetworkMetric
import com.appdoctor.timeline.FakeNetworkRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineEngineTest {

    @Test
    fun `orders events and assigns grouping ids`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val memory = FakeCollector("memory", MemoryInfo(usedBytes = 50, maxBytes = 100, freeBytes = 50, nativeAllocatedBytes = 0))
        val network = FakeCollector<Metric>(
            "network",
            FakeNetworkMetric(
                requests = listOf(
                    FakeNetworkRequest(id = 1, timestampMillis = 1_000L, responseTimeMillis = 200, success = true),
                    FakeNetworkRequest(id = 2, timestampMillis = 1_500L, responseTimeMillis = 900, success = true),
                ),
            ),
        )
        val registry = FakeCollectorRegistry(
            listOf(
                memory as com.appdoctor.core.metric.MetricCollector<Metric>,
                network,
            ),
        )
        val repository = TimelineRepository(maximumEvents = 100) { testScheduler.currentTime }
        val engine = TimelineEngine(
            collectorRegistry = registry,
            repository = repository,
            factory = TimelineEventFactory(),
            exporter = TimelineExporter(),
            scope = scope,
            groupingWindowMillis = 1_000L,
            clockMillis = { testScheduler.currentTime },
        )

        engine.start()
        advanceTimeBy(2_500L)
        engine.stop()

        val events = engine.events.value.sortedBy { it.timestamp }
        assertTrue(events.isNotEmpty())
        assertTrue(events.all { !it.groupId.isNullOrBlank() })
    }
}
