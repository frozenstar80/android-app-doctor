package com.appdoctor.network.okhttp

import com.appdoctor.network.repository.InMemoryNetworkRequestRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class AppDoctorNetworkInterceptorTest {

    @Test
    fun `captures request and response details`() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"ok":true}"""),
        )
        server.start()
        try {
            val repository = InMemoryNetworkRequestRepository(maxRequests = 10)
            val interceptor = AppDoctorNetworkInterceptor(
                repository = repository,
                captureRequestBody = true,
                captureResponseBody = true,
                maxCapturedBodyBytes = 256 * 1024L,
                enabled = AtomicBoolean(true),
                requestId = AtomicLong(0L),
            )
            val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
            val request = Request.Builder()
                .url(server.url("/users?id=42"))
                .post("""{"name":"Ana"}""".toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                assertTrue(response.isSuccessful)
            }

            val record = repository.requests.value.single()
            assertEquals("POST", record.method)
            assertEquals(200, record.statusCode)
            assertTrue(record.success)
            assertTrue(record.url.contains("/users?id=42"))
            assertEquals("42", record.queryParameters.single().value)
            assertNotNull(record.requestBody?.text)
            assertNotNull(record.responseBody?.text)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `respects max body capture bytes`() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/plain")
                .setBody("x".repeat(5000)),
        )
        server.start()
        try {
            val repository = InMemoryNetworkRequestRepository(maxRequests = 10)
            val interceptor = AppDoctorNetworkInterceptor(
                repository = repository,
                captureRequestBody = true,
                captureResponseBody = true,
                maxCapturedBodyBytes = 1024L,
                enabled = AtomicBoolean(true),
                requestId = AtomicLong(0L),
            )
            val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
            client.newCall(Request.Builder().url(server.url("/big")).build()).execute().close()

            val body = repository.requests.value.single().responseBody
            assertNotNull(body)
            assertTrue(body!!.truncated)
            assertEquals(1024, body.text!!.length)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `captures failures`() {
        val repository = InMemoryNetworkRequestRepository(maxRequests = 10)
        val interceptor = AppDoctorNetworkInterceptor(
            repository = repository,
            captureRequestBody = false,
            captureResponseBody = false,
            maxCapturedBodyBytes = 1024L,
            enabled = AtomicBoolean(true),
            requestId = AtomicLong(0L),
        )
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val request = Request.Builder().url("http://127.0.0.1:1/fail").build()

        runCatching { client.newCall(request).execute() }.onSuccess { it.close() }

        val record = repository.requests.value.single()
        assertFalse(record.success)
        assertEquals(null, record.statusCode)
        assertNotNull(record.failureMessage)
    }
}
