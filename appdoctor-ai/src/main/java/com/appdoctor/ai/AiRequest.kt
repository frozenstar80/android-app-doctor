package com.appdoctor.ai

/**
 * Immutable AI request payload used for provider execution and diagnostics export.
 *
 * @property sessionId session identifier associated with the source report.
 * @property prompt sanitized prompt text submitted to the provider.
 * @property provider provider identifier (for example `openai` or `gemini`).
 * @property model model name sent to the provider.
 * @property temperature configured model temperature.
 * @property timeoutMillis request timeout in milliseconds.
 */
public data class AiRequest(
    public val sessionId: String,
    public val prompt: String,
    public val provider: String,
    public val model: String,
    public val temperature: Double,
    public val timeoutMillis: Long,
)
