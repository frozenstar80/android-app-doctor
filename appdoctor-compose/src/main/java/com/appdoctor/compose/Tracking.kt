package com.appdoctor.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import com.appdoctor.compose.model.TrackedComposable

/**
 * Optional hierarchy depth for tracked composables. Provide it around a subtree to record
 * how deep a `TrackRecompositions` call sits; defaults to [TrackedComposable.UNKNOWN_DEPTH].
 *
 * Reading a `CompositionLocal` does **not** cause extra recomposition here — it is only read
 * inside a [SideEffect], never used to drive UI.
 */
public val LocalComposeTrackingDepth: androidx.compose.runtime.ProvidableCompositionLocal<Int> =
    compositionLocalOf { TrackedComposable.UNKNOWN_DEPTH }

/**
 * Opt-in runtime tracking for a single composable.
 *
 * Drop this call inside any composable you want to observe:
 * ```kotlin
 * @Composable
 * fun ProductCard(product: Product) {
 *     TrackRecompositions("ProductCard")
 *     // …your UI…
 * }
 * ```
 *
 * ### Why it can't cause recompositions
 * The only remembered state is a tiny per-instance commit counter. The counter is read and
 * incremented inside [SideEffect], which runs **after** a successful composition and reads no
 * snapshot state, and the value is forwarded to a plain, non-Compose sink
 * ([AppDoctorCompose.activeTracker]). Because nothing here reads or writes Compose `State`,
 * this composable can never invalidate itself or its parent — no recomposition loops.
 *
 * It is a complete no-op unless the Compose inspector is installed **and**
 * [com.appdoctor.core.AppDoctorConfig.enableComposableTracking] is `true`, so it is safe to
 * leave in place in every build.
 *
 * @param name stable identifier shown in the dashboard. Give distinct instances distinct
 *   names (e.g. include an id) if you want them counted separately.
 */
@Composable
public fun TrackRecompositions(name: String) {
    val depth = LocalComposeTrackingDepth.current
    // One tiny per-instance counter; distinguishes the initial composition from recompositions.
    val commits = remember { IntArray(1) }

    SideEffect {
        val initial = commits[0] == 0
        commits[0] = commits[0] + 1
        AppDoctorCompose.activeTracker?.onCommit(
            name = name,
            initialComposition = initial,
            depth = depth,
            screen = AppDoctorCompose.currentScreen,
        )
    }

    DisposableEffect(name) {
        onDispose { AppDoctorCompose.activeTracker?.onDisposed(name) }
    }
}

/**
 * Convenience that records the current screen name for the lifetime of a composition (e.g.
 * the content of a navigation destination) and clears it on exit. Purely optional; sets only
 * a string on [AppDoctorCompose.currentScreen] and never retains anything.
 */
@Composable
public fun TrackScreen(name: String) {
    DisposableEffect(name) {
        AppDoctorCompose.currentScreen = name
        onDispose {
            if (AppDoctorCompose.currentScreen == name) AppDoctorCompose.currentScreen = null
        }
    }
}
