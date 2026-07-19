package com.appdoctor.session.model

public data class SessionMetadata(
    public val sessionId: String,
    public val startTimeMillis: Long,
    public val endTimeMillis: Long,
    public val durationMillis: Long,
    public val appVersion: String,
    public val versionCode: Long,
    public val buildVariant: String,
    public val packageName: String,
    public val deviceModel: String,
    public val androidVersion: String,
    public val apiLevel: Int,
    public val manufacturer: String,
    public val screenSize: String,
    public val orientation: String?,
)

public data class TimelineSection(
    public val totalEvents: Int,
    public val groupedEvents: Int,
    public val issueReferences: Int,
    public val events: List<Map<String, String>>,
)

public data class DiagnosticsSection(
    public val issueCount: Int,
    public val openIssueCount: Int,
    public val issues: List<Map<String, String>>,
)

public data class HealthSection(
    public val overallScore: Int,
    public val performanceScore: Int,
    public val memoryScore: Int,
    public val networkScore: Int,
    public val databaseScore: Int,
    public val composeScore: Int,
    public val timestampMillis: Long,
)

public data class SessionReport(
    public val sessionId: String,
    public val metadata: SessionMetadata,
    public val generatedAtMillis: Long,
    public val durationMillis: Long,
    public val timeline: TimelineSection?,
    public val diagnostics: DiagnosticsSection?,
    public val healthReport: HealthSection?,
    public val collectorSummaries: Map<String, Map<String, String>>,
    public val analyticsSummaries: Map<String, Map<String, String>>,
    public val deviceInformation: Map<String, String>,
    public val applicationInformation: Map<String, String>,
    public val configuration: Map<String, String>,
    public val buildInformation: Map<String, String>,
    public val detailedPayloads: Map<String, List<Map<String, String>>> = emptyMap(),
)
