package com.appdoctor.diagnostics.engine

import com.appdoctor.diagnostics.model.DiagnosticIssue
import com.appdoctor.diagnostics.model.IssueStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Thread-safe issue lifecycle store with bounded history.
 */
public class IssueRepository(
    private val maximumIssueHistory: Int,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {

    private val lock = Any()
    private val latestById = LinkedHashMap<String, DiagnosticIssue>()
    private val history = ArrayDeque<DiagnosticIssue>()
    private val ignoredIds = LinkedHashSet<String>()
    private val _issues = MutableStateFlow<List<DiagnosticIssue>>(emptyList())

    public val issues: StateFlow<List<DiagnosticIssue>> = _issues

    public fun reconcile(detectedOpenIssues: List<DiagnosticIssue>) {
        synchronized(lock) {
            val now = clockMillis()
            val activeIds = detectedOpenIssues.mapTo(LinkedHashSet()) { it.id }

            for (incoming in detectedOpenIssues) {
                val next = if (ignoredIds.contains(incoming.id)) {
                    incoming.copy(status = IssueStatus.IGNORED, timestampMillis = now)
                } else {
                    incoming.copy(status = IssueStatus.OPEN)
                }
                recordIfChanged(next)
            }

            for ((id, current) in latestById.toMap()) {
                if (current.status == IssueStatus.OPEN || current.status == IssueStatus.IGNORED) {
                    if (!activeIds.contains(id)) {
                        if (current.status == IssueStatus.IGNORED) ignoredIds.remove(id)
                        recordIfChanged(current.copy(status = IssueStatus.RESOLVED, timestampMillis = now))
                    }
                }
            }

            publish()
        }
    }

    public fun ignoreIssue(id: String) {
        synchronized(lock) {
            val existing = latestById[id] ?: return
            ignoredIds.add(id)
            recordIfChanged(existing.copy(status = IssueStatus.IGNORED, timestampMillis = clockMillis()))
            publish()
        }
    }

    public fun openIssues(): List<DiagnosticIssue> = synchronized(lock) {
        latestById.values.filter { it.status == IssueStatus.OPEN }
    }

    private fun recordIfChanged(next: DiagnosticIssue) {
        val previous = latestById[next.id]
        if (previous != null && previous == next) return
        latestById[next.id] = next
        history.addFirst(next)
        trimHistory()
    }

    private fun trimHistory() {
        val max = maximumIssueHistory.coerceAtLeast(1)
        while (history.size > max) {
            history.removeLast()
        }
    }

    private fun publish() {
        _issues.value = history.toList()
    }
}
