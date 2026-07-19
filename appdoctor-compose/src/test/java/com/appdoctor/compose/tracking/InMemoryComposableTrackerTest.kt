package com.appdoctor.compose.tracking

import com.appdoctor.compose.model.TrackedComposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicLong

class InMemoryComposableTrackerTest {

    @Test
    fun `records nothing while disabled`() {
        val tracker = InMemoryComposableTracker(limit = 10)
        tracker.onCommit("A", initialComposition = true, depth = 0, screen = null)
        assertTrue(tracker.tracked.value.isEmpty())
        assertEquals(0, tracker.activeCount)
    }

    @Test
    fun `counts recompositions separately from the initial composition`() {
        val tracker = InMemoryComposableTracker(limit = 10).apply { setEnabled(true) }
        tracker.onCommit("A", initialComposition = true, depth = 2, screen = "Home")
        tracker.onCommit("A", initialComposition = false, depth = 2, screen = "Home")
        tracker.onCommit("A", initialComposition = false, depth = 2, screen = "Home")

        val entry = tracker.tracked.value.first { it.name == "A" }
        assertEquals(2L, entry.recompositions)
        assertEquals(2, entry.depth)
        assertEquals("Home", entry.screen)
        assertFalse(entry.disposed)
        assertEquals(1, tracker.activeCount)
    }

    @Test
    fun `disposal decrements active count and marks entry disposed`() {
        val tracker = InMemoryComposableTracker(limit = 10).apply { setEnabled(true) }
        tracker.onCommit("A", initialComposition = true, depth = 0, screen = null)
        tracker.onDisposed("A")

        assertEquals(1L, tracker.disposalCount)
        assertEquals(0, tracker.activeCount)
        assertTrue(tracker.tracked.value.first { it.name == "A" }.disposed)
    }

    @Test
    fun `evicts the least recently active entry beyond capacity`() {
        val clock = AtomicLong(0L)
        val tracker = InMemoryComposableTracker(limit = 2, clock = { clock.getAndIncrement() })
            .apply { setEnabled(true) }
        tracker.onCommit("A", initialComposition = true, depth = 0, screen = null) // t=0
        tracker.onCommit("B", initialComposition = true, depth = 0, screen = null) // t=1
        tracker.onCommit("C", initialComposition = true, depth = 0, screen = null) // t=2 -> evicts A

        val names = tracker.tracked.value.map(TrackedComposable::name).toSet()
        assertEquals(setOf("B", "C"), names)
    }

    @Test
    fun `supports concurrent commits safely`() = runBlocking {
        val tracker = InMemoryComposableTracker(limit = 1_000).apply { setEnabled(true) }
        (0 until 500).map { index ->
            async(Dispatchers.Default) {
                tracker.onCommit("c$index", initialComposition = true, depth = 0, screen = null)
            }
        }.awaitAll()

        assertEquals(500, tracker.tracked.value.size)
        assertEquals(500, tracker.activeCount)
    }
}
