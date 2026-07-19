package com.appdoctor.ai.provider

import com.appdoctor.ai.AiProvider
import com.appdoctor.ai.AiRequest
import com.appdoctor.ai.AiResponse
import com.appdoctor.ai.engine.AiPromptBuilder
import com.appdoctor.ai.engine.AiSessionAnalyzer
import com.appdoctor.session.model.SessionReport

public class LocalModelProvider(
    private val promptBuilder: AiPromptBuilder = AiPromptBuilder(),
    private val analyzer: AiSessionAnalyzer = AiSessionAnalyzer(),
) : AiProvider {
    override suspend fun analyze(report: SessionReport): AiResponse {
        val start = System.currentTimeMillis()
        val request = AiRequest(
            sessionId = report.sessionId,
            prompt = promptBuilder.build(
                report = report,
                config = com.appdoctor.ai.AiConfiguration(
                    enableAi = true,
                    provider = "local",
                    apiKey = null,
                    baseUrl = null,
                    model = "local-stub",
                    temperature = 0.0,
                    timeoutMillis = 1_000L,
                    cacheEnabled = true,
                    cacheSize = 1,
                    localOnly = true,
                ),
                provider = "local",
                model = "local-stub",
            ).prompt,
            provider = "local",
            model = "local-stub",
            temperature = 0.0,
            timeoutMillis = 1_000L,
        )
        val analysis = analyzer.fallback(report)
        return AiResponse(
            sessionId = report.sessionId,
            provider = "local",
            model = "local-stub",
            request = request,
            analysis = analysis,
            rawResponse = null,
            error = null,
            generatedAtMillis = System.currentTimeMillis(),
            latencyMillis = System.currentTimeMillis() - start,
            fromCache = false,
        )
    }
}
