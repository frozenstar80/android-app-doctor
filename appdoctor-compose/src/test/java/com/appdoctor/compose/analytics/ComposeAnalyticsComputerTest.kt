package com.appdoctor.compose.analytics

import com.appdoctor.compose.model.ComposeRuntimeSnapshot
import com.appdoctor.compose.model.TrackedComposable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeAnalyticsComputerTest {

    @Test
    fun `empty tracked list still derives snapshot totals and frame stats`() {
        val snapshot = ComposeRuntimeSnapshot.Empty.copy(
            recompositionCount = 100L,
            frameCount = 50L,
            frameDrops = 5L,
        )
        val analytics = ComposeAnalyticsComputer.compute(snapshot, emptyList(), elapsedMillis = 10_000L)

        assertEquals(100L, analytics.totalRecompositions)
        assertEquals(10.0, analytics.averageRecompositionsPerSecond, 1e-9) // 100 / 10s
        assertEquals(50L, analytics.frame.frameCount)
        assertEquals(5L, analytics.frame.frameDrops)
        assertEquals(0.1, analytics.frame.dropRate, 1e-9)
        assertTrue(analytics.mostRecomposed.isEmpty())
        assertTrue(analytics.screens.isEmpty())
    }

    @Test
    fun `ranks tracked composables and groups screens`() {
        val tracked = listOf(
            tracked(name = "A", recompositions = 10, firstMs = 0, lastMs = 1_000, screen = "Home"),
            tracked(name = "B", recompositions = 2, firstMs = 0, lastMs = 5_000, screen = "Home"),
            tracked(name = "C", recompositions = 50, firstMs = 0, lastMs = 500, screen = "Detail"),
        )
        val snapshot = ComposeRuntimeSnapshot.Empty.copy(recompositionCount = 62L)

        val analytics = ComposeAnalyticsComputer.compute(snapshot, tracked, elapsedMillis = 10_000L)

        assertEquals("C", analytics.mostRecomposed.first().name) // 50 recompositions
        assertEquals("B", analytics.leastRecomposed.first().name) // 2 recompositions
        assertEquals("B", analytics.longestLiving.first().name) // 5s lifetime
        assertEquals("C", analytics.highestFrequency.first().name) // 50 / 0.5s = 100/s
        assertEquals(3, analytics.trackedComposables)

        // Screens ranked by recompositions: Detail (50) before Home (12).
        assertEquals("Detail", analytics.screens.first().screen)
        assertEquals(50L, analytics.screens.first().recompositions)
        val home = analytics.screens.first { it.screen == "Home" }
        assertEquals(2, home.trackedComposables)
        assertEquals(12L, home.recompositions)
    }

    private fun tracked(
        name: String,
        recompositions: Long,
        firstMs: Long,
        lastMs: Long,
        screen: String?,
    ) = TrackedComposable(
        name = name,
        recompositions = recompositions,
        firstComposedAtMillis = firstMs,
        lastRecomposedAtMillis = lastMs,
        depth = TrackedComposable.UNKNOWN_DEPTH,
        screen = screen,
        disposed = false,
    )
}
