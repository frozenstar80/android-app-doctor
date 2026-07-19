package com.appdoctor.diagnostics.api

import com.appdoctor.diagnostics.model.DiagnosticIssue
import com.appdoctor.diagnostics.model.HealthReport
import kotlinx.coroutines.flow.StateFlow

/**
 * Read-only diagnostics API for health scores and issue streams.
 */
public interface DiagnosticsReadApi {
    /**
     * Returns the live health score stream.
     */
    public fun healthReports(): StateFlow<HealthReport>

    /**
     * Returns the live issue stream.
     */
    public fun issues(): StateFlow<List<DiagnosticIssue>>

    /**
     * Returns the latest health report snapshot.
     */
    public fun currentHealthReport(): HealthReport = healthReports().value

    /**
     * Returns the latest issue snapshot.
     */
    public fun currentIssues(): List<DiagnosticIssue> = issues().value
}
