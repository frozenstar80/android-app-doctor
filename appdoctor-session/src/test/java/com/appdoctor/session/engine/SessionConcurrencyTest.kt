package com.appdoctor.session.engine

import com.appdoctor.session.model.SessionMetadata
import com.appdoctor.session.model.SessionReport
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class SessionConcurrencyTest {
    @Test
    fun `repository remains bounded under concurrent writes`() {
        val repository = SessionRepository(maximumStoredReports = 10)
        val pool = Executors.newFixedThreadPool(6)
        val latch = CountDownLatch(100)

        repeat(100) { index ->
            pool.execute {
                repository.save(report("session-$index"))
                latch.countDown()
            }
        }
        latch.await()
        pool.shutdown()

        assertTrue(repository.all().size <= 10)
    }

    private fun report(id: String): SessionReport = SessionReport(
        sessionId = id,
        metadata = SessionMetadata(
            sessionId = id,
            startTimeMillis = 0L,
            endTimeMillis = 1L,
            durationMillis = 1L,
            appVersion = "1.0",
            versionCode = 1L,
            buildVariant = "DEBUG",
            packageName = "pkg",
            deviceModel = "model",
            androidVersion = "15",
            apiLevel = 35,
            manufacturer = "man",
            screenSize = "1x1",
            orientation = null,
        ),
        generatedAtMillis = 1L,
        durationMillis = 1L,
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
