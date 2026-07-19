package com.appdoctor.ai.provider

import com.appdoctor.ai.AiConfiguration
import okhttp3.Headers

public class CustomEndpointProvider(
    configuration: AiConfiguration,
) : BaseHttpAiProvider(
    providerName = "custom",
    configuration = configuration,
) {
    override fun resolveModel(): String = configuration.model ?: "custom-model"

    override fun resolveEndpoint(model: String): String =
        configuration.baseUrl?.ifBlank { null } ?: "http://localhost:8080/analyze"

    override fun requestBody(prompt: String, model: String): String = """
        {
          "model":"$model",
          "temperature":${configuration.temperature},
          "prompt":${prompt.quoteJson()}
        }
    """.trimIndent()

    override fun headers(apiKey: String): Headers = Headers.Builder()
        .add("Content-Type", "application/json")
        .apply {
            if (apiKey.isNotBlank()) add("Authorization", "Bearer $apiKey")
        }
        .build()

    override fun extractText(responseBody: String): String {
        val analysis = Regex("\"analysis\"\\s*:\\s*\"(.*?)\"", setOf(RegexOption.DOT_MATCHES_ALL))
            .find(responseBody)
            ?.groupValues
            ?.getOrNull(1)
            ?.unescapeJson()
        if (!analysis.isNullOrBlank()) return analysis
        return Regex("\"text\"\\s*:\\s*\"(.*?)\"", setOf(RegexOption.DOT_MATCHES_ALL))
            .find(responseBody)
            ?.groupValues
            ?.getOrNull(1)
            ?.unescapeJson()
            .orEmpty()
    }
}

private fun String.quoteJson(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\""

private fun String.unescapeJson(): String = replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\")
