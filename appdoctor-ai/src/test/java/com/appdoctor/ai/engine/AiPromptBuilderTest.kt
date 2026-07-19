package com.appdoctor.ai.engine

import com.appdoctor.ai.AiConfiguration
import com.appdoctor.ai.sampleReport
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPromptBuilderTest {
    @Test
    fun `builds compact prompt from session report`() {
        val prompt = AiPromptBuilder().build(
            report = sampleReport(),
            config = AiConfiguration(
                enableAi = true,
                provider = "local",
                apiKey = null,
                baseUrl = null,
                model = "local",
                temperature = 0.1,
                timeoutMillis = 1_000L,
                cacheEnabled = true,
                cacheSize = 5,
                localOnly = true,
            ),
            provider = "local",
            model = "local",
        ).prompt
        assertTrue(prompt.contains("Health Report"))
        assertTrue(prompt.contains("Collector summaries"))
        assertTrue(prompt.contains("App metadata"))
        assertTrue(prompt.contains("Device metadata"))
    }
}
