package com.appdoctor.network.repository

import com.appdoctor.network.model.NetworkRequestRecord
import kotlinx.coroutines.flow.StateFlow

/** Thread-safe in-memory storage for captured network requests. */
public interface NetworkRequestRepository {
    /** Latest-first snapshot of captured requests. */
    public val requests: StateFlow<List<NetworkRequestRecord>>

    /** Adds a request and trims the history to the configured maximum. */
    public fun add(record: NetworkRequestRecord)

    /** Clears all captured requests. */
    public fun clear()
}
