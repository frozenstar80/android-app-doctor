package com.appdoctor.compose.tracking

import com.appdoctor.compose.model.TrackedComposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Bounded, thread-safe [ComposableTracker]. Distinct composables are keyed by name; when
 * more than [limit] names are tracked the least-recently-active one is evicted (a tracker is
 * a live view, not an audit log).
 *
 * Mutable [Entry] objects are updated in place under a [ReentrantLock] to avoid allocating on
 * every recomposition; the exposed [tracked] list is rebuilt as immutable
 * [TrackedComposable] snapshots on each change (mirroring the database module's repository).
 * Nothing here references a `Composition`, `Composer`, `Context` or `Activity`.
 */
public class InMemoryComposableTracker(
    limit: Int,
    private val clock: () -> Long = System::currentTimeMillis,
) : ComposableTracker {

    private val capacity: Int = limit.coerceAtLeast(1)
    private val lock = ReentrantLock()
    private val entries = LinkedHashMap<String, Entry>()

    private val mutableTracked = MutableStateFlow<List<TrackedComposable>>(emptyList())
    private val disposals = AtomicLong(0L)
    private val recording = AtomicBoolean(false)

    override val tracked: StateFlow<List<TrackedComposable>> = mutableTracked

    override val activeCount: Int
        get() = lock.withLock { entries.values.sumOf { it.aliveInstances.coerceAtLeast(0) } }

    override val disposalCount: Long get() = disposals.get()

    override val isEnabled: Boolean get() = recording.get()

    override fun onCommit(name: String, initialComposition: Boolean, depth: Int, screen: String?) {
        if (!recording.get()) return
        val now = clock()
        lock.withLock {
            val existing = entries[name]
            if (existing == null) {
                if (entries.size >= capacity) evictOldestLocked()
                entries[name] = Entry(
                    name = name,
                    recompositions = 0L,
                    firstComposedAtMillis = now,
                    lastRecomposedAtMillis = now,
                    depth = depth,
                    screen = screen,
                    aliveInstances = 1,
                )
            } else {
                existing.lastRecomposedAtMillis = now
                if (initialComposition) {
                    existing.aliveInstances += 1
                } else {
                    existing.recompositions += 1
                }
                if (existing.depth == TrackedComposable.UNKNOWN_DEPTH && depth != TrackedComposable.UNKNOWN_DEPTH) {
                    existing.depth = depth
                }
            }
            publishLocked()
        }
    }

    override fun onDisposed(name: String) {
        if (!recording.get()) return
        lock.withLock {
            val entry = entries[name] ?: return
            if (entry.aliveInstances > 0) entry.aliveInstances -= 1
            disposals.incrementAndGet()
            publishLocked()
        }
    }

    override fun setEnabled(enabled: Boolean) {
        recording.set(enabled)
    }

    override fun clear() {
        lock.withLock {
            entries.clear()
            disposals.set(0L)
            mutableTracked.value = emptyList()
        }
    }

    private fun evictOldestLocked() {
        val oldest = entries.values.minByOrNull { it.lastRecomposedAtMillis } ?: return
        entries.remove(oldest.name)
    }

    private fun publishLocked() {
        mutableTracked.value = entries.values.map { it.toModel() }
    }

    private class Entry(
        val name: String,
        var recompositions: Long,
        val firstComposedAtMillis: Long,
        var lastRecomposedAtMillis: Long,
        var depth: Int,
        val screen: String?,
        var aliveInstances: Int,
    ) {
        fun toModel(): TrackedComposable = TrackedComposable(
            name = name,
            recompositions = recompositions,
            firstComposedAtMillis = firstComposedAtMillis,
            lastRecomposedAtMillis = lastRecomposedAtMillis,
            depth = depth,
            screen = screen,
            disposed = aliveInstances <= 0,
        )
    }
}
