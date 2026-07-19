package com.appdoctor.timeline.api

import com.appdoctor.timeline.model.TimelineEvent
import kotlinx.coroutines.flow.StateFlow

public interface TimelineReadApi {
    public fun events(): StateFlow<List<TimelineEvent>>
}
