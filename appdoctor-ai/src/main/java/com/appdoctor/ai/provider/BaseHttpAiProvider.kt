package com.appdoctor.ai.provider

import com.appdoctor.ai.AiConfiguration
import com.appdoctor.ai.AiError
import com.appdoctor.ai.AiErrorType
import com.appdoctor.ai.AiProvider
import com.appdoctor.ai.AiResponse
import com.appdoctor.ai.engine.AiPromptBuilder
import com.appdoctor.ai.engine.AiSessionAnalyzer
import com.appdoctor.session.model.SessionReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

public abstract class BaseHttpAiProvider(
    private val providerName: String,
    protected val configuration: AiConfiguration,
    private val promptBuilder: AiPromptBuilder = AiPromptBuilder(),
    private val analyzer: AiSessionAnalyzer = AiSessionAnalyzer(),
) : AiProvider {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(configuration.timeoutMillis, TimeUnit.MILLISECONDS)
        .readTimeout(configuration.timeoutMillis, TimeUnit.MILLISECONDS)
        .writeTimeout(configuration.timeoutMillis, TimeUnit.MILLISECONDS)
        .build()

    final override suspend fun analyze(report: SessionReport): AiResponse = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val model = resolveModel()
        val request = promptBuilder.build(
            report = report,
            config = configuration,
            provider = providerName,
            model = model,
        )
        val apiKey = configuration.apiKey.orEmpty()
        if (apiKey.isBlank()) {
            return@withContext failed(
                report = report,
                model = model,
                start = start,
                type = AiErrorType.CONFIGURATION,
                message = "Missing API key for $providerName provider.",
                requestPrompt = request.prompt,
            )
        }
        val endpoint = resolveEndpoint(model)
        val body = requestBody(request.prompt, model)
        val httpRequest = Request.Builder()
            .url(endpoint)
            .post(body.toRequestBody("application/json".toMediaType()))
            .headers(headers(apiKey))
            .build()
        try {
            client.newCall(httpRequest).execute().use { response ->
                val payload = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errorType = when (response.code) {
                        401, 403 -> AiErrorType.AUTHENTICATION
                        408 -> AiErrorType.TIMEOUT
                        429 -> AiErrorType.RATE_LIMIT
                        in 500..599 -> AiErrorType.NETWORK
                        else -> AiErrorType.INVALID_RESPONSE
                    }
                    return@withContext failed(
                        report = report,
                        model = model,
                        start = start,
                        type = errorType,
                        message = "HTTP ${response.code} from $providerName.",
                        requestPrompt = request.prompt,
                        raw = payload,
                    )
                }
                val text = extractText(payload)
                if (text.isBlank()) {
                    return@withContext failed(
                        report = report,
                        model = model,
                        start = start,
                        type = AiErrorType.INVALID_RESPONSE,
                        message = "Provider returned an empty response.",
                        requestPrompt = request.prompt,
                        raw = payload,
                    )
                }
                val analysis = analyzer.parseOrFallback(text, report)
                return@withContext AiResponse(
                    sessionId = report.sessionId,
                    provider = providerName,
                    model = model,
                    request = request.copy(prompt = request.prompt),
                    analysis = analysis,
                    rawResponse = text,
                    error = null,
                    generatedAtMillis = System.currentTimeMillis(),
                    latencyMillis = System.currentTimeMillis() - start,
                    fromCache = false,
                )
            }
        } catch (_: java.net.SocketTimeoutException) {
            failed(
                report = report,
                model = model,
                start = start,
                type = AiErrorType.TIMEOUT,
                message = "AI request timed out.",
                requestPrompt = request.prompt,
            )
        } catch (_: IOException) {
            failed(
                report = report,
                model = model,
                start = start,
                type = AiErrorType.NETWORK,
                message = "AI request failed due to network error.",
                requestPrompt = request.prompt,
            )
        } catch (t: Throwable) {
            failed(
                report = report,
                model = model,
                start = start,
                type = AiErrorType.UNKNOWN,
                message = t.message ?: "Unexpected AI error.",
                requestPrompt = request.prompt,
            )
        }
    }

    protected abstract fun resolveModel(): String

    protected abstract fun resolveEndpoint(model: String): String

    protected abstract fun requestBody(prompt: String, model: String): String

    protected abstract fun headers(apiKey: String): okhttp3.Headers

    protected abstract fun extractText(responseBody: String): String

    private fun failed(
        report: SessionReport,
        model: String,
        start: Long,
        type: AiErrorType,
        message: String,
        requestPrompt: String,
        raw: String? = null,
    ): AiResponse = AiResponse(
        sessionId = report.sessionId,
        provider = providerName,
        model = model,
        request = com.appdoctor.ai.AiRequest(
            sessionId = report.sessionId,
            prompt = requestPrompt,
            provider = providerName,
            model = model,
            temperature = configuration.temperature,
            timeoutMillis = configuration.timeoutMillis,
        ),
        analysis = null,
        rawResponse = raw,
        error = AiError(type = type, message = message),
        generatedAtMillis = System.currentTimeMillis(),
        latencyMillis = System.currentTimeMillis() - start,
        fromCache = false,
    )
}
