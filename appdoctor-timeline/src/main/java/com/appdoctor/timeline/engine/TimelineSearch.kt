package com.appdoctor.timeline.engine

import com.appdoctor.timeline.model.RuntimeTimelineEvent

public class TimelineSearch {
    public fun search(events: List<RuntimeTimelineEvent>, query: String): List<RuntimeTimelineEvent> {
        val normalized = query.trim()
        if (normalized.isEmpty()) return events
        return events.filter { event ->
            event.title.contains(normalized, ignoreCase = true) ||
                event.summary.contains(normalized, ignoreCase = true) ||
                event.collectorId.contains(normalized, ignoreCase = true) ||
                event.category.name.contains(normalized, ignoreCase = true) ||
                event.relatedIssueId?.contains(normalized, ignoreCase = true) == true ||
                event.metadata.entries.any { (key, value) ->
                    key.contains(normalized, ignoreCase = true) ||
                        value.toString().contains(normalized, ignoreCase = true)
                }
        }
    }
}
