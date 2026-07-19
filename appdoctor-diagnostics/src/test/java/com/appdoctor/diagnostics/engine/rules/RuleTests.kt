package com.appdoctor.diagnostics.engine.rules

import com.appdoctor.diagnostics.engine.DatabaseSample
import com.appdoctor.diagnostics.engine.FpsSample
import com.appdoctor.diagnostics.engine.MemorySample
import com.appdoctor.diagnostics.engine.NetworkSample
import com.appdoctor.diagnostics.engine.RuleContext
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RuleTests {

    @Test
    fun `sustained memory rule triggers on persistent pressure`() {
        val rule = SustainedHighMemoryUsageRule()
        val context = RuleContext(
            nowMillis = 10_000L,
            memorySamples = listOf(
                MemorySample(1, 86f, 0),
                MemorySample(2, 88f, 0),
                MemorySample(3, 90f, 0),
                MemorySample(4, 89f, 0),
                MemorySample(5, 87f, 0),
                MemorySample(6, 91f, 0),
            ),
            fpsSamples = emptyList(),
            networkSamples = emptyList(),
            databaseSamples = emptyList(),
            composeSamples = emptyList(),
        )
        assertNotNull(rule.evaluate(context))
    }

    @Test
    fun `network failure rule ignores healthy samples`() {
        val rule = RepeatedNetworkFailuresRule()
        val context = RuleContext(
            nowMillis = 1_000L,
            memorySamples = emptyList(),
            fpsSamples = emptyList(),
            networkSamples = List(20) { index ->
                NetworkSample(index.toLong(), 1_000L + index, 200L, success = true)
            },
            databaseSamples = emptyList(),
            composeSamples = emptyList(),
        )
        assertNull(rule.evaluate(context))
    }

    @Test
    fun `cross rule triggers when fps and database are both bad`() {
        val rule = FpsDropWithSlowDatabaseRule()
        val context = RuleContext(
            nowMillis = 65_000L,
            memorySamples = emptyList(),
            fpsSamples = List(6) { FpsSample(it.toLong(), 42f) },
            networkSamples = emptyList(),
            databaseSamples = List(8) { index ->
                DatabaseSample(index.toLong(), 64_000L - index * 500L, 55.0, success = true)
            },
            composeSamples = emptyList(),
        )
        assertNotNull(rule.evaluate(context))
    }
}
