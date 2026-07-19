# 🧬 AppDoctor Compose Runtime Inspector

`appdoctor-compose` is Phase 4 of AppDoctor: a **runtime** Compose metrics collector with
optional per-composable tracking, an optional analytics engine, and a dedicated dashboard
tab.

It is **not** the Android Studio Layout Inspector, **not** a Compose Preview tool, and **not**
a UI editor. It does not read your layout tree, node bounds or modifiers — it observes how
Compose *behaves at runtime* (recompositions, frames) using only **stable, public** Compose
APIs.

## What it captures

Global, automatic (no code changes):

- **Recomposition count** — cumulative applied recomposition passes since process start.
- **Recomposition rate** — recompositions per second over the sampling window.
- **Average / longest recomposition duration** — approximated from recomposer busy bursts.
- **Composition count** — number of active recomposition roots (running `Recomposer`s).
- **Frame count / frame drops** — via `Choreographer`, since first observed.
- **Current screen** — optional, when you set it.

Opt-in per-composable (off by default):

- name, **recomposition count**, first composed, last recomposed, **lifetime**, optional
  hierarchy **depth**, screen, and disposal.

## Architecture

```
Recomposer.runningRecomposers (stable StateFlow<Set<RecomposerInfo>>)   Choreographer
        │  RecomposerInfo.changeCount / .state / .hasPendingWork              │
   RecomposerRuntimeProbe  ── RecompositionCounter (monotonic)          FrameStatsProbe
        │                  ── BurstAccumulator (PendingWork→Idle timing)      │
        └───────────────────────────┬─────────────────────────────────────────┘
                                     ▼
              ComposeRuntimeCollectorEngine  → StateFlow<ComposeRuntimeSnapshot>
                                     ▼            (WhileSubscribed → idle-cost 0)
              ComposeMetricCollector : MetricCollector  → CollectorRegistry (id "compose")

opt-in:  TrackRecompositions("Name")  →  AppDoctorCompose (sink)
              → InMemoryComposableTracker (bounded StateFlow<List<TrackedComposable>>)

optional: ComposeAnalyticsComputer (pure) + ComposeAnalyticsEngine (live)  ← consume only
```

The module self-registers through the existing `ServiceLoader` plugin SPI
(`AppDoctorComposePluginFactory` under
`META-INF/services/com.appdoctor.core.plugin.AppDoctorPluginFactory`); no `appdoctor-core`
change is needed to add it. The plugin exposes its collector via `MetricCollectorProvider`, so
it lands in `AppDoctor.collectors` automatically (stable id `"compose"`).

## Collector design

`ComposeMetricCollector` implements the existing `MetricCollector` contract, exposing the
engine's hot `StateFlow<ComposeRuntimeSnapshot>` and a fresh `snapshot()` / `current()` read.
**The collector performs no analytics** — it only measures and exposes runtime values.

Recomposition data comes from the **stable, non-experimental** Compose runtime surface:

- `Recomposer.runningRecomposers: StateFlow<Set<RecomposerInfo>>` — the live set of
  recomposers (one per window/root),
- `RecomposerInfo.changeCount: Long` — cumulative applied recomposition passes,
- `RecomposerInfo.state: Flow<Recomposer.State>` — used to time `PendingWork → Idle` bursts,
- `RecomposerInfo.hasPendingWork: Boolean`.

`RecompositionCounter` folds per-recomposer `changeCount` deltas into a **monotonic** total
that never dips when a recomposer disappears. `BurstAccumulator` turns state transitions into
average/longest busy-burst durations. Frames come from a `Choreographer` frame callback (the
same officially-supported vsync signal the core `FpsMonitor` uses).

## Component tracking (optional, off by default)

Global metrics require **no** code changes. To observe *individual* composables, opt in:

```kotlin
@Composable
fun ProductCard(product: Product) {
    TrackRecompositions("ProductCard")   // no-op unless tracking is enabled
    // …your UI…
}
```

`TrackRecompositions` increments a tiny per-instance counter inside a `SideEffect` and forwards
it to a process-wide sink — it reads/writes **no** Compose `State`, so it can never invalidate
itself or its parent (no recomposition loops). It is a complete no-op unless the inspector is
installed **and** `enableComposableTracking = true`, so it is safe to leave in place.

