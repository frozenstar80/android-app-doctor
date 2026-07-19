package com.appdoctor.ai

public enum class AiErrorType {
    AUTHENTICATION,
    NETWORK,
    TIMEOUT,
    INVALID_RESPONSE,
    RATE_LIMIT,
    CONFIGURATION,
    UNKNOWN,
}

public data class AiError(
    public val type: AiErrorType,
    public val message: String,
)

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
    public val isSuccess: Boolean get() = analysis != null && error == null
}
