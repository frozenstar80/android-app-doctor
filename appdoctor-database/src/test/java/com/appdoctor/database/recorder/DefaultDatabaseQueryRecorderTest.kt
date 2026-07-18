package com.appdoctor.database.recorder

import com.appdoctor.database.model.QueryType
import com.appdoctor.database.repository.InMemoryDatabaseQueryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class DefaultDatabaseQueryRecorderTest {

    @Test
    fun `records when enabled and assigns increasing ids`() {
        val repository = InMemoryDatabaseQueryRepository(10)
        val recorder = DefaultDatabaseQueryRecorder(repository, AtomicBoolean(true), clock = { 42L })
        recorder.record(exec("SELECT 1", QueryType.SELECT, rowsReturned = 1))
        recorder.record(exec("UPDATE t SET a=1", QueryType.UPDATE, rowsAffected = 3))

        val stored = repository.queries.value
        assertEquals(2, stored.size)
        assertEquals(listOf(2L, 1L), stored.map { it.id })
        assertEquals(42L, stored.first().timestampMillis)
        assertEquals(3, stored.first { it.type == QueryType.UPDATE }.rowsAffected)
    }

    @Test
    fun `does not record when disabled`() {
        val repository = InMemoryDatabaseQueryRepository(10)
        val recorder = DefaultDatabaseQueryRecorder(repository, AtomicBoolean(false))
        assertFalse(recorder.isRecording)
        recorder.record(exec("SELECT 1", QueryType.SELECT))
        assertTrue(repository.queries.value.isEmpty())
    }

    @Test
    fun `concurrent recording stays bounded and unique`() = runBlocking {
        val repository = InMemoryDatabaseQueryRepository(100)
        val recorder = DefaultDatabaseQueryRecorder(repository, AtomicBoolean(true))
        (0 until 400).map { async(Dispatchers.Default) { recorder.record(exec("SELECT $it", QueryType.SELECT)) } }.awaitAll()
        val stored = repository.queries.value
        assertEquals(100, stored.size)
        assertEquals(100, stored.map { it.id }.distinct().size)
    }

    private fun exec(sql: String, type: QueryType, rowsAffected: Int? = null, rowsReturned: Int? = null) =
        QueryExecution(sql, type, 1_000L, true, null, rowsAffected, rowsReturned, null, "app.db")
}
