package com.appdoctor.compose.internal.runtime

import android.view.Choreographer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/** Raw frame sample derived from `Choreographer`. Not public API. */
internal data class FrameStats(
    val frameCount: Long,
    val frameDrops: Long,
) {
    companion object {
        val Empty: FrameStats = FrameStats(frameCount = 0L, frameDrops = 0L)
    }
}

/**
 * Counts UI frames and dropped frames. Behind an interface so the engine is testable without
 * a `Choreographer`/main looper. Not public API.
 */
internal interface FrameStatsProbe {
    /** Starts frame counting bound to [scope]; the callback is removed when the scope cancels. */
    fun activate(scope: CoroutineScope)

    /** Reads the current cumulative frame counters. */
    fun snapshot(): FrameStats
}

/**
 * [FrameStatsProbe] using the platform [Choreographer] — the same officially-supported
 * vsync signal the core `FpsMonitor` uses. A frame whose interval exceeds
 * [dropThresholdNanos] (default ≈ 1.5 frames at 60 Hz) is counted as a dropped/janky frame.
 *
 * The callback is posted on the main thread and removed on cancellation via a `finally`
 * block, so nothing keeps running once the collector stops observing. No `Window`, `View` or
 * `Context` is retained. Not public API.
 */
internal class ChoreographerFrameStatsProbe(
    private val dropThresholdNanos: Long = DEFAULT_DROP_THRESHOLD_NANOS,
    private val choreographerProvider: () -> Choreographer = { Choreographer.getInstance() },
) : FrameStatsProbe {

    private val frameCount = AtomicLong(0L)
    private val frameDrops = AtomicLong(0L)

    override fun activate(scope: CoroutineScope) {
        scope.launch(Dispatchers.Main) {
            val choreographer = choreographerProvider()
            var lastFrameNanos = 0L
            val callback = object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    if (lastFrameNanos != 0L) {
                        val delta = frameTimeNanos - lastFrameNanos
                        if (delta > 0L) {
                            frameCount.incrementAndGet()
                            if (delta > dropThresholdNanos) frameDrops.incrementAndGet()
                        }
                    }
                    lastFrameNanos = frameTimeNanos
                    choreographer.postFrameCallback(this)
                }
            }
            choreographer.postFrameCallback(callback)
            try {
                awaitCancellation()
            } finally {
                choreographer.removeFrameCallback(callback)
            }
        }
    }

    override fun snapshot(): FrameStats =
        FrameStats(frameCount = frameCount.get(), frameDrops = frameDrops.get())

    private companion object {
        /** ~1.5× a 60 Hz frame (25 ms): comfortably flags a missed vsync without false positives. */
        private const val DEFAULT_DROP_THRESHOLD_NANOS = 25_000_000L
    }
}
