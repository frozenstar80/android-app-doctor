package com.appdoctor.timeline.engine

import com.appdoctor.timeline.model.RuntimeTimelineEvent
import com.appdoctor.timeline.model.TimelineFilter
import com.appdoctor.timeline.model.TimelineSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public class TimelineRepository(
    private val maximumEvents: Int,
    private val search: TimelineSearch = TimelineSearch(),
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {

    private val lock = Any()
    private val buffer = ArrayDeque<RuntimeTimelineEvent>()
    private val _events = MutableStateFlow<List<RuntimeTimelineEvent>>(emptyList())
    private var session = TimelineSession(
        id = "timeline-${clockMillis()}",
        startedAtMillis = clockMillis(),
        lastUpdatedAtMillis = clockMillis(),
        eventCount = 0,
    )

    public val events: StateFlow<List<RuntimeTimelineEvent>> = _events

    public fun session(): TimelineSession = synchronized(lock) { session }

    public fun append(event: RuntimeTimelineEvent) {
        synchronized(lock) {
            buffer.addFirst(event)
            trim()
            session = session.copy(
                lastUpdatedAtMillis = event.timestamp,
                eventCount = buffer.size,
            )
            _events.value = buffer.toList()
        }
    }

    public fun appendAll(events: List<RuntimeTimelineEvent>) {
        if (events.isEmpty()) return
        synchronized(lock) {
            events.sortedBy { it.timestamp }.forEach(buffer::addFirst)
            trim()
            val latest = events.maxOf { it.timestamp }
            session = session.copy(
                lastUpdatedAtMillis = latest,
                eventCount = buffer.size,
            )
            _events.value = buffer.toList()
        }
    }

    public fun filtered(filter: TimelineFilter): List<RuntimeTimelineEvent> = synchronized(lock) {
        buffer.asSequence()
            .filter { event ->
                filter.collectorIds.isEmpty() || filter.collectorIds.contains(event.collectorId)
            }
            .filter { event ->
                filter.categories.isEmpty() || filter.categories.contains(event.category)
            }
            .filter { event ->
                filter.severities.isEmpty() || (event.severity != null && filter.severities.contains(event.severity))
            }
            .filter { event ->
                val start = filter.startTimeMillis
                val end = filter.endTimeMillis
                (start == null || event.timestamp >= start) && (end == null || event.timestamp <= end)
            }
            .toList()
    }

    public fun search(filter: TimelineFilter, query: String): List<RuntimeTimelineEvent> =
        search.search(filtered(filter), query)

    private fun trim() {
        val bound = maximumEvents.coerceAtLeast(1)
        while (buffer.size > bound) {
            buffer.removeLast()
        }
    }
}
