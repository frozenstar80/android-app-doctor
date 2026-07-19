package com.appdoctor.diagnostics.engine

import com.appdoctor.diagnostics.model.DiagnosticIssue
import com.appdoctor.diagnostics.model.IssueCategory
import com.appdoctor.diagnostics.model.IssueRecommendation
import com.appdoctor.diagnostics.model.IssueSeverity
import com.appdoctor.diagnostics.model.IssueStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthEngineTest {

    private val engine = HealthEngine()

    @Test
    fun `healthy metrics keep high score`() {
        val context = RuleContext(
            nowMillis = 10_000L,
            memorySamples = listOf(MemorySample(1, 45f, 0), MemorySample(2, 48f, 0)),
            fpsSamples = listOf(FpsSample(1, 58f), FpsSample(2, 60f)),
            networkSamples = listOf(NetworkSample(1, 1, 120, true)),
            databaseSamples = listOf(DatabaseSample(1, 1, 4.0, true)),
            composeSamples = listOf(ComposeSample(1, 20.0, 0.02)),
        )
        val report = engine.compute(context, openIssues = emptyList(), timestampMillis = 10_000L)
        assertTrue(report.overallScore >= 85)
    }

    @Test
    fun `problematic metrics lower score`() {
        val context = RuleContext(
            nowMillis = 10_000L,
            memorySamples = listOf(MemorySample(1, 90f, 0), MemorySample(2, 94f, 0)),
            fpsSamples = listOf(FpsSample(1, 34f), FpsSample(2, 36f)),
            networkSamples = List(20) { index -> NetworkSample(index.toLong(), 1, 1_300, success = false) },
            databaseSamples = List(20) { index -> DatabaseSample(index.toLong(), 1, 60.0, success = false) },
            composeSamples = listOf(ComposeSample(1, 160.0, 0.35)),
        )
        val report = engine.compute(context, openIssues = listOf(errorIssue()), timestampMillis = 10_000L)
        assertTrue(report.overallScore <= 40)
    }

    private fun errorIssue(): DiagnosticIssue = DiagnosticIssue(
        id = "x",
        title = "x",
        description = "x",
        category = IssueCategory.CROSS_COLLECTOR,
        severity = IssueSeverity.ERROR,
        confidence = 90,
        timestampMillis = 0L,
        collectorIds = listOf("fps", "database"),
        recommendation = IssueRecommendation("p", "r", "rec", "impact"),
        documentationLink = null,
        status = IssueStatus.OPEN,
    )
}
