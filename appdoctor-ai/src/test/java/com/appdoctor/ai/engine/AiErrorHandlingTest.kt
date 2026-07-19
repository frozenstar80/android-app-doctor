package com.appdoctor.ai.engine

import com.appdoctor.ai.AiConfiguration
import com.appdoctor.ai.AiErrorType
import com.appdoctor.ai.sanitize.CompositeReportSanitizer
import com.appdoctor.ai.sampleReport
import org.junit.Assert.assertEquals
import org.junit.Test

class AiErrorHandlingTest {
    @Test
    fun `returns configuration error when provider is missing`() = kotlinx.coroutines.test.runTest {
        val engine = AiEngine(
            configuration = AiConfiguration(
                enableAi = true,
                provider = null,
                apiKey = null,
                baseUrl = null,
                model = null,
                temperature = 0.2,
                timeoutMillis = 1_000L,
                cacheEnabled = true,
                cacheSize = 5,
                localOnly = false,
            ),
            providerResolver = { null },
            cache = AiCache(5),
            historyRepository = AiHistoryRepository(5),
            sanitizer = CompositeReportSanitizer(emptyList()),
        )
        val response = engine.analyze(sampleReport())
        assertEquals(AiErrorType.CONFIGURATION, response.error?.type)
        assertEquals("No AI provider configured.", response.error?.message)
    }
}