> Because `appdoctor-compose` is typically a **debug-only** dependency, calls to
> `TrackRecompositions` must sit in debug-only code, or the module must be added in all
> variants (it self-disables in release regardless). Optional hierarchy depth is provided via
> `LocalComposeTrackingDepth`; `TrackScreen("Home")` records the current screen name.

## Analytics architecture (optional)

Analytics is a fully independent consumer, off by default:

- `ComposeAnalyticsComputer` — a **pure**, stateless function turning a runtime snapshot +
  tracked list into `ComposeAnalytics`. New metrics are additive here and never touch the
  collector.
- `ComposeAnalyticsEngine` — recomputes reactively on `Dispatchers.Default` with `mapLatest`
  (which cancels superseded computations), so aggregation never blocks sampling. Created only
  when `enableComposeAnalytics = true`.

Analytics includes: total recompositions, average recompositions/second, most/least recomposed
composables, longest-living composables, highest recomposition frequency, frame statistics and
per-screen statistics.

## Dashboard tab

- **Overview** — recompositions, rate, active recomposers, tracked (active), frames.
- **Live Metrics** — recompositions/sec sparkline, average/longest recomposition, disposals,
  frame-drop rate and a frame-drop timeline (lightweight `Canvas` charts).
- **Tracked Composables** — search, filter (All / Active / Disposed), sort (most/least
  recomposed, newest, longest lived), and expandable details. Shown only when tracking is on.
- **Analytics** — shown only when analytics are enabled, including a "most recomposed" bar
  chart.

## Configuration

```kotlin
AppDoctor.install(
    application = this,
    config = AppDoctorConfig(
        captureCompose = true,             // install the inspector (default true)
        enableComposableTracking = false,  // opt in to per-composable tracking
        trackedComposableLimit = 200,      // bounded tracked-composable history
        enableComposeAnalytics = false,    // opt in to aggregate analytics
    ),
)
```

All fields are additive and backward compatible.

## Performance

Performance is a first-class requirement — the collector must never become a reason for extra
recompositions:

- **Idle cost ≈ 0.** The runtime flow is `WhileSubscribed`; the `Choreographer` callback and
  `Recomposer.state` collectors run only while the dashboard (or analytics) observes them.
- **No recomposition pressure.** The collector runs entirely on background coroutines and
  reads Compose state through plain `StateFlow.value` / `Long` getters; it never participates
  in composition.
- **No recomposition loops.** `TrackRecompositions` uses one necessary `remember` counter and a
  `SideEffect` that reads no snapshot state, writing only to a non-Compose sink.
- **No retained references.** Nothing holds a `Composition`, `Composer`, `Context`, `Activity`
  or `Window`. Records are immutable data classes of primitives/strings; history is bounded.
- **No reflection** into Compose internals, and **no experimental** Compose APIs.
- Analytics runs on a background dispatcher and coalesces bursts, so it never blocks sampling.

## Limitations

AppDoctor deliberately restricts itself to **stable** Compose APIs. Some runtime facts are only
available through experimental (`@ExperimentalComposeRuntimeApi`) or internal APIs and are
therefore **not** measured, rather than risk breaking across Compose releases:

| Metric | Status |
|---|---|
| Recomposition count / rate | ✅ reliable (`RecomposerInfo.changeCount`) |
| Avg / longest recomposition duration | ≈ approximate (recomposer busy-burst timing) |
| Composition count | proxy = number of active `Recomposer` roots |
| Active composable count | approximate — alive **tracked** composables only |
| Composition disposal count | reliable only for **tracked** composables |
| Frame count / drops | reliable, but only **since first observed** |
| **Skipped recompositions** | ❌ not exposed by stable APIs → reported as `0` |
| **Active animations** | ❌ no stable global registry → reported as `0` |

- Per-composable tracking is opt-in and keyed by the name you pass; distinct instances that
  share a name are aggregated. It measures recomposition *counts*, not per-scope durations.
- This is not a layout/preview/inspection tool: no node tree, bounds, modifiers or state
  editing — runtime metrics only.

## Future compatibility

The collector substrate (stable `MetricCollector` with `snapshot()`, decoupled pure analytics)
is intentionally shaped so future features — Diagnostics, Timeline, Session Reports — can
consume `ComposeRuntimeSnapshot`/`TrackedComposable` without any change here. None of those are
implemented in Phase 4.
