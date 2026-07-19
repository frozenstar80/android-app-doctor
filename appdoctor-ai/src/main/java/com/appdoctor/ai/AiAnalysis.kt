package com.appdoctor.ai

/**
 * Structured AI interpretation of a [com.appdoctor.session.model.SessionReport].
 *
 * This model is deterministic from the SDK perspective: AppDoctor only stores returned text
 * sections and does not mutate host application state.
 */
public data class AiAnalysis(
    public val executiveSummary: String,
    public val performanceFindings: List<String>,
    public val rootCauseCandidates: List<String>,
    public val recommendations: List<String>,
    public val optimizationOpportunities: List<String>,
    public val riskAssessment: String,
    public val confidence: Int,
    public val actionItems: List<String>,
    public val nextInvestigationSteps: List<String>,
)
