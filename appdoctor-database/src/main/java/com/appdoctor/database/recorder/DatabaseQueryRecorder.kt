package com.appdoctor.database.recorder

import com.appdoctor.database.model.DatabaseQuery
import com.appdoctor.database.model.QueryType
import com.appdoctor.database.repository.DatabaseQueryRepository
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Raw execution data captured by the SupportSQLite wrappers, before it is stamped with an
 * id/timestamp/thread and stored. Not part of the public API.
 */
internal class QueryExecution(
    val sql: String,
    val type: QueryType,
    val durationNanos: Long,
    val success: Boolean,
    val error: String?,
    val rowsAffected: Int?,
    val rowsReturned: Int?,
    val transactionId: Long?,
    val databaseName: String?,
)

/**
 * Ingestion seam between instrumentation and storage. The wrappers call [record] on the
 * executing thread; implementations must be cheap and non-blocking so query collection is
 * never delayed. Not part of the public API.
 */
internal interface DatabaseQueryRecorder {
    /** Fast gate the wrappers read to skip work entirely when capture is off. */
    val isRecording: Boolean

    /** Finalises and stores a captured execution. */
    fun record(execution: QueryExecution)
}

/**
 * Default recorder: stamps id/timestamp/thread and appends to the [repository]. Analytics
 * are intentionally NOT computed here — that is the analytics component's job.
 */
internal class DefaultDatabaseQueryRecorder(
    private val repository: DatabaseQueryRepository,
    private val enabled: AtomicBoolean,
    private val ids: AtomicLong = AtomicLong(0L),
    private val clock: () -> Long = System::currentTimeMillis,
) : DatabaseQueryRecorder {

    override val isRecording: Boolean get() = enabled.get()

    override fun record(execution: QueryExecution) {
        if (!enabled.get()) return
        repository.add(
            DatabaseQuery(
                id = ids.incrementAndGet(),
                sql = execution.sql,
                type = execution.type,
                durationNanos = execution.durationNanos,
                timestampMillis = clock(),
                success = execution.success,
                error = execution.error,
                threadName = Thread.currentThread().name,
                databaseName = execution.databaseName,
                rowsAffected = execution.rowsAffected,
                rowsReturned = execution.rowsReturned,
                transactionId = execution.transactionId,
            ),
        )
    }
}
