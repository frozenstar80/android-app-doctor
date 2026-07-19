package com.appdoctor.ai

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
