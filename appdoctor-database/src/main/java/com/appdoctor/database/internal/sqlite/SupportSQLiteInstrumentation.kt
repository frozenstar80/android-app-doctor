package com.appdoctor.database.internal.sqlite

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import com.appdoctor.database.model.QueryType
import com.appdoctor.database.recorder.DatabaseQueryRecorder
import com.appdoctor.database.recorder.QueryExecution
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Delegating [SupportSQLiteOpenHelper.Factory] that transparently instruments any
 * SupportSQLite-backed database (Room, raw SQLite, future SQLDelight). Not part of the
 * public API — callers use `RoomDatabase.Builder.enableAppDoctor()` or
 * `AppDoctorDatabase.wrapOpenHelperFactory(...)`.
 */
internal class AppDoctorSQLiteOpenHelperFactory(
    private val delegate: SupportSQLiteOpenHelper.Factory,
    private val recorderProvider: () -> DatabaseQueryRecorder?,
) : SupportSQLiteOpenHelper.Factory {

    override fun create(
        configuration: SupportSQLiteOpenHelper.Configuration,
    ): SupportSQLiteOpenHelper = AppDoctorSupportSQLiteOpenHelper(
        delegate = delegate.create(configuration),
        recorderProvider = recorderProvider,
    )
}

/** Delegating open helper that wraps the writable/readable databases. */
internal class AppDoctorSupportSQLiteOpenHelper(
    private val delegate: SupportSQLiteOpenHelper,
    private val recorderProvider: () -> DatabaseQueryRecorder?,
) : SupportSQLiteOpenHelper {

    private val tracker = TransactionTracker()

    @Volatile private var lastDelegate: SupportSQLiteDatabase? = null
    @Volatile private var lastWrapped: SupportSQLiteDatabase? = null

    override val databaseName: String? get() = delegate.databaseName

    override fun setWriteAheadLoggingEnabled(enabled: Boolean): Unit =
        delegate.setWriteAheadLoggingEnabled(enabled)

    override val writableDatabase: SupportSQLiteDatabase get() = wrap(delegate.writableDatabase)
    override val readableDatabase: SupportSQLiteDatabase get() = wrap(delegate.readableDatabase)

    override fun close(): Unit = delegate.close()

    private fun wrap(db: SupportSQLiteDatabase): SupportSQLiteDatabase {
        val cached = lastWrapped
        if (cached != null && lastDelegate === db) return cached
        val wrapped = SupportSQLiteDatabaseProxy.create(db, databaseName, recorderProvider, tracker)
        lastDelegate = db
        lastWrapped = wrapped
        return wrapped
    }
}

/**
 * Creates a [Proxy] over [SupportSQLiteDatabase] that faithfully delegates every method and
 * instruments only the hot paths (`query`, `compileStatement`, `insert`/`update`/`delete`,
 * `execSQL`, and transaction boundaries). Using a Proxy keeps the wrapper resilient across
 * androidx.sqlite versions and free of ~40 hand-written delegations.
 */
internal object SupportSQLiteDatabaseProxy {

    private val EMPTY_ARGS = emptyArray<Any?>()

    fun create(
        delegate: SupportSQLiteDatabase,
        databaseName: String?,
        recorderProvider: () -> DatabaseQueryRecorder?,
        tracker: TransactionTracker,
    ): SupportSQLiteDatabase = Proxy.newProxyInstance(
        SupportSQLiteDatabase::class.java.classLoader,
        arrayOf(SupportSQLiteDatabase::class.java),
        Handler(delegate, databaseName, recorderProvider, tracker),
    ) as SupportSQLiteDatabase

    private enum class RowsFrom { INSERT, INT_RESULT, NONE }

    private class Handler(
        private val delegate: SupportSQLiteDatabase,
        private val databaseName: String?,
        private val recorderProvider: () -> DatabaseQueryRecorder?,
        private val tracker: TransactionTracker,
    ) : InvocationHandler {

        override fun invoke(proxy: Any?, method: Method, args: Array<out Any?>?): Any? {
            val a = args ?: EMPTY_ARGS
            val recorder = recorderProvider()?.takeIf { it.isRecording }
                ?: return invokeDelegate(method, a)

            return when (method.name) {
                "compileStatement" -> AppDoctorSupportSQLiteStatement(
                    delegate = invokeDelegate(method, a) as SupportSQLiteStatement,
                    sql = a[0] as String,
                    recorderProvider = recorderProvider,
                    tracker = tracker,
                    databaseName = databaseName,
                )

                "query" -> handleQuery(method, a, recorder)

                "insert" -> timedWrite(method, a, recorder, QueryType.INSERT, "INSERT INTO ${a[0]}", RowsFrom.INSERT)
                "update" -> timedWrite(method, a, recorder, QueryType.UPDATE, "UPDATE ${a[0]}", RowsFrom.INT_RESULT)
                "delete" -> timedWrite(method, a, recorder, QueryType.DELETE, "DELETE FROM ${a[0]}", RowsFrom.INT_RESULT)
                "execSQL" -> timedWrite(method, a, recorder, QueryType.fromSql(a[0] as String), a[0] as String, RowsFrom.NONE)

                "beginTransaction",
                "beginTransactionNonExclusive",
                "beginTransactionWithListener",
                "beginTransactionWithListenerNonExclusive",
                -> invokeDelegate(method, a).also { tracker.begin() }

                "endTransaction" -> invokeDelegate(method, a).also { tracker.end() }

                else -> invokeDelegate(method, a)
            }
        }

        private fun handleQuery(method: Method, args: Array<out Any?>, recorder: DatabaseQueryRecorder): Cursor {
            val sql = extractSql(args[0])
            val type = QueryType.fromSql(sql)
            val start = System.nanoTime()
            val transactionId = tracker.current()
            val cursor = try {
                invokeDelegate(method, args) as Cursor
            } catch (t: Throwable) {
                recorder.record(QueryExecution(sql, type, System.nanoTime() - start, false, t.message ?: t.javaClass.simpleName, null, null, transactionId, databaseName))
                throw t
            }
            return MeasuredCursor(cursor, start) { rows, duration, error ->
                recorder.record(QueryExecution(sql, type, duration, error == null, error, null, rows, transactionId, databaseName))
            }
        }

        private fun timedWrite(
            method: Method,
            args: Array<out Any?>,
            recorder: DatabaseQueryRecorder,
            type: QueryType,
            sql: String,
            rowsFrom: RowsFrom,
        ): Any? {
            val start = System.nanoTime()
            val transactionId = tracker.current()
            try {
                val result = invokeDelegate(method, args)
                val affected = when (rowsFrom) {
                    RowsFrom.INSERT -> if ((result as? Long ?: -1L) >= 0L) 1 else 0
                    RowsFrom.INT_RESULT -> result as? Int
                    RowsFrom.NONE -> null
                }
                recorder.record(QueryExecution(sql, type, System.nanoTime() - start, true, null, affected, null, transactionId, databaseName))
                return result
            } catch (t: Throwable) {
                recorder.record(QueryExecution(sql, type, System.nanoTime() - start, false, t.message ?: t.javaClass.simpleName, null, null, transactionId, databaseName))
                throw t
            }
        }

        private fun invokeDelegate(method: Method, args: Array<out Any?>): Any? = try {
            method.invoke(delegate, *args)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }

        private fun extractSql(arg: Any?): String = when (arg) {
            is String -> arg
            is SupportSQLiteQuery -> arg.sql
            else -> arg?.toString() ?: ""
        }
    }
}
