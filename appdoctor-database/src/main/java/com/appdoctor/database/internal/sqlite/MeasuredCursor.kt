package com.appdoctor.database.internal.sqlite

import android.database.Cursor
import android.database.CursorWrapper

/**
 * A [CursorWrapper] that times a SELECT from acquisition to [close] and reports rows
 * returned — driven by the caller's own iteration, so no result is materialised eagerly
 * and no cursor is ever retained by AppDoctor (only primitives escape via [onComplete]).
 *
 * Not part of the public API.
 */
internal class MeasuredCursor(
    private val inner: Cursor,
    private val startNanos: Long,
    private val onComplete: (rowsReturned: Int?, durationNanos: Long, error: String?) -> Unit,
) : CursorWrapper(inner) {

    private var accessed = false
    private var error: String? = null
    private var reported = false

    override fun getCount(): Int = track { inner.count }
    override fun moveToFirst(): Boolean = track { super.moveToFirst() }
    override fun moveToNext(): Boolean = track { super.moveToNext() }
    override fun moveToLast(): Boolean = track { super.moveToLast() }
    override fun moveToPosition(position: Int): Boolean = track { super.moveToPosition(position) }
    override fun move(offset: Int): Boolean = track { super.move(offset) }

    override fun close() {
        report()
        super.close()
    }

    private inline fun <R> track(block: () -> R): R {
        accessed = true
        return try {
            block()
        } catch (t: Throwable) {
            error = t.message ?: t.javaClass.simpleName
            report()
            throw t
        }
    }

    private fun report() {
        if (reported) return
        reported = true
        val rows = if (accessed && error == null) runCatching { inner.count }.getOrNull() else null
        onComplete(rows, System.nanoTime() - startNanos, error)
    }
}
