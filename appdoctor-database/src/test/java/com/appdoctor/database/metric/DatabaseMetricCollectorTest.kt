package com.appdoctor.database.metric

import com.appdoctor.database.model.DatabaseQuery
import com.appdoctor.database.model.QueryType
import com.appdoctor.database.repository.InMemoryDatabaseQueryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Test

class DatabaseMetricCollectorTest {

    @Test
    fun `snapshot reflects live repository and has stable id`() {
        val repository = InMemoryDatabaseQueryRepository(10)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val collector = DatabaseMetricCollector(repository, scope)
            assertEquals("database", collector.id)
            assertEquals(0, collector.snapshot().queries.size)

            repository.add(sample(1))
            assertEquals(1, collector.snapshot().queries.size)
            assertEquals(1L, collector.snapshot().queries.first().id)
        } finally {
            scope.cancel()
        }
    }

    private fun sample(id: Long) = DatabaseQuery(
        id = id, sql = "SELECT 1", type = QueryType.SELECT, durationNanos = 1L, timestampMillis = id,
        success = true, error = null, threadName = "t", databaseName = "app.db",
        rowsAffected = null, rowsReturned = 1, transactionId = null,
    )
}
