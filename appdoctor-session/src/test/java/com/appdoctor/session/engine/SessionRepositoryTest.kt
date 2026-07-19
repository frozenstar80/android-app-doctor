package com.appdoctor.session.engine

import com.appdoctor.session.model.SessionMetadata
import com.appdoctor.session.model.SessionReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SessionRepositoryTest {
    @Test
    fun `keeps bounded newest-first reports`() {
        val repository = SessionRepository(maximumStoredReports = 2)
        repository.save(report("a"))
        repository.save(report("b"))
        repository.save(report("c"))

        val all = repository.all()
        assertEquals(2, all.size)
        assertEquals("c", all[0].sessionId)
        assertEquals("b", all[1].sessionId)
        assertNotNull(repository.latest())
    }

    private fun report(id: String): SessionReport = SessionReport(
        sessionId = id,
        metadata = SessionMetadata(
            sessionId = id,
            startTimeMillis = 0L,
            endTimeMillis = 10L,
            durationMillis = 10L,
            appVersion = "1.0",
            versionCode = 1L,
            buildVariant = "DEBUG",
            packageName = "pkg",
            deviceModel = "model",
            androidVersion = "15",
            apiLevel = 35,
            manufacturer = "man",
            screenSize = "1x1",
            orientation = "PORTRAIT",
        ),
        generatedAtMillis = 10L,
        durationMillis = 10L,
        timeline = null,
        diagnostics = null,
        healthReport = null,
        collectorSummaries = emptyMap(),
        analyticsSummaries = emptyMap(),
        deviceInformation = emptyMap(),
        applicationInformation = emptyMap(),
        configuration = emptyMap(),
        buildInformation = emptyMap(),
    )
}
