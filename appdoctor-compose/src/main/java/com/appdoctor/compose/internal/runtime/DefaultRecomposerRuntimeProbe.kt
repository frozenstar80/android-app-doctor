package com.appdoctor.compose.internal.runtime

import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.RecomposerInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * [RecomposerRuntimeProbe] backed by the **stable** Compose runtime APIs:
 *
 *  - `Recomposer.runningRecomposers: StateFlow<Set<RecomposerInfo>>` — the live set of
 *    recomposers (one per window/root),
 *  - `RecomposerInfo.changeCount` — cumulative applied recomposition passes,
 *  - `RecomposerInfo.state: Flow<Recomposer.State>` — used to time busy bursts,
 *  - `RecomposerInfo.hasPendingWork` — whether work is queued right now.
 *
 * None of these require an experimental opt-in and none reach into Compose internals, so the
 * probe stays resilient across Compose releases. The probe holds **no** `Composition`,
 * `Composer`, `Context` or `Activity` reference — only the framework's own `RecomposerInfo`
 * handles, keyed by identity. Not public API.
 */
internal class DefaultRecomposerRuntimeProbe(
    private val runningRecomposers: () -> StateFlow<Set<RecomposerInfo>> = { Recomposer.runningRecomposers },
    private val clockNanos: () -> Long = System::nanoTime,
) : RecomposerRuntimeProbe {

    private val counter = RecompositionCounter<RecomposerInfo>()
    private val bursts = BurstAccumulator()

    override fun activate(scope: CoroutineScope) {
        scope.launch {
            val self = this
            val stateJobs = HashMap<RecomposerInfo, Job>()
            runningRecomposers().collect { recomposers ->
                for (info in recomposers) {
                    if (info !in stateJobs) {
                        stateJobs[info] = self.launch {
                            info.state.collect { state ->
                                bursts.onState(info, state == Recomposer.State.PendingWork, clockNanos())
                            }
                        }
                    }
                }
                val iterator = stateJobs.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (entry.key !in recomposers) {
                        entry.value.cancel()
                        bursts.forget(entry.key, clockNanos())
                        iterator.remove()
                    }
                }
            }
        }
    }

    override fun sample(): RecomposerStats {
        val recomposers = runningRecomposers().value
        val counts = HashMap<RecomposerInfo, Long>(recomposers.size)
        var pending = false
        for (info in recomposers) {
            counts[info] = info.changeCount
            if (info.hasPendingWork) pending = true
        }
        return RecomposerStats(
            cumulativeRecompositions = counter.update(counts),
            activeRecomposers = recomposers.size,
            pendingWork = pending,
            burst = bursts.snapshot(),
        )
    }
}
