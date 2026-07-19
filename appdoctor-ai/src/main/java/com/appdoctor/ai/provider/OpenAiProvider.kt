package com.appdoctor.ai.provider

import com.appdoctor.ai.AiConfiguration
import okhttp3.Headers

public class OpenAiProvider(
    configuration: AiConfiguration,
) : BaseHttpAiProvider(
    providerName = "openai",
    configuration = configuration,
) {
    override fun resolveModel(): String = configuration.model ?: "gpt-4o-mini"

    override fun resolveEndpoint(model: String): String =
        configuration.baseUrl?.ifBlank { null } ?: "https://api.openai.com/v1/chat/completions"

    override fun requestBody(prompt: String, model: String): String = """
        {
          "model":"$model",
          "temperature":${configuration.temperature},
          "messages":[
            {"role":"system","content":"You are an AppDoctor diagnostics analyst."},
            {"role":"user","content":${prompt.quoteJson()}}
          ]
        }
    """.trimIndent()

    override fun headers(apiKey: String): Headers = Headers.Builder()
        .add("Authorization", "Bearer $apiKey")
        .add("Content-Type", "application/json")
        .build()

    override fun extractText(responseBody: String): String =
        Regex("\"content\"\\s*:\\s*\"(.*?)\"", setOf(RegexOption.DOT_MATCHES_ALL))
            .find(responseBody)
            ?.groupValues
            ?.getOrNull(1)
            ?.unescapeJson()
            .orEmpty()
}

private fun String.quoteJson(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\""

private fun String.unescapeJson(): String = replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\")
