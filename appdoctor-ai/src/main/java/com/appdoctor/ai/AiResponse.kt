package com.appdoctor.ai

/**
 * Normalized AI provider failure categories.
 */
public enum class AiErrorType {
    AUTHENTICATION,
    NETWORK,
    TIMEOUT,
    INVALID_RESPONSE,
    RATE_LIMIT,
    CONFIGURATION,
    UNKNOWN,
}

/**
 * Structured provider failure payload.
 *
 * @property type normalized failure category.
 * @property message user-readable failure message.
 */
public data class AiError(
    public val type: AiErrorType,
    public val message: String,
)

/**
 * Result of an AI analysis attempt for a session report.
 *
 * @property sessionId session identifier.
 * @property provider provider identifier used for this run.
 * @property model model identifier used for this run.
 * @property request normalized request payload, when available.
 * @property analysis structured analysis result on success.
 * @property rawResponse raw provider text payload when available.
 * @property error failure payload when analysis did not succeed.
 * @property generatedAtMillis generation timestamp in epoch milliseconds.
 * @property latencyMillis end-to-end provider latency.
 * @property fromCache true when served from cache.
 */
public data class AiResponse(
    public val sessionId: String,
    public val provider: String,
    public val model: String,
    public val request: AiRequest?,
    public val analysis: AiAnalysis?,
    public val rawResponse: String?,
    public val error: AiError?,
    public val generatedAtMillis: Long,
    public val latencyMillis: Long,
    public val fromCache: Boolean,
) {
    /**
     * Returns true when an analysis payload exists and no error was recorded.
     */
    public val isSuccess: Boolean get() = analysis != null && error == null
}
