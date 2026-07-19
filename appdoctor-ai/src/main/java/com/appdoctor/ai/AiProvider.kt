package com.appdoctor.ai

import com.appdoctor.session.model.SessionReport

/**
 * Contract for AI providers that transform a [SessionReport] into an [AiResponse].
 *
 * Implementations should be thread-safe because analysis can be triggered from background
 * coroutine contexts.
 */
public interface AiProvider {
    /**
     * Runs AI analysis for the given [report].
     *
     * @param report normalized AppDoctor session payload.
     * @return provider response containing either structured analysis or an error.
     */
    public suspend fun analyze(
        report: SessionReport,
    ): AiResponse
}
