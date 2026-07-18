package com.appdoctor.database.repository

import com.appdoctor.database.model.DatabaseQuery
import kotlinx.coroutines.flow.StateFlow

/** Thread-safe in-memory storage for captured SQL queries. */
public interface DatabaseQueryRepository {
    /** Latest-first snapshot of captured queries. */
    public val queries: StateFlow<List<DatabaseQuery>>

    /** Adds a query and trims the history to the configured maximum. */
    public fun add(query: DatabaseQuery)

    /** Clears all captured queries. */
    public fun clear()
}
