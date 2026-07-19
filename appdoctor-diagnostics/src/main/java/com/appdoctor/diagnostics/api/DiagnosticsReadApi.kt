package com.appdoctor.diagnostics.api

import com.appdoctor.diagnostics.model.DiagnosticIssue
import com.appdoctor.diagnostics.model.HealthReport
import kotlinx.coroutines.flow.StateFlow

public interface DiagnosticsReadApi {
    public fun healthReports(): StateFlow<HealthReport>
    public fun issues(): StateFlow<List<DiagnosticIssue>>

    public fun currentHealthReport(): HealthReport = healthReports().value
    public fun currentIssues(): List<DiagnosticIssue> = issues().value
}
