package com.appdoctor.database.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.appdoctor.database.analytics.DatabaseAnalytics
import com.appdoctor.database.internal.DatabaseQueryTextExporter
import com.appdoctor.database.internal.SqlFormatter
import com.appdoctor.database.model.DatabaseQuery
import com.appdoctor.database.model.QueryType
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun DatabaseTabScreen(
    queriesFlow: StateFlow<List<DatabaseQuery>>,
    analyticsFlow: StateFlow<DatabaseAnalytics>,
    analyticsEnabled: Boolean,
    slowThresholdMillis: Long,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val queries by queriesFlow.collectAsStateWithLifecycle()
    var searchText by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf(TypeFilter.ALL) }
    var resultFilter by remember { mutableStateOf(ResultFilter.ALL) }
    var sortOrder by remember { mutableStateOf(QuerySortOrder.NEWEST) }
    var selectedId by remember { mutableStateOf<Long?>(null) }

    val filtered = remember(queries, searchText, typeFilter, resultFilter, sortOrder) {
        queries.asSequence()
            .filter { query ->
                val matchesSearch = searchText.isBlank() || query.sql.contains(searchText, ignoreCase = true)
                val matchesType = typeFilter == TypeFilter.ALL || query.type == typeFilter.type
                val matchesResult = when (resultFilter) {
                    ResultFilter.ALL -> true
                    ResultFilter.SUCCESS -> query.success
                    ResultFilter.FAILURE -> !query.success
                }
                matchesSearch && matchesType && matchesResult
            }
            .sortedWith(sortOrder.comparator)
            .toList()
    }
    val selected = queries.firstOrNull { it.id == selectedId }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DatabaseFilters(
            searchText = searchText,
            onSearchTextChange = { searchText = it },
            typeFilter = typeFilter,
            onTypeSelected = { typeFilter = it },
            resultFilter = resultFilter,
            onResultSelected = { resultFilter = it },
            sortOrder = sortOrder,
            onSortSelected = { sortOrder = it },
            onClear = {
                selectedId = null
                onClear()
            },
        )

        if (analyticsEnabled) {
            val analytics by analyticsFlow.collectAsStateWithLifecycle()
            DatabaseAnalyticsSection(analytics)
        }

        if (selected != null) {
            QueryDetailsCard(query = selected, onClose = { selectedId = null })
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.id }) { query ->
                    QueryRow(
                        query = query,
                        slowThresholdMillis = slowThresholdMillis,
                        selected = query.id == selectedId,
                        onSelect = { selectedId = query.id },
                    )
                }
            }
        }
    }
}

@Composable
private fun DatabaseFilters(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    typeFilter: TypeFilter,
    onTypeSelected: (TypeFilter) -> Unit,
    resultFilter: ResultFilter,
    onResultSelected: (ResultFilter) -> Unit,
    sortOrder: QuerySortOrder,
    onSortSelected: (QuerySortOrder) -> Unit,
    onClear: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search SQL") },
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TypeFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = typeFilter == filter,
                        onClick = { onTypeSelected(filter) },
                        label = { Text(filter.label) },
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ResultFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = resultFilter == filter,
                        onClick = { onResultSelected(filter) },
                        label = { Text(filter.label) },
                    )
                }
                TextButton(onClick = onClear) { Text("Clear") }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                QuerySortOrder.entries.forEach { order ->
                    AssistChip(
                        onClick = { onSortSelected(order) },
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
private fun QueryRow(
    query: DatabaseQuery,
    slowThresholdMillis: Long,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val slow = query.durationMillis >= slowThresholdMillis
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(query.type.name, color = typeColor(query.type), fontWeight = FontWeight.Bold)
                Text(
                    text = String.format(Locale.US, "%.2f ms", query.durationMillis),
                    color = if (slow) Color(0xFFD29922) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (slow) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
            Text(
                text = SqlFormatter.collapse(query.sql),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )
            Text(
                text = buildString {
                    append(timestamp(query.timestampMillis))
                    query.rowsReturned?.let { append(" • $it rows") }
                    query.rowsAffected?.let { append(" • $it affected") }
                    if (!query.success) append(" • FAILED")
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (query.success) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFF85149),
            )
        }
    }
}

@Composable
private fun QueryDetailsCard(query: DatabaseQuery, onClose: () -> Unit) {
    val context = LocalContext.current
    var sqlExpanded by remember(query.id) { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Query Details", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { copyQuery(context, query) }) { Text("Copy") }
                    TextButton(onClick = { exportQuery(context, query) }) { Text("Export") }
                    TextButton(onClick = onClose) { Text("Close") }
                }
            }
            val sqlText = if (sqlExpanded) SqlFormatter.format(query.sql) else SqlFormatter.collapse(query.sql, max = 240)
            Text(
                text = sqlText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable { sqlExpanded = !sqlExpanded },
            )
            TextButton(onClick = { sqlExpanded = !sqlExpanded }) {
                Text(if (sqlExpanded) "Collapse SQL" else "Expand SQL")
            }
            DetailRow("Type", query.type.name)
            DetailRow("Duration", String.format(Locale.US, "%.3f ms", query.durationMillis))
            DetailRow("Success", query.success.toString())
            DetailRow("Thread", query.threadName)
            DetailRow("Database", query.databaseName ?: "(unknown)")
            DetailRow("Rows affected", query.rowsAffected?.toString() ?: "—")
            DetailRow("Rows returned", query.rowsReturned?.toString() ?: "—")
            DetailRow("Transaction", query.transactionId?.let { "#$it" } ?: "—")
            DetailRow("Timestamp", timestamp(query.timestampMillis))
            if (!query.error.isNullOrBlank()) DetailRow("Exception", query.error)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
    }
}

private fun copyQuery(context: Context, query: DatabaseQuery) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("AppDoctor SQL Query", DatabaseQueryTextExporter.toText(query)))
    Toast.makeText(context, "Copied query", Toast.LENGTH_SHORT).show()
}

private fun exportQuery(context: Context, query: DatabaseQuery) {
    val folder = File(context.getExternalFilesDir(null), "appdoctor-database").apply { mkdirs() }
    val file = File(folder, "query-${query.id}.txt")
    file.writeText(DatabaseQueryTextExporter.toText(query))
    Toast.makeText(context, "Exported: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
}

private fun timestamp(millis: Long): String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(millis))

@Composable
private fun typeColor(type: QueryType): Color = when (type) {
    QueryType.SELECT -> Color(0xFF3FB950)
    QueryType.INSERT -> Color(0xFF4F9CF9)
    QueryType.UPDATE -> Color(0xFFD29922)
    QueryType.DELETE -> Color(0xFFF85149)
    QueryType.OTHER -> MaterialTheme.colorScheme.onSurfaceVariant
}

private enum class TypeFilter(val label: String, val type: QueryType?) {
    ALL("All", null),
    SELECT("SELECT", QueryType.SELECT),
    INSERT("INSERT", QueryType.INSERT),
    UPDATE("UPDATE", QueryType.UPDATE),
    DELETE("DELETE", QueryType.DELETE),
}

private enum class ResultFilter(val label: String) { ALL("Any"), SUCCESS("Success"), FAILURE("Failure") }

private enum class QuerySortOrder(val label: String, val comparator: Comparator<DatabaseQuery>) {
    NEWEST("Newest", compareByDescending { it.timestampMillis }),
    OLDEST("Oldest", compareBy { it.timestampMillis }),
    DURATION("Duration", compareByDescending { it.durationNanos }),
}
