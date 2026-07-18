# AppDoctor — Architecture

This document explains how AppDoctor is put together, why the boundaries are where they
are, and where future features plug in. It complements the high‑level overview in the
[README](../README.md).

---

## 1. Goals that shaped the design

| Goal | Consequence in the code |
|---|---|
| **One‑line install** | A single `object AppDoctor` facade; the UI is discovered by reflection. |
| **Nothing in release** | `install()` gates on `FLAG_DEBUGGABLE`; UI ships via `debugImplementation`. |
| **No leaks** | `WeakReference`/`WeakHashMap` only; `ActivityLifecycleCallbacks`; no static Activity refs. |
| **Minimal CPU** | Monitors are cold flows shared with `WhileSubscribed` — they run only while observed. |
| **Swappable UI / testable** | Core depends on **ports** (`OverlayFactory`), not Compose (Dependency Inversion). |
| **Extensible** | A stable `AppDoctorPlugin` SPI is present from Phase 1. |

---

## 2. Module boundaries

```
┌──────────────────────────────────────────────────────────────────┐
│ sample-app  (com.android.application)                                 │
│   SampleApplication → AppDoctor.install(this)                         │
│   implementation(appdoctor-core) + debugImplementation(ui/network)    │
└───────────────┬───────────────────────────────┬──────────────────────┘
                │ (all variants)                 │ (debug only)
                ▼                                 ▼
┌───────────────────────────────┐   ┌────────────────────────────────────┐
│ appdoctor-core  (library)     │   │ appdoctor-ui / appdoctor-network   │
│  • AppDoctor (facade)         │   │  • Compose overlay + dashboard      │
│  • AppDoctorEngine            │◀──┤  • Network tab plugin + interceptor │
│  • ActivityTracker            │   │  • Material3 plugin tab rendering   │
│  • OverlayCoordinator         │   │                                      │
│  • Monitors (mem/cpu/fps)     │   │                                      │
│  • MetricsProvider            │──▶│  (reads metrics & plugin data)      │
│  • Ports + Plugin SPI         │   │                                      │
│  NO Compose, NO UI            │   │                                      │
└───────────────────────────────┘   └────────────────────────────────────┘
```

- **`appdoctor-core`** compiles with `explicitApi()` and depends only on `kotlinx‑coroutines`
  and `androidx.core`. It never references Compose or any concrete UI.
- **`appdoctor-ui`** `api`‑exposes core (so a consumer gets the public API transitively) and
  contains everything Compose/`WindowManager`.
- The dependency arrow between UI and core only ever points **UI → core**. Core reaches UI
  exclusively through interfaces it owns, resolved at runtime.

---

## 3. The Dependency‑Inversion seam

Core defines two ports:

```kotlin
interface OverlayFactory { fun create(context: Context): AppDoctorOverlay }
interface AppDoctorOverlay { fun attach(a: Activity); fun detach(a: Activity); fun release() }
```

`AppDoctorEngine.resolveOverlay()` obtains an implementation in one of three ways:

1. **Explicit** — `AppDoctorConfig.overlayFactory` (great for tests / custom UIs).
2. **Reflection** — `Class.forName("com.appdoctor.ui.ComposeOverlayFactory")` when the UI
   module is present. This is what makes zero‑config `install()` possible.
3. **Headless** — if neither is available, core still runs the monitors (metrics‑only) and
   simply has no button. No crash.

This is the classic Clean‑Architecture rule: the inner layer (core) declares the interface,
the outer layer (ui) implements it, and wiring happens at the boundary.

---

## 4. Lifecycle & overlay flow

```mermaid
sequenceDiagram
    participant App as Application
    participant F as AppDoctor
    participant E as AppDoctorEngine
    participant T as ActivityTracker
    participant C as OverlayCoordinator
    participant O as FloatingButtonOverlay
    participant D as DashboardActivity

    App->>F: install(app)
    F->>F: isDebuggable? (else return)
    F->>E: new + start()
    E->>App: registerActivityLifecycleCallbacks(T)
    Note over E: startEnabled → enable()

    App-->>T: onActivityResumed(A)
    T->>C: onActivityResumed(A)  (WeakReference)
    C->>O: attach(A)
    O->>A: windowManager.addView(button, TYPE_APPLICATION)

    O->>D: tap → startActivity(Dashboard)
    D->>F: AppDoctor.metrics
    D->>D: collectAsStateWithLifecycle(memory/cpu/fps)

    App-->>T: onActivityPaused(A)
    T->>C: onActivityPaused(A)
    C->>O: detach(A) → removeViewImmediate
```

**Leak safety.** `ActivityTracker` stores the resumed Activity in a `WeakReference` and
clears it on pause. `OverlayCoordinator` and `FloatingButtonOverlay` (via `WeakHashMap`)
hold Activities weakly and always remove their view on pause/destroy. AppDoctor's own UI
Activities (`com.appdoctor.ui.*`) are skipped so the button never appears on the dashboard.

**Threading.** Lifecycle callbacks arrive on the main thread. Overlay/`WindowManager` work
is confined to a `Dispatchers.Main.immediate` scope. Monitor polling runs on
`Dispatchers.Default`; the FPS `Choreographer` callback runs on the main thread.

