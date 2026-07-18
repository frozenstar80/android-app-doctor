package com.appdoctor.database.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.appdoctor.database.analytics.DatabaseAnalytics
import com.appdoctor.database.internal.SqlFormatter
import java.util.Locale

@Composable
internal fun DatabaseAnalyticsSection(analytics: DatabaseAnalytics) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Analytics", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            StatRow("Total queries", analytics.totalQueries.toString())
            StatRow("Avg duration", ms(analytics.averageDurationNanos))
            StatRow("Min / Max", "${ms(analytics.minDurationNanos.toDouble())} / ${ms(analytics.maxDurationNanos.toDouble())}")
            StatRow("Slow queries", analytics.slowQueryCount.toString())
            StatRow("Failed queries", analytics.failedQueryCount.toString())
            StatRow("Success rate", String.format(Locale.US, "%.1f%%", analytics.successRate * 100))
            StatRow("Reads : Writes", "${analytics.readCount} : ${analytics.writeCount}")
            StatRow("Transactions", analytics.transactionsExecuted.toString())
            analytics.countByType.forEach { (type, count) -> StatRow("• $type", count.toString()) }
            if (analytics.mostQueriedTables.isNotEmpty()) {
                Text("Most queried tables", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                analytics.mostQueriedTables.forEach { StatRow("• ${it.table}", it.count.toString()) }
            }
            if (analytics.mostFrequentStatements.isNotEmpty()) {
                Text("Most frequent statements", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                analytics.mostFrequentStatements.forEach { StatRow("• ${SqlFormatter.collapse(it.sql, max = 60)}", it.count.toString()) }
            }
            if (analytics.longestQueries.isNotEmpty()) {
                Text("Longest running", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                analytics.longestQueries.forEach { StatRow("• ${SqlFormatter.collapse(it.sql, max = 60)}", ms(it.durationNanos.toDouble())) }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

private fun ms(nanos: Double): String = String.format(Locale.US, "%.2f ms", nanos / 1_000_000.0)
