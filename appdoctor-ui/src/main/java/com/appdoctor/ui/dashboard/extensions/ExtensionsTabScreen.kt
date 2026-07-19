package com.appdoctor.ui.dashboard.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appdoctor.core.AppDoctor
import com.appdoctor.extension.ExtensionMetadata
import com.appdoctor.ui.dashboard.components.InfoRow
import com.appdoctor.ui.dashboard.components.SectionCard

@Composable
internal fun ExtensionsTabScreen(
    modifier: Modifier = Modifier,
) {
    var refreshToken by remember { mutableIntStateOf(0) }
    val registry = AppDoctor.extensionRegistry
    val installed = remember(refreshToken) { registry?.installed.orEmpty() }

    if (registry == null) {
        Column(modifier = modifier.fillMaxSize()) {
            SectionCard(title = "Extensions") {
                Text("Extension SDK runtime is unavailable.")
            }
        }
        return
    }

    if (installed.isEmpty()) {
        Column(modifier = modifier.fillMaxSize()) {
            SectionCard(title = "Extensions") {
                Text("No extensions are installed.")
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        installed.forEach { metadata ->
            val lifecycle = registry.lifecycle(metadata.id)
            val health = registry.health(metadata.id)
            val capabilities = registry.capabilities(metadata.id)
            SectionCard(title = metadata.name) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = metadata.id,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = "v${metadata.version.semanticVersion}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = metadata.description,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
                InfoRow("Author", metadata.author)
                InfoRow("Status", lifecycle?.name ?: "UNKNOWN")
                InfoRow("Health", health?.status?.name ?: "UNKNOWN")
                InfoRow(
                    "Capabilities",
                    capabilities?.supported?.joinToString() ?: metadata.compatibility.supportedCapabilities.joinToString(),
                )
                metadata.website?.let { InfoRow("Website", it) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    val enabled = registry.isEnabled(metadata.id)
                    if (enabled) {
                        TextButton(
                            onClick = {
                                AppDoctor.disableExtension(metadata.id)
                                refreshToken++
                            },
                        ) { Text("Disable") }
                    } else {
                        TextButton(
                            onClick = {
                                AppDoctor.enableExtension(metadata.id)
                                refreshToken++
                            },
                        ) { Text("Enable") }
                    }
                }
            }
        }
    }
}
