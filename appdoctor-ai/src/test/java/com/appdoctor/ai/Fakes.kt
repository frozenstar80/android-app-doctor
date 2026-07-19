package com.appdoctor.ai

import com.appdoctor.session.model.DiagnosticsSection
import com.appdoctor.session.model.HealthSection
import com.appdoctor.session.model.SessionMetadata
import com.appdoctor.session.model.SessionReport
import com.appdoctor.session.model.TimelineSection

internal fun sampleReport(sessionId: String = "session-1"): SessionReport = SessionReport(
    sessionId = sessionId,
    metadata = SessionMetadata(
        sessionId = sessionId,
        startTimeMillis = 1L,
        endTimeMillis = 10L,
        durationMillis = 9L,
        appVersion = "1.0.0",
        versionCode = 1L,
        buildVariant = "DEBUG",
        packageName = "com.example.app",
        deviceModel = "Pixel",
        androidVersion = "15",
        apiLevel = 35,
        manufacturer = "Google",
        screenSize = "1080x2400",
        orientation = "PORTRAIT",
    ),
    generatedAtMillis = 10L,
    durationMillis = 9L,
    timeline = TimelineSection(
        totalEvents = 2,
        groupedEvents = 1,
        issueReferences = 1,
        events = listOf(mapOf("url" to "https://example.com?q=secret", "Authorization" to "Bearer token")),
    ),
    diagnostics = DiagnosticsSection(
        issueCount = 1,
        openIssueCount = 1,
        issues = listOf(mapOf("title" to "High memory usage", "severity" to "WARNING", "category" to "MEMORY")),
    ),
    healthReport = HealthSection(
        overallScore = 72,
        performanceScore = 71,
        memoryScore = 63,
        networkScore = 80,
        databaseScore = 76,
        composeScore = 79,
        timestampMillis = 10L,
    ),
    collectorSummaries = mapOf("network" to mapOf("cookie" to "abc", "avgMs" to "123")),
    analyticsSummaries = emptyMap(),
    deviceInformation = mapOf("androidId" to "A-1", "model" to "Pixel"),
    applicationInformation = mapOf("packageName" to "com.example.app"),
    configuration = mapOf("enableSessionReports" to "true"),
    buildInformation = mapOf("buildVariant" to "DEBUG"),
)
