package com.appdoctor.database.repository

import com.appdoctor.database.model.DatabaseQuery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** Bounded, thread-safe in-memory query store. Old entries are discarded automatically. */
public class InMemoryDatabaseQueryRepository(
    maxQueries: Int,
) : DatabaseQueryRepository {

    private val capacity: Int = maxQueries.coerceAtLeast(1)
    private val lock = ReentrantLock()
    private val mutableQueries = MutableStateFlow<List<DatabaseQuery>>(emptyList())

    override val queries: StateFlow<List<DatabaseQuery>> = mutableQueries

    override fun add(query: DatabaseQuery) {
        lock.withLock {
            val updated = ArrayList<DatabaseQuery>(mutableQueries.value.size + 1)
            updated += query
            updated += mutableQueries.value
            if (updated.size > capacity) {
                updated.subList(capacity, updated.size).clear()
            }
            mutableQueries.value = updated
        }
    }

    override fun clear() {
        lock.withLock {
            mutableQueries.value = emptyList()
        }
    }
}
