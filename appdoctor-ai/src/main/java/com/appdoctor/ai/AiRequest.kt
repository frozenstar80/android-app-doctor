package com.appdoctor.ai

public data class AiRequest(
    public val sessionId: String,
    public val prompt: String,
    public val provider: String,
    public val model: String,
    public val temperature: Double,
    public val timeoutMillis: Long,
)
