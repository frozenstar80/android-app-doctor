package com.appdoctor.compose

import com.appdoctor.compose.internal.runtime.BurstStats
import com.appdoctor.compose.internal.runtime.FrameStats
import com.appdoctor.compose.internal.runtime.FrameStatsProbe
import com.appdoctor.compose.internal.runtime.RecomposerRuntimeProbe
import com.appdoctor.compose.internal.runtime.RecomposerStats
import com.appdoctor.compose.model.TrackedComposable
import com.appdoctor.compose.tracking.ComposableTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Emits a scripted sequence of cumulative recomposition counts, clamped at the last value. */
internal class FakeRecomposerProbe(
    private val counts: List<Long>,
    private val burst: BurstStats = BurstStats.Empty,
    private val activeRecomposers: Int = 1,
) : RecomposerRuntimeProbe {
    private var index = 0
    override fun activate(scope: CoroutineScope) = Unit
    override fun sample(): RecomposerStats {
        val count = counts[index.coerceAtMost(counts.lastIndex)]
        index++
        return RecomposerStats(
            cumulativeRecompositions = count,
            activeRecomposers = activeRecomposers,
            pendingWork = false,
            burst = burst,
        )
    }
}

/** Returns a fixed [FrameStats] snapshot. */
internal class FakeFrameProbe(private val stats: FrameStats) : FrameStatsProbe {
    override fun activate(scope: CoroutineScope) = Unit
    override fun snapshot(): FrameStats = stats
}

/** A tracker stub with fixed active/disposal counts. */
internal class FakeTracker(
    override val activeCount: Int = 0,
    override val disposalCount: Long = 0L,
    trackedList: List<TrackedComposable> = emptyList(),
) : ComposableTracker {
    override val tracked: StateFlow<List<TrackedComposable>> = MutableStateFlow(trackedList)
    override val isEnabled: Boolean = true
    override fun onCommit(name: String, initialComposition: Boolean, depth: Int, screen: String?) = Unit
    override fun onDisposed(name: String) = Unit
    override fun setEnabled(enabled: Boolean) = Unit
    override fun clear() = Unit
}

/** A deterministic clock advancing by [stepNanos] on each read, starting at 0. */
internal class SequenceClock(private val stepNanos: Long = 1_000_000_000L) {
    private var current = -stepNanos
    fun next(): Long {
        current += stepNanos
        return current
    }
}
