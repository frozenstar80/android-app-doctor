package com.appdoctor.database.internal.sqlite

import androidx.sqlite.db.SupportSQLiteStatement
import com.appdoctor.database.model.QueryType
import com.appdoctor.database.recorder.DatabaseQueryRecorder
import com.appdoctor.database.recorder.QueryExecution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDoctorSupportSQLiteStatementTest {

    @Test
    fun `executeUpdateDelete records UPDATE with rows affected`() {
        val recorder = RecordingRecorder()
        val stmt = statement("UPDATE users SET name = ?", FakeStatement(updateDeleteResult = 7), recorder)
        assertEquals(7, stmt.executeUpdateDelete())

        val e = recorder.single()
        assertEquals(QueryType.UPDATE, e.type)
        assertEquals(7, e.rowsAffected)
        assertTrue(e.success)
        assertNull(e.rowsReturned)
    }

    @Test
    fun `executeInsert records INSERT with one row affected`() {
        val recorder = RecordingRecorder()
        val stmt = statement("INSERT INTO t VALUES (?)", FakeStatement(insertResult = 42L), recorder)
        assertEquals(42L, stmt.executeInsert())

        val e = recorder.single()
        assertEquals(QueryType.INSERT, e.type)
        assertEquals(1, e.rowsAffected)
        assertTrue(e.success)
    }

    @Test
    fun `failure is recorded and rethrown`() {
        val recorder = RecordingRecorder()
        val stmt = statement("UPDATE t SET a = 1", FakeStatement(throwOnExecute = IllegalStateException("boom")), recorder)

        val thrown = runCatching { stmt.executeUpdateDelete() }.exceptionOrNull()
        assertTrue(thrown is IllegalStateException)

        val e = recorder.single()
        assertFalse(e.success)
        assertEquals("boom", e.error)
    }

    @Test
    fun `does not record when recorder is idle`() {
        val recorder = RecordingRecorder(recording = false)
        val stmt = statement("SELECT 1", FakeStatement(), recorder)
        stmt.execute()
        assertTrue(recorder.executions.isEmpty())
    }

    @Test
    fun `tags statements with the open transaction id`() {
        val recorder = RecordingRecorder()
        val tracker = TransactionTracker()
        val stmt = AppDoctorSupportSQLiteStatement(FakeStatement(insertResult = 1L), "INSERT INTO t VALUES (?)", { recorder }, tracker, "app.db")

        tracker.begin()
        stmt.executeInsert()
        tracker.end()

        assertEquals(1L, recorder.single().transactionId)
    }

    private fun statement(sql: String, delegate: SupportSQLiteStatement, recorder: DatabaseQueryRecorder) =
        AppDoctorSupportSQLiteStatement(delegate, sql, { recorder }, TransactionTracker(), "app.db")

    private class RecordingRecorder(recording: Boolean = true) : DatabaseQueryRecorder {
        override val isRecording: Boolean = recording
        val executions = mutableListOf<QueryExecution>()
        override fun record(execution: QueryExecution) {
            executions += execution
        }

        fun single(): QueryExecution = executions.single()
    }

    private class FakeStatement(
        private val updateDeleteResult: Int = 0,
        private val insertResult: Long = -1L,
        private val throwOnExecute: RuntimeException? = null,
    ) : SupportSQLiteStatement {
        override fun execute() {
            throwOnExecute?.let { throw it }
        }

        override fun executeUpdateDelete(): Int {
            throwOnExecute?.let { throw it }
            return updateDeleteResult
        }

        override fun executeInsert(): Long {
            throwOnExecute?.let { throw it }
            return insertResult
        }

        override fun simpleQueryForLong(): Long = 0L
        override fun simpleQueryForString(): String? = null
        override fun bindNull(index: Int) = Unit
        override fun bindLong(index: Int, value: Long) = Unit
        override fun bindDouble(index: Int, value: Double) = Unit
        override fun bindString(index: Int, value: String) = Unit
        override fun bindBlob(index: Int, value: ByteArray) = Unit
        override fun clearBindings() = Unit
        override fun close() = Unit
    }
}
