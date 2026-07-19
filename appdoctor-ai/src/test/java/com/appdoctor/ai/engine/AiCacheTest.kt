package com.appdoctor.ai.engine

import com.appdoctor.ai.AiResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiCacheTest {
    @Test
    fun `evicts least recently used entries`() {
        val cache = AiCache(2)
        cache.put("s1", response("s1"))
        cache.put("s2", response("s2"))
        cache.get("s1")
        cache.put("s3", response("s3"))
        assertEquals("s1", cache.get("s1")?.sessionId)
        assertNull(cache.get("s2"))
    }

    private fun response(session: String): AiResponse = AiResponse(
        sessionId = session,
        provider = "local",
        model = "local",
        request = null,
        analysis = null,
        rawResponse = null,
        error = null,
        generatedAtMillis = 1L,
        latencyMillis = 1L,
        fromCache = false,
    )
}