---

## 5. Monitors — why they cost ~0 while idle

Every monitor implements `Monitor<T>` and exposes a `StateFlow<T>` built like this:

```kotlin
override val data: StateFlow<T> =
    flow { while (true) { emit(read()); delay(interval) } }
        .flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.WhileSubscribed(2_000), initial)
```

`WhileSubscribed` means the upstream (`while` loop / `Choreographer` registration) starts
only when the first collector subscribes and stops shortly after the last one leaves.
Since the **only** collector is the dashboard, closing the dashboard stops all sampling.

| Monitor | Source | Cadence |
|---|---|---|
| `MemoryMonitor` | `Runtime` heap + `Debug.getNativeHeapAllocatedSize()` | 1 s poll |
| `CpuMonitor` | delta of `utime+stime` from `/proc/self/stat` ÷ elapsed ÷ cores | 1 s poll |
| `FpsMonitor` | `Choreographer` frame deltas → current / windowed avg / lowest | per frame |

All collaborators (runtime, stat reader, choreographer, clocks) are constructor‑injected, so
each monitor is unit‑testable without a device (see `CpuMonitorParseTest`, `MemoryInfoTest`).

---

## 6. The UI layer

- **Floating button** is a plain `View` (not Compose) added to the Activity's own
  `WindowManager` with `TYPE_APPLICATION` — **no `SYSTEM_ALERT_WINDOW` permission**. The
  window is `WRAP_CONTENT` with `FLAG_NOT_TOUCH_MODAL | FLAG_NOT_FOCUSABLE`, so touches
  outside the button pass through. A plain View also avoids `ViewTree*Owner` plumbing and
  works over **any** Activity type, not just `ComponentActivity`.
- **Dashboard** is a real (non‑exported) `DashboardActivity` hosting pure Compose. Being an
  Activity gives it a proper lifecycle, back handling and `ViewModel` scope for free, and
  makes the metric flows stop automatically when it is backgrounded.
- **`DashboardViewModel`** receives the `MetricsProvider` through a factory (DI‑friendly) and
  re‑exposes the flows; composables never touch the core singleton directly.
- **Recomposition hygiene:** each section (`MemorySection`, `FpsSection`, `CpuSection`)
  collects its own `StateFlow`, so an update to one metric doesn't recompose the others.

---

## 7. Extension points (future‑proofing)

```kotlin
interface AppDoctorPlugin {
    val id: String
    val title: String
    fun onInstall(context: PluginContext)   // PluginContext: application, metrics, scope
    fun onEnable() {}
    fun onDisable() {}
}
```

Plugins are registered via `AppDoctorConfig.plugins` or `AppDoctor.registerPlugin(...)`.
`AppDoctorEngine` installs them, mirrors enable/disable, and guards every callback so a
faulty plugin can't crash the host. This single seam is how the roadmap items land without
modifying core:

- 🌐 **Network Inspector** — delivered in `appdoctor-network` (OkHttp interceptor + Network tab).
- 🗄️ **Room Inspector** — a plugin that opens the app's databases read‑only for browsing.
- 🧬 **Compose Inspector** — a plugin surfacing recomposition counts.
- 🧩 **Plugin System** — third‑party plugins discovered via the same SPI (and, later,
  `ServiceLoader`/manifest metadata) so they need no core changes at all.

The dashboard is intentionally sectioned so a future `PluginSection` can render each
registered plugin's `title` + content with no structural change.

---

## 8. Package map

```
appdoctor-core/…/com/appdoctor/core/
├── AppDoctor.kt                 facade (public API)
├── AppDoctorConfig.kt           configuration
├── MetricsProvider.kt           read‑only metrics aggregate (port)
├── info/                        DeviceInfo(+Provider), AppInfo(+Provider)
├── monitor/                     Monitor<T>
│   ├── memory/                  MemoryInfo, MemoryMonitor
│   ├── cpu/                     CpuInfo, CpuMonitor
│   └── fps/                     FpsInfo, FpsMonitor
├── overlay/                     OverlayFactory, AppDoctorOverlay (ports)
├── plugin/                      AppDoctorPlugin, PluginContext (SPI)
└── internal/                    AppDoctorEngine, OverlayCoordinator,
                                 lifecycle/ActivityTracker, util/*

appdoctor-ui/…/com/appdoctor/ui/
├── ComposeOverlayFactory.kt     port impl (reflection entry point)
├── overlay/FloatingButtonOverlay.kt
├── dashboard/                   DashboardActivity, DashboardViewModel, DashboardScreen
│   └── components/              SectionCard, InfoRow, MetricBar
│   └── plugin/                  DashboardTabPlugin
├── theme/                       AppDoctorTheme, AppDoctorTokens
└── format/                      Formatters

appdoctor-network/…/com/appdoctor/network/
├── AppDoctorNetworkPlugin.kt    plugin + tab registration surface
├── okhttp/                      AppDoctorNetworkInterceptor
├── repository/                  bounded in-memory request store
├── model/                       immutable network transaction models
└── ui/                          NetworkTabScreen (filters/details/actions)
```
