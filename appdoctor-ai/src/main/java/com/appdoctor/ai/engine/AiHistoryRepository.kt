package com.appdoctor.ai.engine

import com.appdoctor.ai.AiResponse

public class AiHistoryRepository(
    maximumEntries: Int,
) {
    private val bound: Int = maximumEntries.coerceAtLeast(1)
    private val lock = Any()
    private val entries = ArrayDeque<AiResponse>()

    public fun add(response: AiResponse) {
        synchronized(lock) {
            entries.addFirst(response)
            while (entries.size > bound) entries.removeLast()
        }
    }

    public fun all(): List<AiResponse> = synchronized(lock) { entries.toList() }

    public fun latest(): AiResponse? = synchronized(lock) { entries.firstOrNull() }
}
