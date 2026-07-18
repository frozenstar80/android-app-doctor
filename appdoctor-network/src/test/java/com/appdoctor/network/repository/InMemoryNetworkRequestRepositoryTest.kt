package com.appdoctor.network.repository

import com.appdoctor.network.model.NetworkRequestRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryNetworkRequestRepositoryTest {

    @Test
    fun `keeps only latest max requests`() {
        val repository = InMemoryNetworkRequestRepository(maxRequests = 3)
        repeat(5) { index ->
            repository.add(sampleRecord(id = index.toLong()))
        }
        val ids = repository.requests.value.map { it.id }
        assertEquals(listOf(4L, 3L, 2L), ids)
    }

    @Test
    fun `supports concurrent writers safely`() = runBlocking {
        val repository = InMemoryNetworkRequestRepository(maxRequests = 100)
        (0 until 500).map { index ->
            async(Dispatchers.Default) { repository.add(sampleRecord(id = index.toLong())) }
        }.awaitAll()

        val snapshot = repository.requests.value
        assertEquals(100, snapshot.size)
        assertEquals(100, snapshot.map { it.id }.distinct().size)
        assertTrue(snapshot.all { it.id in 0L..499L })
    }

    private fun sampleRecord(id: Long): NetworkRequestRecord = NetworkRequestRecord(
        id = id,
        timestampMillis = id,
        method = "GET",
        url = "https://example.com/$id",
        queryParameters = emptyList(),
        requestHeaders = emptyList(),
        requestBody = null,
        statusCode = 200,
        responseHeaders = emptyList(),
        responseBody = null,
        responseTimeMillis = 10L,
        contentLength = 123L,
        success = true,
        failureMessage = null,
    )
}
