package com.appdoctor.database.internal.sqlite

import java.util.concurrent.atomic.AtomicLong

/**
 * Assigns a stable id to each (possibly nested) SQLite transaction, per thread — SQLite
 * transactions are connection/thread-confined. Statements executed while a transaction is
 * open are tagged with [current]. Not part of the public API.
 */
internal class TransactionTracker {

    private class Frame(val id: Long, var depth: Int)

    private val ids = AtomicLong(0L)
    private val local = ThreadLocal<Frame?>()

    /** Marks a transaction begin; nested begins reuse the outermost id. */
    fun begin(): Long {
        val frame = local.get()
        return if (frame == null) {
            val created = Frame(ids.incrementAndGet(), 1)
            local.set(created)
            created.id
        } else {
            frame.depth++
            frame.id
        }
    }

    /** Marks a transaction end; clears the frame when fully unwound. */
    fun end() {
        val frame = local.get() ?: return
        frame.depth--
        if (frame.depth <= 0) local.remove()
    }

    /** The id of the transaction currently open on this thread, or `null`. */
    fun current(): Long? = local.get()?.id
}
