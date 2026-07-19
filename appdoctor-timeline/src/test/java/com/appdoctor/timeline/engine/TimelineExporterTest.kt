package com.appdoctor.timeline.engine

import com.appdoctor.timeline.model.RuntimeTimelineEvent
import com.appdoctor.timeline.model.TimelineCategory
import com.appdoctor.timeline.model.TimelineSeverity
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineExporterTest {

    private val exporter = TimelineExporter()

    @Test
    fun `exports ordered json`() {
        val json = exporter.exportJson(
            listOf(event(3), event(1), event(2)),
        )
        val i1 = json.indexOf("\"timestamp\":1")
        val i2 = json.indexOf("\"timestamp\":2")
        val i3 = json.indexOf("\"timestamp\":3")
        assertTrue(i1 < i2 && i2 < i3)
        assertTrue(json.contains("\"metadata\""))
    }

    @Test
    fun `exports markdown with metadata`() {
        val markdown = exporter.exportMarkdown(listOf(event(1)))
        assertTrue(markdown.contains("# AppDoctor Timeline Export"))
        assertTrue(markdown.contains("| Timestamp | Collector |"))
        assertTrue(markdown.contains("## Event Metadata"))
        assertTrue(markdown.contains("Source Metric"))
    }

    private fun event(timestamp: Long): RuntimeTimelineEvent = RuntimeTimelineEvent(
        timestamp = timestamp,
        sessionId = "session",
        source = "memory",
        collectorId = "memory",
        category = TimelineCategory.MEMORY,
        title = "Memory Sample",
        summary = "usagePercent=40",
        severity = TimelineSeverity.INFO,
        relatedIssueId = null,
        metadata = mapOf("usagePercent" to 40),
        sourceMetric = "MemoryInfo",
    )
}
