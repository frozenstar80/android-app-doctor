package com.appdoctor.timeline.engine

import com.appdoctor.timeline.model.RuntimeTimelineEvent
import com.appdoctor.timeline.model.TimelineCategory
import com.appdoctor.timeline.model.TimelineSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineConcurrencyTest {

    @Test
    fun `repository append is thread-safe`() = runBlocking {
        val repository = TimelineRepository(maximumEvents = 500)
        (0 until 100).map { i ->
            async(Dispatchers.Default) {
                repository.append(event(i.toLong()))
            }
        }.awaitAll()

        val events = repository.events.value
        assertEquals(100, events.size)
        assertTrue(events.first().timestamp >= events.last().timestamp)
    }

    private fun event(timestamp: Long): RuntimeTimelineEvent = RuntimeTimelineEvent(
        timestamp = timestamp,
        sessionId = "s",
        source = "memory",
        collectorId = "memory",
        category = TimelineCategory.MEMORY,
        title = "t",
        summary = "s",
        severity = TimelineSeverity.INFO,
        relatedIssueId = null,
        metadata = emptyMap(),
        sourceMetric = "MemoryInfo",
    )
}
