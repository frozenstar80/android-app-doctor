package com.appdoctor.core.monitor.cpu

import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import com.appdoctor.core.monitor.Monitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.io.File

/**
 * Estimates this process's CPU usage by sampling `/proc/self/stat`.
 *
 * The value is a delta measurement: on each tick it reads the accumulated
 * `utime + stime` (in clock ticks), diffs it against the previous read, and divides by
 * the elapsed wall-clock time and the core count to get a percentage of total capacity.
 *
 * Reading only `/proc/self` (the app's own process) requires no permission and works on
 * API 24+. If the file cannot be read the monitor degrades gracefully to `0%`.
 *
 * The sampling loop only runs while [data] is being collected.
 *
 * @param scope scope the hot [StateFlow] is shared in.
 * @param intervalMillis delay between samples.
 * @param coreCount processors to normalise against.
 * @param clockTicksPerSecond kernel `USER_HZ`; injected for testability.
 * @param statReader supplies raw `/proc/self/stat` contents; injected for testability.
 * @param elapsedRealtimeMillis monotonic clock source; injected for testability.
 */
public class CpuMonitor(
    scope: CoroutineScope,
    private val intervalMillis: Long = DEFAULT_INTERVAL_MS,
    private val coreCount: Int = Runtime.getRuntime().availableProcessors(),
    private val clockTicksPerSecond: Long = Os.sysconf(OsConstants._SC_CLK_TCK),
    private val statReader: () -> String? = ::readProcSelfStat,
    private val elapsedRealtimeMillis: () -> Long = SystemClock::elapsedRealtime,
) : Monitor<CpuInfo> {

    override val data: StateFlow<CpuInfo> =
        flow {
            var lastTicks = -1L
            var lastTimeMs = -1L
            while (true) {
                val ticks = readProcessTicks()
                val nowMs = elapsedRealtimeMillis()
                if (ticks >= 0L && lastTicks >= 0L && lastTimeMs >= 0L) {
                    emit(CpuInfo(computePercent(ticks - lastTicks, nowMs - lastTimeMs), coreCount))
                }
                if (ticks >= 0L) {
                    lastTicks = ticks
                    lastTimeMs = nowMs
                }
                delay(intervalMillis)
            }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CpuInfo.Empty)

    private fun computePercent(deltaTicks: Long, deltaTimeMs: Long): Float {
        val deltaSeconds = deltaTimeMs / 1_000.0
        if (deltaSeconds <= 0.0 || clockTicksPerSecond <= 0L || coreCount <= 0) return 0f
        val usedCoreSeconds = deltaTicks.toDouble() / clockTicksPerSecond.toDouble()
        val percent = usedCoreSeconds / (deltaSeconds * coreCount) * 100.0
        return percent.coerceIn(0.0, 100.0).toFloat()
    }

    private fun readProcessTicks(): Long =
        statReader()?.let(::parseProcessTicks) ?: -1L

    internal companion object {
        private const val DEFAULT_INTERVAL_MS = 1_000L
        private const val STOP_TIMEOUT_MS = 2_000L

        /**
         * Parses `utime + stime` (in ticks) out of a `/proc/<pid>/stat` line.
         *
         * The `comm` field (index 2) may itself contain spaces and parentheses, so we
         * anchor parsing on the final `')'` and index fields relative to `state`.
         * Returns `-1` if the line cannot be parsed.
         */
        internal fun parseProcessTicks(stat: String): Long {
            val afterComm = stat.lastIndexOf(')')
            if (afterComm < 0 || afterComm + 2 > stat.length) return -1L
            val fields = stat.substring(afterComm + 1)
                .trim()
                .split(Regex("\\s+"))
            // Relative to `state` (field 3): utime is field 14 -> index 11, stime -> index 12.
            if (fields.size <= 12) return -1L
            val utime = fields[11].toLongOrNull() ?: return -1L
            val stime = fields[12].toLongOrNull() ?: return -1L
            return utime + stime
        }

        private fun readProcSelfStat(): String? = try {
            File("/proc/self/stat").readText()
        } catch (t: Throwable) {
            null
        }
    }
}
