package com.appdoctor.ai.engine

import com.appdoctor.ai.AiResponse

public class AiCache(
    maximumSize: Int,
) {
    private val bound: Int = maximumSize.coerceAtLeast(1)
    private val lock = Any()
    private val lru = object : LinkedHashMap<String, AiResponse>(bound, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, AiResponse>?): Boolean =
            size > bound
    }

    public fun get(sessionId: String): AiResponse? = synchronized(lock) { lru[sessionId] }

    public fun put(sessionId: String, response: AiResponse) {
        synchronized(lock) { lru[sessionId] = response }
    }

    public fun clear() {
        synchronized(lock) { lru.clear() }
    }
}
