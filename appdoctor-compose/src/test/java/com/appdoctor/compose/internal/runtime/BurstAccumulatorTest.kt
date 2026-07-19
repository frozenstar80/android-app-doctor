package com.appdoctor.compose.internal.runtime

import org.junit.Assert.assertEquals
import org.junit.Test

class BurstAccumulatorTest {

    @Test
    fun `times a completed busy burst`() {
        val acc = BurstAccumulator()
        acc.onState("r", busy = true, nowNanos = 100)
        acc.onState("r", busy = false, nowNanos = 300)

        val stats = acc.snapshot()
        assertEquals(1L, stats.count)
        assertEquals(200L, stats.totalNanos)
        assertEquals(200L, stats.longestNanos)
        assertEquals(200.0, stats.averageNanos, 1e-9)
    }

    @Test
    fun `accumulates multiple bursts and tracks the longest`() {
        val acc = BurstAccumulator()
        acc.onState("r", true, 0)
        acc.onState("r", false, 200) // 200
        acc.onState("r", true, 400)
        acc.onState("r", false, 900) // 500

        val stats = acc.snapshot()
        assertEquals(2L, stats.count)
        assertEquals(700L, stats.totalNanos)
        assertEquals(500L, stats.longestNanos)
        assertEquals(350.0, stats.averageNanos, 1e-9)
    }

    @Test
    fun `idle without a start is ignored`() {
        val acc = BurstAccumulator()
        acc.onState("r", busy = false, nowNanos = 100)
        assertEquals(0L, acc.snapshot().count)
    }

    @Test
    fun `tracks recomposers independently by key`() {
        val acc = BurstAccumulator()
        acc.onState("a", true, 0)
        acc.onState("b", true, 10)
        acc.onState("a", false, 100) // a: 100, b still open

        val stats = acc.snapshot()
        assertEquals(1L, stats.count)
        assertEquals(100L, stats.totalNanos)
    }

    @Test
    fun `forget closes an open burst`() {
        val acc = BurstAccumulator()
        acc.onState("a", true, 0)
        acc.forget("a", 50)
        val stats = acc.snapshot()
        assertEquals(1L, stats.count)
        assertEquals(50L, stats.totalNanos)
    }
}
