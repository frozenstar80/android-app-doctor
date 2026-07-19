package com.appdoctor.timeline.api

import com.appdoctor.timeline.model.TimelineEvent
import kotlinx.coroutines.flow.StateFlow

/**
 * Read-only access contract for timeline streams.
 */
public interface TimelineReadApi {
    /**
     * Returns the live event stream.
     *
     * @return hot flow of timeline events.
     */
    public fun events(): StateFlow<List<TimelineEvent>>
}
