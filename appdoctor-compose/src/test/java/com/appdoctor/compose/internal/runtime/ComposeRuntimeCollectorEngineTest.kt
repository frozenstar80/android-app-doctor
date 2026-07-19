package com.appdoctor.compose.internal.runtime

import com.appdoctor.compose.FakeFrameProbe
import com.appdoctor.compose.FakeRecomposerProbe
import com.appdoctor.compose.FakeTracker
import com.appdoctor.compose.SequenceClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ComposeRuntimeCollectorEngineTest {

    @Test
    fun `derives recomposition rate from cumulative deltas and maps probe outputs`() = runTest {
        val recomposer = FakeRecomposerProbe(counts = listOf(0L, 5L, 15L, 30L))
        val frame = FakeFrameProbe(FrameStats(frameCount = 100L, frameDrops = 4L))
        val tracker = FakeTracker(activeCount = 2, disposalCount = 1L)
        val nanos = SequenceClock(stepNanos = 1_000_000_000L)

        val engine = ComposeRuntimeCollectorEngine(
            scope = backgroundScope,
            tracker = tracker,
            recomposerProbe = recomposer,
            frameProbe = frame,
            currentScreenProvider = { "Home" },
            sampleIntervalMillis = 1_000L,
            clockMillis = { 42L },
            clockNanos = nanos::next,
        )

        val job = launch(UnconfinedTestDispatcher(testScheduler)) { engine.data.collect {} }
        advanceTimeBy(3_500)
        runCurrent()

        val snapshot = engine.data.value
        assertEquals(30L, snapshot.recompositionCount)
        assertEquals(15.0, snapshot.recompositionRate, 1e-9) // (30 - 15) over 1s
        assertEquals(100L, snapshot.frameCount)
        assertEquals(4L, snapshot.frameDrops)
        assertEquals(2, snapshot.activeComposables)
        assertEquals(1L, snapshot.compositionDisposalCount)
        assertEquals(0L, snapshot.skippedRecompositions)
        assertEquals(0, snapshot.activeAnimations)
        assertEquals("Home", snapshot.currentScreen)
        assertEquals(42L, snapshot.timestampMillis)

        job.cancel()
    }

    @Test
    fun `starts at the empty snapshot before subscription`() = runTest {
        val engine = ComposeRuntimeCollectorEngine(
            scope = backgroundScope,
            tracker = FakeTracker(),
            recomposerProbe = FakeRecomposerProbe(counts = listOf(7L)),
            frameProbe = FakeFrameProbe(FrameStats.Empty),
            clockNanos = SequenceClock()::next,
        )
        assertEquals(0L, engine.data.value.recompositionCount)
    }
}
