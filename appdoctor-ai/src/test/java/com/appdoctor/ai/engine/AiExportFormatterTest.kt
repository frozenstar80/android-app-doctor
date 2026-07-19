package com.appdoctor.ai.engine

import com.appdoctor.ai.AiAnalysis
import com.appdoctor.ai.AiResponse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiExportFormatterTest {
    @Test
    fun `serializes analysis to markdown and json`() {
        val response = AiResponse(
            sessionId = "s1",
            provider = "local",
            model = "stub",
            request = null,
            analysis = AiAnalysis(
                executiveSummary = "summary",
                performanceFindings = listOf("f1"),
                rootCauseCandidates = listOf("r1"),
                recommendations = listOf("rec1"),
                optimizationOpportunities = listOf("o1"),
                riskAssessment = "low",
                confidence = 80,
                actionItems = listOf("a1"),
                nextInvestigationSteps = listOf("n1"),
            ),
            rawResponse = null,
            error = null,
            generatedAtMillis = 1L,
            latencyMillis = 1L,
            fromCache = false,
        )
        val formatter = AiExportFormatter()
        assertTrue(formatter.toMarkdown(response).contains("# AppDoctor AI Analysis"))
        assertTrue(formatter.toJson(response).contains("\"sessionId\":\"s1\""))
    }
}
