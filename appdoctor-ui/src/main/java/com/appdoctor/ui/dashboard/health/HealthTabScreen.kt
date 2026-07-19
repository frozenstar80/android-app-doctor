package com.appdoctor.ui.dashboard.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdoctor.ui.dashboard.components.InfoRow
import com.appdoctor.ui.dashboard.components.MetricBar
import com.appdoctor.ui.dashboard.components.SectionCard
import com.appdoctor.ui.theme.AppDoctorTokens

private enum class SeverityFilter { ALL, CRITICAL, ERROR, WARNING, INFO }
private enum class SortBy { NEWEST, SEVERITY, CONFIDENCE }

@Composable
internal fun HealthTabScreen(
    modifier: Modifier = Modifier,
    adapter: DiagnosticsReflectionAdapter = remember { DiagnosticsReflectionAdapter() },
) {
    val rawHealth by adapter.healthFlow.collectAsStateWithLifecycle()
    val rawIssues by adapter.issuesFlow.collectAsStateWithLifecycle()
    val health = adapter.parseHealth(rawHealth)
    val issues = adapter.parseIssues(rawIssues)

    var severityFilter by remember { mutableStateOf(SeverityFilter.ALL) }
    var sortBy by remember { mutableStateOf(SortBy.NEWEST) }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    val openIssues = issues.filter { it.status == "OPEN" }
    val filtered = openIssues.filter { issue ->
        when (severityFilter) {
            SeverityFilter.ALL -> true
            SeverityFilter.CRITICAL -> issue.severity == "CRITICAL"
            SeverityFilter.ERROR -> issue.severity == "ERROR"
            SeverityFilter.WARNING -> issue.severity == "WARNING"
            SeverityFilter.INFO -> issue.severity == "INFO"
        }
    }.sortedWith(
        when (sortBy) {
            SortBy.NEWEST -> compareByDescending<IssueUiModel> { it.timestampMillis }
            SortBy.SEVERITY -> compareByDescending<IssueUiModel> { severityRank(it.severity) }
                .thenByDescending { it.timestampMillis }
            SortBy.CONFIDENCE -> compareByDescending<IssueUiModel> { it.confidence }
                .thenByDescending { it.timestampMillis }
        },
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (health == null) {
            SectionCard(title = "Health") {
                Text(
                    text = "Diagnostics module is not active. Enable it with AppDoctorConfig(enableDiagnostics = true) and include appdoctor-diagnostics.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        SectionCard(title = "Overall Health") {
            MetricBar(
                label = "Overall Score",
                fraction = health.overallScore / 100f,
                valueLabel = "${health.overallScore}%",
            )
            MetricBar("Performance", health.performanceScore / 100f, "${health.performanceScore}%")
            MetricBar("Memory", health.memoryScore / 100f, "${health.memoryScore}%")
            MetricBar("Network", health.networkScore / 100f, "${health.networkScore}%")
            MetricBar("Database", health.databaseScore / 100f, "${health.databaseScore}%")
            MetricBar("Compose", health.composeScore / 100f, "${health.composeScore}%")
        }

        SectionCard(title = "Severity Counts") {
            InfoRow("Critical", openIssues.count { it.severity == "CRITICAL" }.toString())
            InfoRow("Error", openIssues.count { it.severity == "ERROR" }.toString())
            InfoRow("Warning", openIssues.count { it.severity == "WARNING" }.toString())
            InfoRow("Info", openIssues.count { it.severity == "INFO" }.toString())
        }

        SectionCard(title = "Latest Recommendations") {
            if (openIssues.isEmpty()) {
                Text("No active recommendations.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                openIssues.sortedByDescending { it.timestampMillis }.take(3).forEach { issue ->
                    Text(
                        text = "• ${issue.recommendation.recommendation}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        SectionCard(title = "Detected Issues") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip("All", severityFilter == SeverityFilter.ALL) { severityFilter = SeverityFilter.ALL }
                FilterChip("Critical", severityFilter == SeverityFilter.CRITICAL) { severityFilter = SeverityFilter.CRITICAL }
                FilterChip("Error", severityFilter == SeverityFilter.ERROR) { severityFilter = SeverityFilter.ERROR }
                FilterChip("Warn", severityFilter == SeverityFilter.WARNING) { severityFilter = SeverityFilter.WARNING }
                FilterChip("Info", severityFilter == SeverityFilter.INFO) { severityFilter = SeverityFilter.INFO }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip("Newest", sortBy == SortBy.NEWEST) { sortBy = SortBy.NEWEST }
                FilterChip("Severity", sortBy == SortBy.SEVERITY) { sortBy = SortBy.SEVERITY }
                FilterChip("Confidence", sortBy == SortBy.CONFIDENCE) { sortBy = SortBy.CONFIDENCE }
            }
            if (filtered.isEmpty()) {
                Text(
                    text = "No open issues matching the current filter.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                filtered.forEach { issue ->
                    val isExpanded = expanded[issue.id] == true
                    val severityColor = severityColor(issue.severity)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = issue.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = severityColor,
                        )
                        InfoRow("Category", issue.category)
                        InfoRow("Severity", issue.severity)
                        InfoRow("Confidence", "${issue.confidence}%")
                        InfoRow("Collectors", issue.collectorIds.joinToString())
                        if (isExpanded) {
                            Text(issue.description, color = MaterialTheme.colorScheme.onSurface)
                            Text("Problem: ${issue.recommendation.problem}", color = MaterialTheme.colorScheme.onSurface)
                            Text("Reason: ${issue.recommendation.reason}", color = MaterialTheme.colorScheme.onSurface)
                            Text("Recommendation: ${issue.recommendation.recommendation}", color = MaterialTheme.colorScheme.onSurface)
                            Text("Expected impact: ${issue.recommendation.expectedImpact}", color = MaterialTheme.colorScheme.onSurface)
                            issue.documentationLink?.let {
                                Text("Docs: $it", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { expanded[issue.id] = !isExpanded }) {
                                Text(if (isExpanded) "Collapse" else "Expand")
                            }
                            TextButton(onClick = { adapter.dismissIssue(issue.id) }) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
        ),
    )
}

private fun severityRank(severity: String): Int = when (severity) {
    "CRITICAL" -> 4
    "ERROR" -> 3
    "WARNING" -> 2
    else -> 1
}

@Composable
private fun severityColor(severity: String): Color = when (severity) {
    "CRITICAL" -> AppDoctorTokens.Bad
    "ERROR" -> AppDoctorTokens.Bad.copy(alpha = 0.85f)
    "WARNING" -> AppDoctorTokens.Warn
    else -> MaterialTheme.colorScheme.onSurface
}
