package com.appdoctor.ai

import com.appdoctor.core.AppDoctorConfig

public data class AiConfiguration(
    public val enableAi: Boolean,
    public val provider: String?,
    public val apiKey: String?,
    public val baseUrl: String?,
    public val model: String?,
    public val temperature: Double,
    public val timeoutMillis: Long,
    public val cacheEnabled: Boolean,
    public val cacheSize: Int,
    public val localOnly: Boolean,
) {
    public val providerConfigured: Boolean get() = !provider.isNullOrBlank()

    public companion object {
        public fun from(config: AppDoctorConfig): AiConfiguration = AiConfiguration(
            enableAi = config.enableAi,
            provider = config.aiProvider?.trim()?.lowercase(),
            apiKey = config.aiApiKey,
            baseUrl = config.aiBaseUrl,
            model = config.aiModel,
            temperature = config.aiTemperature.coerceIn(0.0, 2.0),
            timeoutMillis = config.aiTimeoutMillis.coerceAtLeast(1_000L),
            cacheEnabled = config.aiCacheEnabled,
            cacheSize = config.aiCacheSize.coerceAtLeast(1),
            localOnly = config.aiLocalOnly,
        )
    }
}
