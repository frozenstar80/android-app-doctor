package com.appdoctor.diagnostics.model

public enum class IssueCategory {
    PERFORMANCE,
    MEMORY,
    NETWORK,
    DATABASE,
    COMPOSE,
    CROSS_COLLECTOR,
}

public enum class IssueSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL,
}

public enum class IssueStatus {
    OPEN,
    RESOLVED,
    IGNORED,
}

public data class IssueRecommendation(
    public val problem: String,
    public val reason: String,
    public val recommendation: String,
    public val expectedImpact: String,
)

/**
 * Diagnosed runtime issue computed deterministically from observed metrics.
 */
public data class DiagnosticIssue(
    public val id: String,
    public val title: String,
    public val description: String,
    public val category: IssueCategory,
    public val severity: IssueSeverity,
    public val confidence: Int,
    public val timestampMillis: Long,
    public val collectorIds: List<String>,
    public val recommendation: IssueRecommendation,
    public val documentationLink: String?,
    public val status: IssueStatus,
)
