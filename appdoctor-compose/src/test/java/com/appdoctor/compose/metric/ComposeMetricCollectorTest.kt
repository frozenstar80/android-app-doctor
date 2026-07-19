package com.appdoctor.compose.metric

import com.appdoctor.compose.FakeFrameProbe
import com.appdoctor.compose.FakeRecomposerProbe
import com.appdoctor.compose.FakeTracker
import com.appdoctor.compose.SequenceClock
import com.appdoctor.compose.internal.runtime.ComposeRuntimeCollectorEngine
import com.appdoctor.compose.internal.runtime.FrameStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ComposeMetricCollectorTest {

    @Test
    fun `has a stable id`() = runTest {
        val collector = ComposeMetricCollector(engine(backgroundScope, counts = listOf(0L)))
        assertEquals("compose", collector.id)
    }

    @Test
    fun `snapshot and current reflect the live engine value`() = runTest {
        val collector = ComposeMetricCollector(engine(backgroundScope, counts = listOf(0L, 7L)))

        assertEquals(0L, collector.current().recompositionCount) // empty before subscription

        val job = launch(UnconfinedTestDispatcher(testScheduler)) { collector.data.collect {} }
        advanceTimeBy(1_500)
        runCurrent()

        assertEquals(7L, collector.current().recompositionCount)
        assertEquals(7L, collector.snapshot().recompositionCount)
        job.cancel()
    }

    private fun engine(scope: CoroutineScope, counts: List<Long>): ComposeRuntimeCollectorEngine =
        ComposeRuntimeCollectorEngine(
            scope = scope,
            tracker = FakeTracker(),
            recomposerProbe = FakeRecomposerProbe(counts = counts),
            frameProbe = FakeFrameProbe(FrameStats.Empty),
            sampleIntervalMillis = 1_000L,
            clockNanos = SequenceClock()::next,
        )
}
