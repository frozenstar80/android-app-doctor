package com.appdoctor.compose.ui

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
import com.appdoctor.compose.analytics.ComposeAnalytics
import com.appdoctor.compose.internal.ComposeFormatter

@Composable
internal fun ComposeAnalyticsSection(analytics: ComposeAnalytics) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Analytics", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            StatRow("Total recompositions", ComposeFormatter.count(analytics.totalRecompositions))
            StatRow("Avg recompositions/s", ComposeFormatter.rate(analytics.averageRecompositionsPerSecond))
            StatRow("Tracked composables", analytics.trackedComposables.toString())
            StatRow(
                "Frames (dropped)",
                "${ComposeFormatter.count(analytics.frame.frameCount)} (${ComposeFormatter.count(analytics.frame.frameDrops)})",
            )
            StatRow("Frame drop rate", ComposeFormatter.percent(analytics.frame.dropRate))

            if (analytics.mostRecomposed.isNotEmpty()) {
                Text("Most recomposed", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                MiniBarChart(
                    bars = analytics.mostRecomposed.map {
                        LabeledValue(it.name, it.count.toFloat(), ComposeFormatter.count(it.count))
                    },
                )
            }
            if (analytics.leastRecomposed.isNotEmpty()) {
                Text("Least recomposed", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                analytics.leastRecomposed.forEach { StatRow("• ${it.name}", ComposeFormatter.count(it.count)) }
            }
            if (analytics.highestFrequency.isNotEmpty()) {
                Text("Highest recomposition frequency", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                analytics.highestFrequency.forEach { StatRow("• ${it.name}", ComposeFormatter.rate(it.perSecond)) }
            }
            if (analytics.longestLiving.isNotEmpty()) {
                Text("Longest living", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                analytics.longestLiving.forEach { StatRow("• ${it.name}", ComposeFormatter.duration(it.millis)) }
            }
            if (analytics.screens.isNotEmpty()) {
                Text("Screen statistics", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                analytics.screens.forEach {
                    StatRow("• ${it.screen}", "${it.trackedComposables} comp • ${ComposeFormatter.count(it.recompositions)} recomp")
                }
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
