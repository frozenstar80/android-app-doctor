package com.appdoctor.ai.provider

import com.appdoctor.ai.AiConfiguration
import okhttp3.Headers

public class GeminiProvider(
    configuration: AiConfiguration,
) : BaseHttpAiProvider(
    providerName = "gemini",
    configuration = configuration,
) {
    override fun resolveModel(): String = configuration.model ?: "gemini-2.0-flash"

    override fun resolveEndpoint(model: String): String {
        val base = configuration.baseUrl?.ifBlank { null } ?: "https://generativelanguage.googleapis.com/v1beta/models"
        return "$base/$model:generateContent?key=${configuration.apiKey.orEmpty()}"
    }

    override fun requestBody(prompt: String, model: String): String = """
        {
          "generationConfig": {
            "temperature": ${configuration.temperature}
          },
          "contents": [{
            "parts": [{"text": ${prompt.quoteJson()}}]
          }]
        }
    """.trimIndent()

    override fun headers(apiKey: String): Headers = Headers.Builder()
        .add("Content-Type", "application/json")
        .build()

    override fun extractText(responseBody: String): String =
        Regex("\"text\"\\s*:\\s*\"(.*?)\"", setOf(RegexOption.DOT_MATCHES_ALL))
            .find(responseBody)
            ?.groupValues
            ?.getOrNull(1)
            ?.unescapeJson()
            .orEmpty()
}

private fun String.quoteJson(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\""

private fun String.unescapeJson(): String = replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\")
