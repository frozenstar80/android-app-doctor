package com.appdoctor.ai

import com.appdoctor.ai.engine.AiCache
import com.appdoctor.ai.engine.AiEngine
import com.appdoctor.ai.engine.AiHistoryRepository
import com.appdoctor.ai.sanitize.CompositeReportSanitizer
import org.junit.Assert.assertEquals
import org.junit.Test

class AiProviderAbstractionTest {
    @Test
    fun `engine works with any AiProvider implementation`() = kotlinx.coroutines.test.runTest {
        val provider = object : AiProvider {
            override suspend fun analyze(report: com.appdoctor.session.model.SessionReport): AiResponse = AiResponse(
                sessionId = report.sessionId,
                provider = "custom-test",
                model = "t1",
                request = null,
                analysis = AiAnalysis(
                    executiveSummary = "ok",
                    performanceFindings = listOf("finding"),
                    rootCauseCandidates = listOf("cause"),
                    recommendations = listOf("rec"),
                    optimizationOpportunities = listOf("opt"),
                    riskAssessment = "low",
                    confidence = 80,
                    actionItems = listOf("do"),
                    nextInvestigationSteps = listOf("next"),
                ),
                rawResponse = null,
                error = null,
                generatedAtMillis = 1L,
                latencyMillis = 1L,
                fromCache = false,
            )
        }
        val engine = AiEngine(
            configuration = AiConfiguration(
                enableAi = true,
                provider = "custom",
                apiKey = null,
                baseUrl = null,
                model = null,
                temperature = 0.2,
                timeoutMillis = 1000L,
                cacheEnabled = true,
                cacheSize = 5,
                localOnly = false,
            ),
            providerResolver = { provider },
            cache = AiCache(5),
            historyRepository = AiHistoryRepository(5),
            sanitizer = CompositeReportSanitizer(emptyList()),
        )
        val response = engine.analyze(sampleReport())
        assertEquals(true, response.isSuccess)
        assertEquals("custom-test", response.provider)
    }
}
