package com.appdoctor.ai.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdoctor.ai.AiAnalysis
import com.appdoctor.ai.AiUiState
import kotlinx.coroutines.flow.StateFlow
import java.io.File

@Composable
internal fun AiTabScreen(
    stateFlow: StateFlow<AiUiState>,
    onGenerate: () -> Unit,
    onRefresh: () -> Unit,
    onExportMarkdown: () -> String?,
    onExportJson: () -> String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by stateFlow.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Section("AI Analysis") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onGenerate) { Text("Generate Analysis") }
                TextButton(onClick = onRefresh) { Text("Refresh") }
                TextButton(
                    onClick = {
                        val markdown = onExportMarkdown() ?: return@TextButton
                        copyToClipboard(context, markdown)
                    },
                ) { Text("Copy") }
                TextButton(
                    onClick = {
                        val markdown = onExportMarkdown() ?: return@TextButton
                        shareText(context, markdown)
                    },
                ) { Text("Share") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = {
                        val markdown = onExportMarkdown() ?: return@TextButton
                        exportToFile(context, "analysis.md", markdown)
                    },
                ) { Text("Export Markdown") }
                TextButton(
                    onClick = {
                        val json = onExportJson() ?: return@TextButton
                        exportToFile(context, "analysis.json", json)
                    },
                ) { Text("Export JSON") }
            }
            state.message?.let {
                Text(text = it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.loading) {
                Text(text = "Generating analysis...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        val analysis = state.latest?.analysis
        if (analysis == null) {
            Section("Status") {
                Text(
                    text = "No AI provider configured.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            AnalysisSection(analysis)
        }

        Section("History") {
            if (state.history.isEmpty()) {
                Text("No analyses yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                state.history.forEach { response ->
                    Info("${response.provider}/${response.model}", response.generatedAtMillis.toString())
                }
            }
        }
    }
}

@Composable
private fun AnalysisSection(analysis: AiAnalysis) {
    Section("Executive Summary") {
        Text(analysis.executiveSummary)
    }
    Section("Performance Findings") {
        analysis.performanceFindings.forEach { Text("• $it") }
    }
    Section("Root Cause Candidates") {
        analysis.rootCauseCandidates.forEach { Text("• $it") }
    }
    Section("Recommendations") {
        analysis.recommendations.forEach { Text("• $it") }
    }
    Section("Optimization Opportunities") {
        analysis.optimizationOpportunities.forEach { Text("• $it") }
    }
    Section("Risk Assessment") {
        Text(analysis.riskAssessment)
        Info("Confidence", "${analysis.confidence}%")
    }
    Section("Action Items") {
        analysis.actionItems.forEach { Text("• $it") }
    }
    Section("Next Investigation Steps") {
        analysis.nextInvestigationSteps.forEach { Text("• $it") }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun Info(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("AppDoctor AI", text))
    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(intent, "Share AI Analysis"))
}

private fun exportToFile(context: Context, fileName: String, content: String) {
    val outputDirectory = File(context.cacheDir, "appdoctor-ai-exports").apply { mkdirs() }
    val target = File(outputDirectory, fileName)
    runCatching { target.writeText(content) }
    Toast.makeText(context, "Saved ${target.name}", Toast.LENGTH_SHORT).show()
}
