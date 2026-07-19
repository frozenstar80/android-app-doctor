package com.appdoctor.timeline.model

public data class TimelineSession(
    public val id: String,
    public val startedAtMillis: Long,
    public val lastUpdatedAtMillis: Long,
    public val eventCount: Int,
)

public data class TimelineFilter(
    public val collectorIds: Set<String> = emptySet(),
    public val severities: Set<TimelineSeverity> = emptySet(),
    public val categories: Set<TimelineCategory> = emptySet(),
    public val startTimeMillis: Long? = null,
    public val endTimeMillis: Long? = null,
)
