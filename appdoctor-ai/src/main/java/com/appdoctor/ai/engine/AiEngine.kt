package com.appdoctor.ai.engine

import com.appdoctor.ai.AiConfiguration
import com.appdoctor.ai.AiError
import com.appdoctor.ai.AiErrorType
import com.appdoctor.ai.AiProvider
import com.appdoctor.ai.AiResponse
import com.appdoctor.ai.sanitize.ReportSanitizer
import com.appdoctor.session.model.SessionReport

public class AiEngine(
    private val configuration: AiConfiguration,
    private val providerResolver: () -> AiProvider?,
    private val cache: AiCache,
    private val historyRepository: AiHistoryRepository,
    private val sanitizer: ReportSanitizer,
) {
    public suspend fun analyze(report: SessionReport, forceRefresh: Boolean = false): AiResponse {
        if (configuration.cacheEnabled && !forceRefresh) {
            cache.get(report.sessionId)?.let { return it.copy(fromCache = true) }
        }
        val provider = providerResolver()
        if (provider == null) {
            return error(report.sessionId, AiErrorType.CONFIGURATION, "No AI provider configured.")
        }
        if (configuration.localOnly && provider !is com.appdoctor.ai.provider.LocalModelProvider) {
            return error(report.sessionId, AiErrorType.CONFIGURATION, "Local-only mode blocks external AI providers.")
        }
        val sanitized = sanitizer.sanitize(report)
        val response = provider.analyze(sanitized)
        if (response.isSuccess) {
            if (configuration.cacheEnabled) cache.put(report.sessionId, response)
            historyRepository.add(response)
        }
        return response
    }

    public fun history(): List<AiResponse> = historyRepository.all()

    public fun latest(): AiResponse? = historyRepository.latest()

    private fun error(sessionId: String, type: AiErrorType, message: String): AiResponse = AiResponse(
        sessionId = sessionId,
        provider = configuration.provider ?: "none",
        model = configuration.model ?: "unknown",
        request = null,
        analysis = null,
        rawResponse = null,
        error = AiError(type = type, message = message),
        generatedAtMillis = System.currentTimeMillis(),
        latencyMillis = 0L,
        fromCache = false,
    )
}
