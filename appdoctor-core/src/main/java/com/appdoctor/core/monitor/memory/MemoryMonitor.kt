package com.appdoctor.core.monitor.memory

import android.os.Debug
import com.appdoctor.core.monitor.Monitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/**
 * Polls JVM and native heap usage on a fixed interval and exposes it as [MemoryInfo].
 *
 * The polling loop only runs while [data] is being collected (dashboard open), so an
 * idle app pays no cost.
 *
 * All collaborators are injected for testability: pass a fake [runtime] or
 * [nativeHeapProvider] in unit tests.
 *
 * @param scope scope the hot [StateFlow] is shared in (typically a background scope).
 * @param intervalMillis delay between samples.
 * @param runtime source of heap figures; defaults to the process [Runtime].
 * @param nativeHeapProvider source of native heap allocation size.
 */
public class MemoryMonitor(
    scope: CoroutineScope,
    private val intervalMillis: Long = DEFAULT_INTERVAL_MS,
    private val runtime: Runtime = Runtime.getRuntime(),
    private val nativeHeapProvider: () -> Long = { Debug.getNativeHeapAllocatedSize() },
) : Monitor<MemoryInfo> {

    override val data: StateFlow<MemoryInfo> =
        flow {
            while (true) {
                emit(read())
                delay(intervalMillis)
            }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MemoryInfo.Empty)

    private fun read(): MemoryInfo {
        val max = runtime.maxMemory()
        val total = runtime.totalMemory()
        val free = runtime.freeMemory()
        val used = total - free
        return MemoryInfo(
            usedBytes = used,
            maxBytes = max,
            freeBytes = (max - used).coerceAtLeast(0L),
            nativeAllocatedBytes = nativeHeapProvider(),
        )
    }

    private companion object {
        private const val DEFAULT_INTERVAL_MS = 1_000L
        private const val STOP_TIMEOUT_MS = 2_000L
    }
}
