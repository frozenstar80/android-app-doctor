package com.appdoctor.core.monitor.fps

import android.view.Choreographer
import com.appdoctor.core.monitor.Monitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/**
 * Measures UI frame rate using [Choreographer] frame callbacks.
 *
 * On every vsync it computes the delta since the previous frame and derives an
 * instantaneous FPS, a windowed average and the lowest value seen. Because a frame
 * callback re-posts itself, the monitor drives one measurement per displayed frame.
 *
 * The [Choreographer] is driven on the main thread; the resulting flow is shared via
 * [StateFlow] so the callback is only registered while the dashboard observes it.
 *
 * @param scope scope the hot [StateFlow] is shared in.
 * @param windowSize number of recent frame intervals used for the rolling average.
 * @param maxFps upper clamp for reported values (guards against tiny-delta spikes).
 * @param choreographerProvider supplies the [Choreographer]; injected for testability.
 */
public class FpsMonitor(
    scope: CoroutineScope,
    private val windowSize: Int = DEFAULT_WINDOW,
    private val maxFps: Float = DEFAULT_MAX_FPS,
    private val choreographerProvider: () -> Choreographer = { Choreographer.getInstance() },
) : Monitor<FpsInfo> {

    override val data: StateFlow<FpsInfo> =
        callbackFlow {
            val intervalsNanos = ArrayDeque<Long>(windowSize)
            var lastFrameNanos = 0L
            var lowest = Float.NaN
            val choreographer = choreographerProvider()

            val callback = object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    if (lastFrameNanos != 0L) {
                        val deltaNanos = frameTimeNanos - lastFrameNanos
                        if (deltaNanos > 0L) {
                            val instant = (NANOS_PER_SECOND / deltaNanos).toFloat().coerceAtMost(maxFps)

                            intervalsNanos.addLast(deltaNanos)
                            while (intervalsNanos.size > windowSize) intervalsNanos.removeFirst()
                            val averageDeltaNanos = intervalsNanos.sum().toDouble() / intervalsNanos.size
                            val average = if (averageDeltaNanos > 0.0) {
                                (NANOS_PER_SECOND / averageDeltaNanos).toFloat().coerceAtMost(maxFps)
                            } else {
                                0f
                            }

                            lowest = if (lowest.isNaN()) instant else minOf(lowest, instant)
                            trySend(FpsInfo(current = instant, average = average, lowest = lowest))
                        }
                    }
                    lastFrameNanos = frameTimeNanos
                    choreographer.postFrameCallback(this)
                }
            }

            choreographer.postFrameCallback(callback)
            awaitClose { choreographer.removeFrameCallback(callback) }
        }
            .flowOn(Dispatchers.Main)
            .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), FpsInfo.Empty)

    private companion object {
        private const val DEFAULT_WINDOW = 120
        private const val DEFAULT_MAX_FPS = 120f
        private const val STOP_TIMEOUT_MS = 2_000L
        private const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
