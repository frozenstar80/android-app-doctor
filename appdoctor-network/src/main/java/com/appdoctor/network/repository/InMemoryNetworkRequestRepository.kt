package com.appdoctor.network.repository

import com.appdoctor.network.model.NetworkRequestRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** Bounded, thread-safe in-memory request store. */
public class InMemoryNetworkRequestRepository(
    maxRequests: Int,
) : NetworkRequestRepository {

    private val capacity: Int = maxRequests.coerceAtLeast(1)
    private val lock = ReentrantLock()
    private val mutableRequests = MutableStateFlow<List<NetworkRequestRecord>>(emptyList())

    override val requests: StateFlow<List<NetworkRequestRecord>> = mutableRequests

    override fun add(record: NetworkRequestRecord) {
        lock.withLock {
            val updated = ArrayList<NetworkRequestRecord>(mutableRequests.value.size + 1)
            updated += record
            updated += mutableRequests.value
            if (updated.size > capacity) {
                updated.subList(capacity, updated.size).clear()
            }
            mutableRequests.value = updated
        }
    }

    override fun clear() {
        lock.withLock {
            mutableRequests.value = emptyList()
        }
    }
}
