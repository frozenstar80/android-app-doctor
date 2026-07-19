package com.appdoctor.ai

import com.appdoctor.session.model.SessionReport

public interface AiProvider {
    public suspend fun analyze(
        report: SessionReport,
    ): AiResponse
}
