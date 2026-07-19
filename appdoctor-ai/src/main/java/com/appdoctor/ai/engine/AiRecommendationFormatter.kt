package com.appdoctor.ai.engine

public class AiRecommendationFormatter {
    public fun actionItems(recommendations: List<String>): List<String> =
        recommendations.map { "Validate: $it" }

    public fun nextSteps(rootCauseCandidates: List<String>): List<String> =
        rootCauseCandidates.map { "Collect additional evidence for: $it" }
}
