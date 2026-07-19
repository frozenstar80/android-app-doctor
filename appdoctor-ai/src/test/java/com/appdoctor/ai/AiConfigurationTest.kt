package com.appdoctor.ai

import com.appdoctor.core.AppDoctorConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiConfigurationTest {
    @Test
    fun `maps additive core config to ai configuration`() {
        val config = AiConfiguration.from(
            AppDoctorConfig(
                enableAi = true,
                aiProvider = "openai",
                aiApiKey = "key",
                aiBaseUrl = "https://example.com",
                aiModel = "model-x",
                aiTemperature = 1.4,
                aiTimeoutMillis = 5000L,
                aiCacheEnabled = false,
                aiCacheSize = 3,
                aiLocalOnly = true,
            ),
        )
        assertTrue(config.enableAi)
        assertEquals("openai", config.provider)
        assertEquals("model-x", config.model)
        assertEquals(false, config.cacheEnabled)
        assertEquals(true, config.localOnly)
    }
}
