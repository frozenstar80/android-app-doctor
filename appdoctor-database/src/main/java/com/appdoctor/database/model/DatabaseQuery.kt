package com.appdoctor.database.model

/** Immutable record of a single executed SQL statement. */
public data class DatabaseQuery(
    public val id: Long,
    public val sql: String,
    public val type: QueryType,
    public val durationNanos: Long,
    public val timestampMillis: Long,
    public val success: Boolean,
    public val error: String?,
    public val threadName: String,
    public val databaseName: String?,
    public val rowsAffected: Int?,
    public val rowsReturned: Int?,
    public val transactionId: Long?,
) {
    /** Execution duration in milliseconds (derived from [durationNanos]). */
    public val durationMillis: Double get() = durationNanos / 1_000_000.0
}
