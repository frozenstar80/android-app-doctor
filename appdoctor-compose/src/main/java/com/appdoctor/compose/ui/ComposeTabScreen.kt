package com.appdoctor.compose.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdoctor.compose.analytics.ComposeAnalytics
import com.appdoctor.compose.internal.ComposeFormatter
import com.appdoctor.compose.model.ComposeRuntimeSnapshot
import com.appdoctor.compose.model.TrackedComposable
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val HISTORY_POINTS = 60

@Composable
internal fun ComposeTabScreen(
    runtimeFlow: StateFlow<ComposeRuntimeSnapshot>,
    trackedFlow: StateFlow<List<TrackedComposable>>,
    analyticsFlow: StateFlow<ComposeAnalytics>,
    trackingEnabled: Boolean,
    analyticsEnabled: Boolean,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val runtime by runtimeFlow.collectAsStateWithLifecycle()

    val rateHistory = remember { mutableStateListOf<Float>() }
    val dropHistory = remember { mutableStateListOf<Float>() }
    LaunchedEffect(runtime.timestampMillis) {
        if (runtime.timestampMillis != 0L) {
            rateHistory.appendCapped(runtime.recompositionRate.toFloat())
            dropHistory.appendCapped(runtime.frameDropRate.toFloat())
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OverviewCard(runtime)
        LiveMetricsCard(runtime, rateHistory.toList(), dropHistory.toList())

        if (analyticsEnabled) {
            val analytics by analyticsFlow.collectAsStateWithLifecycle()
            ComposeAnalyticsSection(analytics)
        }

        if (!trackingEnabled) {
            TrackingDisabledCard()
        } else {
            TrackedComposablesSection(trackedFlow = trackedFlow, onClear = onClear)
        }
    }
}

@Composable
private fun OverviewCard(runtime: ComposeRuntimeSnapshot) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Overview", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            StatRow("Recompositions", ComposeFormatter.count(runtime.recompositionCount))
            StatRow("Recomposition rate", ComposeFormatter.rate(runtime.recompositionRate))
            StatRow("Active recomposers", runtime.compositionCount.toString())
            StatRow("Tracked (active)", runtime.activeComposables.toString())
            StatRow("Frames (dropped)", "${ComposeFormatter.count(runtime.frameCount)} (${ComposeFormatter.count(runtime.frameDrops)})")
            runtime.currentScreen?.let { StatRow("Current screen", it) }
        }
    }
}

@Composable
private fun LiveMetricsCard(
    runtime: ComposeRuntimeSnapshot,
    rateHistory: List<Float>,
    dropHistory: List<Float>,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Live Metrics", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            Text("Recompositions / sec", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Sparkline(values = rateHistory)

            StatRow("Avg recomposition", ComposeFormatter.durationNanos(runtime.averageRecompositionDurationNanos))
            StatRow("Longest recomposition", ComposeFormatter.durationNanos(runtime.longestRecompositionNanos))
            StatRow("Composition disposals", ComposeFormatter.count(runtime.compositionDisposalCount))
            StatRow("Frame drop rate", ComposeFormatter.percent(runtime.frameDropRate))

            Text("Frame drops timeline", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FrameTimeline(dropRates = dropHistory)

            Text(
                text = "Skipped recompositions and active animations are not exposed by stable " +
                    "Compose APIs and are reported as 0.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TrackingDisabledCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Tracked Composables", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                text = "Per-composable tracking is off. Enable it with " +
                    "AppDoctorConfig(enableComposableTracking = true) and add TrackRecompositions(\"Name\") " +
                    "inside the composables you want to observe.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TrackedComposablesSection(
    trackedFlow: StateFlow<List<TrackedComposable>>,
    onClear: () -> Unit,
) {
    val tracked by trackedFlow.collectAsStateWithLifecycle()
    var searchText by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(TrackedSortOrder.MOST_RECOMPOSED) }
    var filter by remember { mutableStateOf(TrackedFilter.ALL) }
    var expandedName by remember { mutableStateOf<String?>(null) }

    val filtered = remember(tracked, searchText, sortOrder, filter) {
        tracked.asSequence()
            .filter { item ->
                val matchesSearch = searchText.isBlank() || item.name.contains(searchText, ignoreCase = true)
                val matchesFilter = when (filter) {
                    TrackedFilter.ALL -> true
                    TrackedFilter.ACTIVE -> !item.disposed
                    TrackedFilter.DISPOSED -> item.disposed
                }
                matchesSearch && matchesFilter
            }
            .sortedWith(sortOrder.comparator)
            .toList()
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tracked Composables", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = onClear) { Text("Clear") }
            }
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search composables") },
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TrackedFilter.entries.forEach { entry ->
                    FilterChip(
                        selected = filter == entry,
                        onClick = { filter = entry },
                        label = { Text(entry.label) },
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TrackedSortOrder.entries.forEach { order ->
                    AssistChip(
                        onClick = { sortOrder = order },
                        label = {
                            Text(
                                text = order.label,
                                fontWeight = if (order == sortOrder) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filtered, key = { it.name }) { item ->
                TrackedRow(
                    item = item,
                    expanded = item.name == expandedName,
                    onToggle = { expandedName = if (expandedName == item.name) null else item.name },
                )
            }
        }
    }
}

@Composable
private fun TrackedRow(
    item: TrackedComposable,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = item.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = "${ComposeFormatter.count(item.recompositions)} recomp",
                    color = if (item.disposed) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF3FB950),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = buildString {
                    append(ComposeFormatter.rate(item.recompositionFrequencyPerSecond))
                    append(" • lived ")
                    append(ComposeFormatter.duration(item.lifetimeMillis))
                    if (item.disposed) append(" • disposed")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (expanded) {
                DetailRow("Recompositions", item.recompositions.toString())
                DetailRow("Frequency", ComposeFormatter.rate(item.recompositionFrequencyPerSecond))
                DetailRow("First composed", timestamp(item.firstComposedAtMillis))
                DetailRow("Last recomposed", timestamp(item.lastRecomposedAtMillis))
                DetailRow("Lifetime", ComposeFormatter.duration(item.lifetimeMillis))
                DetailRow("Depth", if (item.depth == TrackedComposable.UNKNOWN_DEPTH) "—" else item.depth.toString())
                DetailRow("Screen", item.screen ?: "—")
                DetailRow("Disposed", item.disposed.toString())
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

private fun MutableList<Float>.appendCapped(value: Float) {
    add(value)
    while (size > HISTORY_POINTS) removeAt(0)
}

private fun timestamp(millis: Long): String =
    SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(millis))

private enum class TrackedFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    DISPOSED("Disposed"),
}

private enum class TrackedSortOrder(val label: String, val comparator: Comparator<TrackedComposable>) {
    MOST_RECOMPOSED("Most recomposed", compareByDescending { it.recompositions }),
    LEAST_RECOMPOSED("Least recomposed", compareBy { it.recompositions }),
    NEWEST("Newest", compareByDescending { it.firstComposedAtMillis }),
    LONGEST_LIVED("Longest lived", compareByDescending { it.lifetimeMillis }),
}
