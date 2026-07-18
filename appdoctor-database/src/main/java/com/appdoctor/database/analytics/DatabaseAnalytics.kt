package com.appdoctor.database.analytics

import com.appdoctor.database.model.DatabaseQuery
import com.appdoctor.database.model.QueryType

/** A table name paired with how many captured statements referenced it. */
public data class TableCount(public val table: String, public val count: Int)

/** A normalised SQL statement paired with how many times it ran. */
public data class StatementCount(public val sql: String, public val count: Int)

/**
 * Immutable aggregate of runtime database statistics, computed from captured queries by
 * [DatabaseAnalyticsComputer]. Adding a new metric is a purely additive change here.
 */
public data class DatabaseAnalytics(
    public val totalQueries: Int,
    public val averageDurationNanos: Double,
    public val minDurationNanos: Long,
    public val maxDurationNanos: Long,
    public val slowQueryCount: Int,
    public val failedQueryCount: Int,
    public val successRate: Double,
    public val countByType: Map<QueryType, Int>,
    public val readCount: Int,
    public val writeCount: Int,
    public val readWriteRatio: Double,
    public val transactionsExecuted: Int,
    public val mostQueriedTables: List<TableCount>,
    public val mostFrequentStatements: List<StatementCount>,
    public val longestQueries: List<DatabaseQuery>,
) {
    public companion object {
        /** The zero value shown before any query is captured. */
        public val Empty: DatabaseAnalytics = DatabaseAnalytics(
            totalQueries = 0,
            averageDurationNanos = 0.0,
            minDurationNanos = 0L,
            maxDurationNanos = 0L,
            slowQueryCount = 0,
            failedQueryCount = 0,
            successRate = 1.0,
            countByType = emptyMap(),
            readCount = 0,
            writeCount = 0,
            readWriteRatio = 0.0,
            transactionsExecuted = 0,
            mostQueriedTables = emptyList(),
            mostFrequentStatements = emptyList(),
            longestQueries = emptyList(),
        )
    }
}
