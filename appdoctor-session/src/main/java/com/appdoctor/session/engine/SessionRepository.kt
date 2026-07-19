package com.appdoctor.session.engine

import com.appdoctor.session.model.SessionReport

public class SessionRepository(
    maximumStoredReports: Int,
) {
    private val bound: Int = maximumStoredReports.coerceAtLeast(1)
    private val lock = Any()
    private val reports = ArrayDeque<SessionReport>()

    public fun save(report: SessionReport) {
        synchronized(lock) {
            reports.addFirst(report)
            while (reports.size > bound) {
                reports.removeLast()
            }
        }
    }

    public fun latest(): SessionReport? = synchronized(lock) { reports.firstOrNull() }

    public fun all(): List<SessionReport> = synchronized(lock) { reports.toList() }

    public fun findBySessionId(sessionId: String): SessionReport? =
        synchronized(lock) { reports.firstOrNull { it.sessionId == sessionId } }

    public fun clear() {
        synchronized(lock) {
            reports.clear()
        }
    }
}
