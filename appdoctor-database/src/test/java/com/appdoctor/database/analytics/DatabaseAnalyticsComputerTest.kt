package com.appdoctor.database.analytics

import com.appdoctor.database.model.DatabaseQuery
import com.appdoctor.database.model.QueryType
import org.junit.Assert.assertEquals
import org.junit.Test

class DatabaseAnalyticsComputerTest {

    @Test
    fun `empty input yields empty analytics`() {
        assertEquals(DatabaseAnalytics.Empty, DatabaseAnalyticsComputer.compute(emptyList(), 16L))
    }

    @Test
    fun `aggregates durations types transactions and rankings`() {
        val queries = listOf(
            q(1, "SELECT * FROM users", QueryType.SELECT, ms = 5, success = true, txn = 1),
            q(2, "SELECT * FROM users WHERE id = 1", QueryType.SELECT, ms = 25, success = true, txn = 1),
            q(3, "INSERT INTO orders VALUES (?)", QueryType.INSERT, ms = 2, success = true, txn = 2),
            q(4, "UPDATE users SET name = ?", QueryType.UPDATE, ms = 40, success = false, txn = null),
        )
        val a = DatabaseAnalyticsComputer.compute(queries, slowThresholdMillis = 16L)

        assertEquals(4, a.totalQueries)
        assertEquals(2, a.slowQueryCount) // 25ms, 40ms
        assertEquals(1, a.failedQueryCount)
        assertEquals(0.75, a.successRate, 1e-9)
        assertEquals(2, a.readCount)
        assertEquals(2, a.writeCount)
        assertEquals(2, a.transactionsExecuted) // ids {1, 2}
        assertEquals(2, a.countByType[QueryType.SELECT])
        assertEquals(2_000_000L, a.minDurationNanos)
        assertEquals(40_000_000L, a.maxDurationNanos)
        assertEquals("users", a.mostQueriedTables.first().table)
        assertEquals(4L, a.longestQueries.first().id) // 40ms is longest
    }

    private fun q(id: Long, sql: String, type: QueryType, ms: Long, success: Boolean, txn: Long?) = DatabaseQuery(
        id = id, sql = sql, type = type, durationNanos = ms * 1_000_000L, timestampMillis = id,
        success = success, error = if (success) null else "boom", threadName = "t",
        databaseName = "app.db", rowsAffected = null, rowsReturned = null, transactionId = txn,
    )
}
