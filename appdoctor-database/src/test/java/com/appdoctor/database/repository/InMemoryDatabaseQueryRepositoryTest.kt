package com.appdoctor.database.repository

import com.appdoctor.database.model.DatabaseQuery
import com.appdoctor.database.model.QueryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryDatabaseQueryRepositoryTest {

    @Test
    fun `keeps only latest max queries newest first`() {
        val repository = InMemoryDatabaseQueryRepository(maxQueries = 3)
        repeat(5) { repository.add(sample(it.toLong())) }
        assertEquals(listOf(4L, 3L, 2L), repository.queries.value.map { it.id })
    }

    @Test
    fun `clear empties the store`() {
        val repository = InMemoryDatabaseQueryRepository(maxQueries = 10)
        repository.add(sample(1L))
        repository.clear()
        assertTrue(repository.queries.value.isEmpty())
    }

    @Test
    fun `supports concurrent writers safely`() = runBlocking {
        val repository = InMemoryDatabaseQueryRepository(maxQueries = 100)
        (0 until 500).map { index ->
            async(Dispatchers.Default) { repository.add(sample(index.toLong())) }
        }.awaitAll()

        val snapshot = repository.queries.value
        assertEquals(100, snapshot.size)
        assertEquals(100, snapshot.map { it.id }.distinct().size)
        assertTrue(snapshot.all { it.id in 0L..499L })
    }

    private fun sample(id: Long) = DatabaseQuery(
        id = id, sql = "SELECT * FROM t WHERE id = $id", type = QueryType.SELECT,
        durationNanos = id, timestampMillis = id, success = true, error = null,
        threadName = "t", databaseName = "app.db", rowsAffected = null, rowsReturned = 1, transactionId = null,
    )
}
