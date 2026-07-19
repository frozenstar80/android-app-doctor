package com.appdoctor.compose.internal.runtime

import org.junit.Assert.assertEquals
import org.junit.Test

class RecompositionCounterTest {

    @Test
    fun `accumulates positive deltas and stays monotonic across recomposer churn`() {
        val counter = RecompositionCounter<String>()

        assertEquals(0L, counter.update(emptyMap()))
        assertEquals(10L, counter.update(mapOf("a" to 10L))) // new recomposer contributes 10
        assertEquals(15L, counter.update(mapOf("a" to 15L))) // +5
        assertEquals(15L, counter.update(mapOf("a" to 15L))) // unchanged

        // "a" disappears (its 15 stays baked in), "b" appears with 3.
        assertEquals(18L, counter.update(mapOf("b" to 3L)))
        assertEquals(20L, counter.update(mapOf("b" to 5L))) // +2
    }

    @Test
    fun `ignores negative deltas`() {
        val counter = RecompositionCounter<String>()
        assertEquals(10L, counter.update(mapOf("a" to 10L)))
        assertEquals(10L, counter.update(mapOf("a" to 4L))) // negative delta ignored
        assertEquals(12L, counter.update(mapOf("a" to 6L))) // resumes from the new baseline (4 -> 6)
    }

    @Test
    fun `sums across multiple concurrent recomposers`() {
        val counter = RecompositionCounter<String>()
        assertEquals(30L, counter.update(mapOf("a" to 10L, "b" to 20L)))
        assertEquals(45L, counter.update(mapOf("a" to 15L, "b" to 30L))) // +5 +10
    }
}
