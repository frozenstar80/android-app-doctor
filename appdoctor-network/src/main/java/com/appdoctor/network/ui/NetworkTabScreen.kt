package com.appdoctor.network.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdoctor.network.internal.BodyPreviewFormatter
import com.appdoctor.network.internal.NetworkRequestTextExporter
import com.appdoctor.network.model.CapturedBody
import com.appdoctor.network.model.NetworkRequestRecord
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun NetworkTabScreen(
    requestsFlow: StateFlow<List<NetworkRequestRecord>>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val requests by requestsFlow.collectAsStateWithLifecycle()
    var searchText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("ALL") }
    var statusCodeFilter by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(NetworkSortOrder.NEWEST) }
    var selectedRequestId by remember { mutableStateOf<Long?>(null) }

    val filtered = remember(requests, searchText, selectedMethod, statusCodeFilter, sortOrder) {
        requests
            .asSequence()
            .filter { request ->
                val matchesSearch = searchText.isBlank() ||
                    request.url.contains(searchText, ignoreCase = true) ||
                    request.method.contains(searchText, ignoreCase = true)
                val matchesMethod = selectedMethod == "ALL" || request.method.equals(selectedMethod, ignoreCase = true)
                val matchesStatus = statusCodeFilter.isBlank() || request.statusCode?.toString() == statusCodeFilter
                matchesSearch && matchesMethod && matchesStatus
            }
            .sortedWith(sortOrder.comparator)
            .toList()
    }
    val selectedRequest = filtered.firstOrNull { it.id == selectedRequestId } ?: requests.firstOrNull { it.id == selectedRequestId }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NetworkFilters(
            searchText = searchText,
            onSearchTextChange = { searchText = it },
            selectedMethod = selectedMethod,
            onMethodSelected = { selectedMethod = it },
            statusCodeFilter = statusCodeFilter,
            onStatusCodeFilterChange = { statusCodeFilter = it.filter(Char::isDigit).take(3) },
            sortOrder = sortOrder,
            onSortOrderSelected = { sortOrder = it },
            onClear = {
                selectedRequestId = null
                onClear()
            },
        )

        if (selectedRequest != null) {
            RequestDetailsCard(
                request = selectedRequest,
                onClose = { selectedRequestId = null },
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.id }) { request ->
                    NetworkTimelineItem(
                        request = request,
                        selected = request.id == selectedRequestId,
                        onSelect = { selectedRequestId = request.id },
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkFilters(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    selectedMethod: String,
    onMethodSelected: (String) -> Unit,
    statusCodeFilter: String,
    onStatusCodeFilterChange: (String) -> Unit,
    sortOrder: NetworkSortOrder,
    onSortOrderSelected: (NetworkSortOrder) -> Unit,
    onClear: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search URL / method") },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = statusCodeFilter,
                    onValueChange = onStatusCodeFilterChange,
                    singleLine = true,
                    label = { Text("Status") },
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClear) { Text("Clear") }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("ALL", "GET", "POST", "PUT", "DELETE").forEach { method ->
                    FilterChip(
                        selected = selectedMethod == method,
                        onClick = { onMethodSelected(method) },
                        label = { Text(method) },
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NetworkSortOrder.entries.forEach { order ->
                    AssistChip(
                        onClick = { onSortOrderSelected(order) },
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
}

@Composable
private fun NetworkTimelineItem(
    request: NetworkRequestRecord,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val statusColor = statusColor(request.statusCode, request.success)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${request.method} ${request.url}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )
                Text(
                    text = request.statusCode?.toString() ?: "ERR",
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "${timestamp(request.timestampMillis)} • ${request.responseTimeMillis} ms • ${request.contentLength?.toString() ?: "?"} B",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RequestDetailsCard(
    request: NetworkRequestRecord,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Request Details", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { copyRequest(context, request) }) { Text("Copy") }
                    TextButton(onClick = { shareRequest(context, request) }) { Text("Share") }
                    TextButton(onClick = { exportRequest(context, request) }) { Text("Export") }
                    TextButton(onClick = onClose) { Text("Close") }
                }
            }
            ExpandableSection(title = "Request") {
                DetailRow("Method", request.method)
                DetailRow("URL", request.url)
                DetailRow("Query", request.queryParameters.joinToString { "${it.name}=${it.value.orEmpty()}" })
            }
            ExpandableSection(title = "Response") {
                DetailRow("Status", request.statusCode?.toString() ?: "Failed")
                DetailRow("Success", request.success.toString())
                DetailRow("Content Length", request.contentLength?.toString() ?: "Unknown")
                if (!request.failureMessage.isNullOrBlank()) {
                    DetailRow("Failure", request.failureMessage)
                }
            }
            ExpandableSection(title = "Headers") {
                request.requestHeaders.forEach { DetailRow("Req ${it.name}", it.value) }
                request.responseHeaders.forEach { DetailRow("Res ${it.name}", it.value) }
            }
            ExpandableSection(title = "Body") {
                BodySection("Request Body", request.requestBody)
                BodySection("Response Body", request.responseBody)
            }
            ExpandableSection(title = "Timing") {
                DetailRow("Timestamp", timestamp(request.timestampMillis))
                DetailRow("Duration", "${request.responseTimeMillis} ms")
            }
        }
    }
}

@Composable
private fun BodySection(title: String, body: CapturedBody?) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
    val value = if (body == null) {
        "(Not captured)"
    } else {
        val formatted = BodyPreviewFormatter.format(body.contentType, body.text, body.isBinary)
        if (body.truncated && formatted.isNotBlank()) "$formatted\n…truncated" else formatted
    }
    Text(
        text = value,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ExpandableSection(
    title: String,
    content: @Composable () -> Unit,
) {
    var expanded by remember(title) { mutableStateOf(true) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "▼ $title" else "▶ $title")
            }
            if (expanded) content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun copyRequest(context: Context, request: NetworkRequestRecord) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("AppDoctor Network Request", NetworkRequestTextExporter.toText(request)))
    Toast.makeText(context, "Copied request", Toast.LENGTH_SHORT).show()
}

private fun shareRequest(context: Context, request: NetworkRequestRecord) {
    val text = NetworkRequestTextExporter.toText(request)
    val intent = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(intent, "Share Request"))
}

private fun exportRequest(context: Context, request: NetworkRequestRecord) {
    val folder = File(context.getExternalFilesDir(null), "appdoctor-network").apply { mkdirs() }
    val file = File(folder, "request-${request.id}.txt")
    file.writeText(NetworkRequestTextExporter.toText(request))
    Toast.makeText(context, "Exported: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
}

private fun timestamp(millis: Long): String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(millis))

@Composable
private fun statusColor(statusCode: Int?, success: Boolean): Color {
    if (!success || statusCode == null) return Color(0xFFF85149)
    return when (statusCode) {
        in 200..299 -> Color(0xFF3FB950)
        in 300..399 -> Color(0xFFD29922)
        in 400..599 -> Color(0xFFF85149)
        else -> MaterialTheme.colorScheme.onSurface
    }
}

private enum class NetworkSortOrder(
    val label: String,
    val comparator: Comparator<NetworkRequestRecord>,
) {
    NEWEST(
        label = "Newest",
        comparator = compareByDescending { it.timestampMillis },
    ),
    OLDEST(
        label = "Oldest",
        comparator = compareBy { it.timestampMillis },
    ),
    LONGEST_DURATION(
        label = "Longest Duration",
        comparator = compareByDescending { it.responseTimeMillis },
    ),
}
