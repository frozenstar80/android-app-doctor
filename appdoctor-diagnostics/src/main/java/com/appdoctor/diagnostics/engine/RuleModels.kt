package com.appdoctor.diagnostics.engine

import com.appdoctor.diagnostics.model.IssueCategory
import com.appdoctor.diagnostics.model.IssueSeverity

public data class MemorySample(
    public val timestampMillis: Long,
    public val usagePercent: Float,
    public val nativeAllocatedBytes: Long,
)

public data class FpsSample(
    public val timestampMillis: Long,
    public val averageFps: Float,
)

public data class NetworkSample(
    public val id: Long,
    public val timestampMillis: Long,
    public val latencyMillis: Long,
    public val success: Boolean,
)

public data class DatabaseSample(
    public val id: Long,
    public val timestampMillis: Long,
    public val durationMillis: Double,
    public val success: Boolean,
)

public data class ComposeSample(
    public val timestampMillis: Long,
    public val recompositionRate: Double,
    public val frameDropRate: Double,
)

public data class RuleContext(
    public val nowMillis: Long,
    public val memorySamples: List<MemorySample>,
    public val fpsSamples: List<FpsSample>,
    public val networkSamples: List<NetworkSample>,
    public val databaseSamples: List<DatabaseSample>,
    public val composeSamples: List<ComposeSample>,
)

public data class RuleEvidence(
    public val supportingPoints: Int,
    public val totalPoints: Int,
    public val consistency: Double,
)

public data class RuleMatch(
    public val id: String,
    public val title: String,
    public val description: String,
    public val category: IssueCategory,
    public val severity: IssueSeverity,
    public val collectorIds: List<String>,
    public val documentationLink: String?,
    public val evidence: RuleEvidence,
)
