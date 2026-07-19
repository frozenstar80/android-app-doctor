package com.appdoctor.ui.dashboard.timeline

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
import androidx.compose.material3.OutlinedTextField
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
import com.appdoctor.ui.dashboard.components.SectionCard
import com.appdoctor.ui.theme.AppDoctorTokens

@Composable
internal fun TimelineTabScreen(
    onJumpToIssue: (String) -> Unit,
    modifier: Modifier = Modifier,
    adapter: TimelineReflectionAdapter = remember { TimelineReflectionAdapter() },
) {
    val rawEvents by adapter.eventsFlow.collectAsStateWithLifecycle()
    val events = adapter.parseEvents(rawEvents)
    var query by remember { mutableStateOf("") }
    var collectorFilter by remember { mutableStateOf("All") }
    val expanded = remember { mutableStateMapOf<Long, Boolean>() }

    val collectors = remember(events) { listOf("All") + events.map { it.collectorId }.distinct().sorted() }
    val filtered = events
        .filter { event -> collectorFilter == "All" || event.collectorId == collectorFilter }
        .filter { event ->
            query.isBlank() ||
                event.title.contains(query, ignoreCase = true) ||
                event.summary.contains(query, ignoreCase = true) ||
                event.category.contains(query, ignoreCase = true) ||
                event.metadata.values.any { it.contains(query, ignoreCase = true) }
        }
        .sortedByDescending { it.timestamp }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (events.isEmpty()) {
            SectionCard("Timeline") {
                Text(
                    text = "Timeline module is not active. Enable it with AppDoctorConfig(enableTimeline = true) and include appdoctor-timeline.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        SectionCard("Timeline Stream") {
            InfoRow("Events", filtered.size.toString())
            InfoRow("Collectors", events.map { it.collectorId }.distinct().size.toString())
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                collectors.forEach { collector ->
                    AssistChip(
                        onClick = { collectorFilter = collector },
                        label = { Text(collector) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (collectorFilter == collector) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                    )
                }
            }
        }

        filtered.forEach { event ->
            val key = event.timestamp xor event.title.hashCode().toLong()
            val isExpanded = expanded[key] == true
            SectionCard(title = event.title) {
                Text(
                    text = event.summary,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = eventColor(event.category, event.severity),
                )
                InfoRow("Collector", event.collectorId)
                InfoRow("Category", event.category)
                InfoRow("Time", event.timestamp.toString())
                InfoRow("Group", event.groupId ?: "-")
                event.relatedIssueId?.let { issueId ->
                    InfoRow("Related Issue", issueId)
                }
                if (isExpanded) {
                    event.metadata.forEach { (keyName, value) ->
                        InfoRow(keyName, value)
                    }
                    InfoRow("Source Metric", event.sourceMetric)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { expanded[key] = !isExpanded }) {
                        Text(if (isExpanded) "Collapse" else "Expand metadata")
                    }
                    val issueId = event.relatedIssueId
                    if (issueId != null) {
                        TextButton(onClick = { onJumpToIssue(issueId) }) {
                            Text("Jump to issue")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun eventColor(category: String, severity: String?): Color = when {
    severity == "CRITICAL" || severity == "ERROR" -> AppDoctorTokens.Bad
    severity == "WARNING" -> AppDoctorTokens.Warn
    category == "NETWORK" -> Color(0xFF5DADE2)
    category == "DATABASE" -> Color(0xFF58D68D)
    category == "FPS" -> Color(0xFFF5B041)
    category == "DIAGNOSTICS" -> Color(0xFFA569BD)
    else -> MaterialTheme.colorScheme.onSurface
}
