package com.appdoctor.timeline.model

public interface TimelineEvent {
    public val timestamp: Long
    public val sessionId: String
    public val source: String
    public val metadata: Map<String, Any>
}

public enum class TimelineCategory {
    MEMORY,
    CPU,
    FPS,
    NETWORK,
    DATABASE,
    COMPOSE,
    DIAGNOSTICS,
    COLLECTOR,
}

public enum class TimelineSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL,
}

public data class RuntimeTimelineEvent(
    override val timestamp: Long,
    override val sessionId: String,
    override val source: String,
    public val collectorId: String,
    public val category: TimelineCategory,
    public val title: String,
    public val summary: String,
    public val severity: TimelineSeverity?,
    public val relatedIssueId: String?,
    override val metadata: Map<String, Any>,
    public val sourceMetric: String,
    public val groupId: String? = null,
) : TimelineEvent
