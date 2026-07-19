package com.appdoctor.diagnostics.engine

import com.appdoctor.diagnostics.model.DiagnosticIssue
import com.appdoctor.diagnostics.model.IssueCategory
import com.appdoctor.diagnostics.model.IssueRecommendation
import com.appdoctor.diagnostics.model.IssueSeverity
import com.appdoctor.diagnostics.model.IssueStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IssueRepositoryTest {

    @Test
    fun `open resolve and ignore lifecycle works`() {
        var now = 1_000L
        val repository = IssueRepository(maximumIssueHistory = 20) { now }

        val issue = issue(id = "memory.sustained_high_usage", status = IssueStatus.OPEN)
        repository.reconcile(listOf(issue))
        assertEquals(IssueStatus.OPEN, repository.openIssues().single().status)

        repository.ignoreIssue(issue.id)
        assertTrue(repository.openIssues().isEmpty())
        assertEquals(IssueStatus.IGNORED, repository.issues.value.first().status)

        now += 500L
        repository.reconcile(emptyList())
        assertEquals(IssueStatus.RESOLVED, repository.issues.value.first().status)
    }

    @Test
    fun `history is bounded`() {
        val repository = IssueRepository(maximumIssueHistory = 3)
        repeat(6) { index ->
            repository.reconcile(listOf(issue(id = "i-$index", status = IssueStatus.OPEN)))
            repository.reconcile(emptyList())
        }
        assertEquals(3, repository.issues.value.size)
    }

    private fun issue(id: String, status: IssueStatus): DiagnosticIssue = DiagnosticIssue(
        id = id,
        title = "Title",
        description = "Description",
        category = IssueCategory.MEMORY,
        severity = IssueSeverity.WARNING,
        confidence = 80,
        timestampMillis = 0L,
        collectorIds = listOf("memory"),
        recommendation = IssueRecommendation("p", "r", "rec", "impact"),
        documentationLink = null,
        status = status,
    )
}
