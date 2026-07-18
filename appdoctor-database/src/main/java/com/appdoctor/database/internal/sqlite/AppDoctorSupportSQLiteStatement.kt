package com.appdoctor.database.internal.sqlite

import androidx.sqlite.db.SupportSQLiteStatement
import com.appdoctor.database.model.QueryType
import com.appdoctor.database.recorder.DatabaseQueryRecorder
import com.appdoctor.database.recorder.QueryExecution

/**
 * Wraps a compiled [SupportSQLiteStatement] to time its execute* calls and record rows
 * affected/returned. Binds are delegated untouched (bind args are not captured).
 *
 * Not part of the public API.
 */
internal class AppDoctorSupportSQLiteStatement(
    private val delegate: SupportSQLiteStatement,
    private val sql: String,
    private val recorderProvider: () -> DatabaseQueryRecorder?,
    private val tracker: TransactionTracker,
    private val databaseName: String?,
) : SupportSQLiteStatement {

    private val type: QueryType = QueryType.fromSql(sql)

    override fun bindNull(index: Int): Unit = delegate.bindNull(index)
    override fun bindLong(index: Int, value: Long): Unit = delegate.bindLong(index, value)
    override fun bindDouble(index: Int, value: Double): Unit = delegate.bindDouble(index, value)
    override fun bindString(index: Int, value: String): Unit = delegate.bindString(index, value)
    override fun bindBlob(index: Int, value: ByteArray): Unit = delegate.bindBlob(index, value)
    override fun clearBindings(): Unit = delegate.clearBindings()
    override fun close(): Unit = delegate.close()

    override fun execute() {
        val recorder = activeRecorder() ?: return delegate.execute()
        val start = System.nanoTime()
        val transactionId = tracker.current()
        try {
            delegate.execute()
            record(recorder, start, success = true, error = null, rowsAffected = null, rowsReturned = null, transactionId)
        } catch (t: Throwable) {
            record(recorder, start, success = false, error = t.message ?: t.javaClass.simpleName, rowsAffected = null, rowsReturned = null, transactionId)
            throw t
        }
    }

    override fun executeUpdateDelete(): Int {
        val recorder = activeRecorder() ?: return delegate.executeUpdateDelete()
        val start = System.nanoTime()
        val transactionId = tracker.current()
        try {
            val affected = delegate.executeUpdateDelete()
            record(recorder, start, success = true, error = null, rowsAffected = affected, rowsReturned = null, transactionId)
            return affected
        } catch (t: Throwable) {
            record(recorder, start, success = false, error = t.message ?: t.javaClass.simpleName, rowsAffected = null, rowsReturned = null, transactionId)
            throw t
        }
    }

    override fun executeInsert(): Long {
        val recorder = activeRecorder() ?: return delegate.executeInsert()
        val start = System.nanoTime()
        val transactionId = tracker.current()
        try {
            val rowId = delegate.executeInsert()
            record(recorder, start, success = true, error = null, rowsAffected = if (rowId >= 0L) 1 else 0, rowsReturned = null, transactionId)
            return rowId
        } catch (t: Throwable) {
            record(recorder, start, success = false, error = t.message ?: t.javaClass.simpleName, rowsAffected = null, rowsReturned = null, transactionId)
            throw t
        }
    }

    override fun simpleQueryForLong(): Long {
        val recorder = activeRecorder() ?: return delegate.simpleQueryForLong()
        val start = System.nanoTime()
        val transactionId = tracker.current()
        try {
            val result = delegate.simpleQueryForLong()
            record(recorder, start, success = true, error = null, rowsAffected = null, rowsReturned = 1, transactionId)
            return result
        } catch (t: Throwable) {
            record(recorder, start, success = false, error = t.message ?: t.javaClass.simpleName, rowsAffected = null, rowsReturned = null, transactionId)
            throw t
        }
    }

    override fun simpleQueryForString(): String? {
        val recorder = activeRecorder() ?: return delegate.simpleQueryForString()
        val start = System.nanoTime()
        val transactionId = tracker.current()
        try {
            val result = delegate.simpleQueryForString()
            record(recorder, start, success = true, error = null, rowsAffected = null, rowsReturned = 1, transactionId)
            return result
        } catch (t: Throwable) {
            record(recorder, start, success = false, error = t.message ?: t.javaClass.simpleName, rowsAffected = null, rowsReturned = null, transactionId)
            throw t
        }
    }

    private fun activeRecorder(): DatabaseQueryRecorder? = recorderProvider()?.takeIf { it.isRecording }

    private fun record(
        recorder: DatabaseQueryRecorder,
        startNanos: Long,
        success: Boolean,
        error: String?,
        rowsAffected: Int?,
        rowsReturned: Int?,
        transactionId: Long?,
    ) {
        recorder.record(
            QueryExecution(
                sql = sql,
                type = type,
                durationNanos = System.nanoTime() - startNanos,
                success = success,
                error = error,
                rowsAffected = rowsAffected,
                rowsReturned = rowsReturned,
                transactionId = transactionId,
                databaseName = databaseName,
            ),
        )
    }
}
