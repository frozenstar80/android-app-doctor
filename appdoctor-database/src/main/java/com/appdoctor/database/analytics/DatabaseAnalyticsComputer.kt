package com.appdoctor.database.analytics

import com.appdoctor.database.internal.SqlFormatter
import com.appdoctor.database.internal.SqlTableExtractor
import com.appdoctor.database.model.DatabaseQuery
import com.appdoctor.database.model.QueryType

/**
 * Pure, stateless aggregation of captured queries into [DatabaseAnalytics].
 *
 * Kept independent of the collector and free of coroutines so it is trivially testable and
 * so future metrics can be added without touching the collection path. Not part of the
 * public API.
 */
internal object DatabaseAnalyticsComputer {

    private const val TOP_N = 5

    fun compute(queries: List<DatabaseQuery>, slowThresholdMillis: Long): DatabaseAnalytics {
        if (queries.isEmpty()) return DatabaseAnalytics.Empty

        val total = queries.size
        val slowNanos = slowThresholdMillis.coerceAtLeast(0L) * 1_000_000L

        var sum = 0L
        var min = Long.MAX_VALUE
        var max = Long.MIN_VALUE
        var slow = 0
        var failed = 0
        var reads = 0
        var writes = 0
        val byType = LinkedHashMap<QueryType, Int>()
        val tableCounts = HashMap<String, Int>()
        val statementCounts = HashMap<String, Int>()
        val transactionIds = HashSet<Long>()

        for (query in queries) {
            val duration = query.durationNanos
            sum += duration
            if (duration < min) min = duration
            if (duration > max) max = duration
            if (duration >= slowNanos) slow++
            if (!query.success) failed++
            if (query.type.isRead) reads++ else if (query.type.isWrite) writes++
            byType[query.type] = (byType[query.type] ?: 0) + 1
            query.transactionId?.let(transactionIds::add)
            SqlTableExtractor.extract(query.sql).forEach { table ->
                tableCounts[table] = (tableCounts[table] ?: 0) + 1
            }
            val normalized = SqlFormatter.normalize(query.sql)
            statementCounts[normalized] = (statementCounts[normalized] ?: 0) + 1
        }

        return DatabaseAnalytics(
            totalQueries = total,
            averageDurationNanos = sum.toDouble() / total,
            minDurationNanos = min,
            maxDurationNanos = max,
            slowQueryCount = slow,
            failedQueryCount = failed,
            successRate = (total - failed).toDouble() / total,
            countByType = byType,
            readCount = reads,
            writeCount = writes,
            readWriteRatio = if (writes == 0) reads.toDouble() else reads.toDouble() / writes,
            transactionsExecuted = transactionIds.size,
            mostQueriedTables = tableCounts.entries
                .sortedByDescending { it.value }
                .take(TOP_N)
                .map { TableCount(it.key, it.value) },
            mostFrequentStatements = statementCounts.entries
                .sortedByDescending { it.value }
                .take(TOP_N)
                .map { StatementCount(it.key, it.value) },
            longestQueries = queries.sortedByDescending { it.durationNanos }.take(TOP_N),
        )
    }
}
