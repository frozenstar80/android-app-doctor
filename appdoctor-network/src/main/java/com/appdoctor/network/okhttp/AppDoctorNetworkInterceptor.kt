package com.appdoctor.network.okhttp

import com.appdoctor.network.model.CapturedBody
import com.appdoctor.network.model.HttpHeader
import com.appdoctor.network.model.NetworkRequestRecord
import com.appdoctor.network.model.QueryParameter
import com.appdoctor.network.repository.NetworkRequestRepository
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSink
import okio.Timeout
import okio.buffer
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

/**
 * OkHttp interceptor that captures request/response metadata for AppDoctor.
 */
public class AppDoctorNetworkInterceptor internal constructor(
    private val repository: NetworkRequestRepository,
    private val captureRequestBody: Boolean,
    private val captureResponseBody: Boolean,
    private val maxCapturedBodyBytes: Long,
    private val enabled: AtomicBoolean,
    private val requestId: AtomicLong,
    private val clock: Clock,
) : Interceptor {

    public constructor(
        repository: NetworkRequestRepository,
        captureRequestBody: Boolean,
        captureResponseBody: Boolean,
        maxCapturedBodyBytes: Long,
        enabled: AtomicBoolean = AtomicBoolean(true),
        requestId: AtomicLong = AtomicLong(0L),
    ) : this(
        repository = repository,
        captureRequestBody = captureRequestBody,
        captureResponseBody = captureResponseBody,
        maxCapturedBodyBytes = maxCapturedBodyBytes.coerceAtLeast(1L),
        enabled = enabled,
        requestId = requestId,
        clock = RealClock,
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!enabled.get()) return chain.proceed(chain.request())

        val request = chain.request()
        val startedAtMillis = clock.nowMillis()
        val startedAtNanos = clock.nanoTime()
        val capturedRequestBody = if (captureRequestBody) captureRequestBody(request) else null

        var response: Response? = null
        var failure: IOException? = null
        try {
            response = chain.proceed(request)
            return response
        } catch (io: IOException) {
            failure = io
            throw io
        } finally {
            val durationMillis = ((clock.nanoTime() - startedAtNanos) / 1_000_000L).coerceAtLeast(0L)
            val capturedResponseBody = if (captureResponseBody) captureResponseBody(response) else null
            repository.add(
                NetworkRequestRecord(
                    id = requestId.incrementAndGet(),
                    timestampMillis = startedAtMillis,
                    method = request.method,
                    url = request.url.toString(),
                    queryParameters = request.url.queryParameterNames.flatMap { key ->
                        request.url.queryParameterValues(key).map { value ->
                            QueryParameter(name = key, value = value)
                        }
                    },
                    requestHeaders = request.headers.names().flatMap { name ->
                        request.headers.values(name).map { value -> HttpHeader(name, value) }
                    },
                    requestBody = capturedRequestBody,
                    statusCode = response?.code,
                    responseHeaders = response?.headers?.names()?.flatMap { name ->
                        response.headers.values(name).map { value -> HttpHeader(name, value) }
                    }.orEmpty(),
                    responseBody = capturedResponseBody,
                    responseTimeMillis = durationMillis,
                    contentLength = response?.body?.contentLength()?.takeIf { it >= 0L },
                    success = response?.isSuccessful == true && failure == null,
                    failureMessage = failure?.message,
                ),
            )
        }
    }

    private fun captureRequestBody(request: Request): CapturedBody? {
        val body = request.body ?: return null
        if (body.isOneShot() || body.isDuplex()) return null
        val collector = LimitedBodyCollector(maxCapturedBodyBytes)
        val bufferedSink: BufferedSink = collector.buffer()
        body.writeTo(bufferedSink)
        bufferedSink.flush()
        bufferedSink.close()
        return createCapturedBody(
            bytes = collector.bytes(),
            totalBytes = body.contentLength().takeIf { it >= 0L },
            contentType = body.contentType()?.toString(),
            truncated = collector.truncated(),
        )
    }

    private fun captureResponseBody(response: Response?): CapturedBody? {
        val body = response?.body ?: return null
        val peekedBody = response.peekBody(maxCapturedBodyBytes + 1L)
        val bytes = peekedBody.bytes()
        val truncated = bytes.size.toLong() > maxCapturedBodyBytes
        val cappedBytes = if (truncated) bytes.copyOf(maxCapturedBodyBytes.toInt()) else bytes
        return createCapturedBody(
            bytes = cappedBytes,
            totalBytes = body.contentLength().takeIf { it >= 0L },
            contentType = body.contentType()?.toString(),
            truncated = truncated,
        )
    }

    private fun createCapturedBody(
        bytes: ByteArray,
        totalBytes: Long?,
        contentType: String?,
        truncated: Boolean,
    ): CapturedBody {
        val binary = isBinary(contentType, bytes)
        val text = if (binary) null else decodeText(contentType, bytes)
        return CapturedBody(
            contentType = contentType,
            contentLength = totalBytes,
            text = text,
            isBinary = binary,
            truncated = truncated,
        )
    }

    private fun decodeText(contentType: String?, bytes: ByteArray): String {
        val charsetName = contentType
            ?.split(";")
            ?.map { it.trim() }
            ?.firstOrNull { it.startsWith("charset=", ignoreCase = true) }
            ?.substringAfter("=")
            ?.trim()
            ?.ifEmpty { null }
        val charset = charsetName
            ?.runCatching { Charset.forName(this) }
            ?.getOrNull()
            ?: StandardCharsets.UTF_8
        return bytes.toString(charset)
    }

    private fun isBinary(contentType: String?, bytes: ByteArray): Boolean {
        val type = contentType?.lowercase().orEmpty()
        if (type.startsWith("text/")) return false
        if ("json" in type || "xml" in type || "x-www-form-urlencoded" in type) return false
        return bytes.any { byte ->
            val value = byte.toInt() and 0xFF
            value == 0 || value in 0x01..0x08 || value in 0x0E..0x1F
        }
    }

    private class LimitedBodyCollector(private val limit: Long) : okio.Sink {
        private val capture = Buffer()
        private var totalBytes: Long = 0L

        override fun write(source: Buffer, byteCount: Long) {
            totalBytes += byteCount
            if (capture.size < limit) {
                val remaining = limit - capture.size
                val copySize = min(remaining, byteCount)
                source.copyTo(capture, 0L, copySize)
            }
            source.skip(byteCount)
        }

        override fun flush() = Unit

        override fun timeout(): Timeout = Timeout.NONE

        override fun close() = Unit

        fun bytes(): ByteArray = capture.clone().readByteArray()

        fun truncated(): Boolean = totalBytes > limit
    }

    internal interface Clock {
        fun nowMillis(): Long
        fun nanoTime(): Long
    }

    private object RealClock : Clock {
        override fun nowMillis(): Long = System.currentTimeMillis()
        override fun nanoTime(): Long = System.nanoTime()
    }
}
