package com.appdoctor.timeline.engine

import com.appdoctor.timeline.model.RuntimeTimelineEvent
import com.appdoctor.timeline.model.TimelineCategory
import com.appdoctor.timeline.model.TimelineFilter
import com.appdoctor.timeline.model.TimelineSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineRepositoryTest {

    @Test
    fun `repository is bounded and ordered newest first`() {
        val repository = TimelineRepository(maximumEvents = 3)
        repository.append(event(1))
        repository.append(event(2))
        repository.append(event(3))
        repository.append(event(4))

        val events = repository.events.value
        assertEquals(3, events.size)
        assertEquals(4L, events[0].timestamp)
        assertEquals(2L, events[2].timestamp)
    }

    @Test
    fun `filters and search work`() {
        val repository = TimelineRepository(maximumEvents = 10)
        repository.append(event(1, collectorId = "memory", title = "Memory Sample"))
        repository.append(event(2, collectorId = "network", title = "Network request"))
        repository.append(event(3, collectorId = "database", title = "Database query"))

        val filtered = repository.filtered(TimelineFilter(collectorIds = setOf("network")))
        assertEquals(1, filtered.size)
        assertEquals("network", filtered.single().collectorId)

        val searched = repository.search(TimelineFilter(), "query")
        assertEquals(1, searched.size)
        assertTrue(searched.single().title.contains("query", ignoreCase = true))
    }

    private fun event(
        timestamp: Long,
        collectorId: String = "memory",
        title: String = "Event",
    ): RuntimeTimelineEvent = RuntimeTimelineEvent(
        timestamp = timestamp,
        sessionId = "s1",
        source = collectorId,
        collectorId = collectorId,
        category = TimelineCategory.COLLECTOR,
        title = title,
        summary = title,
        severity = TimelineSeverity.INFO,
        relatedIssueId = null,
        metadata = mapOf("k" to "v"),
        sourceMetric = "Metric",
    )
}
