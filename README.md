# 🩺 AppDoctor

**A zero‑config, debug‑only diagnostics overlay for Android.**
One line in your `Application` gives you a draggable floating button on every screen that opens a live dashboard of **device info, app info, memory, FPS, CPU and network requests** — and it compiles to **nothing** in release builds.

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDoctor.install(this)   // that's it
    }
}
```

> Phase 2 in progress. Built with Kotlin, Jetpack Compose and Clean Architecture.

---

## ✨ Features

| | Feature | Notes |
|---|---|---|
| 🎈 | **Floating debug button** | Appears automatically on **every** Activity. Draggable, edge‑aware, and **never intercepts touches outside itself**. |
| 📊 | **Compose dashboard** | Opens on tap. Fully Jetpack Compose, self‑themed (looks identical in every host app). |
| 📱 | **Device Info** | Android version, API level, manufacturer, model, brand, ABI. |
| 📦 | **App Info** | Version name, version code, build type, package, min/target SDK. |
| 🧠 | **Memory monitor** | Used / max heap / free / native, heap %, refreshed every second. |
| 🎞️ | **FPS monitor** | Current, average and lowest FPS, updated live via `Choreographer`. |
| ⚙️ | **CPU monitor** | Approximate process CPU %, sampled from `/proc/self/stat` every second. |
| 🌐 | **Network Inspector** | OkHttp interceptor + dashboard tab with search/filter/sort, headers/body/timing details, copy/share/export actions. |
| 🔌 | **Programmatic control** | `enable()`, `disable()`, `isEnabled()`. |
| 🚫 | **Release‑safe** | Complete **no‑op** in non‑debuggable builds — no lifecycle callbacks, monitors, or overlay are ever created. |

---

## 🏗️ Architecture

AppDoctor follows **Clean Architecture** with strict separation between a UI‑agnostic core and a swappable Compose UI, wired together through the **Dependency Inversion Principle**.

```mermaid
flowchart TD
    subgraph host["Host app"]
        APP["Application.onCreate()"]
    end

    subgraph core["appdoctor-core  (no Compose)"]
        FACADE["AppDoctor (facade)"]
        ENGINE["AppDoctorEngine"]
        TRACKER["ActivityTracker\n(ActivityLifecycleCallbacks)"]
        COORD["OverlayCoordinator"]
        subgraph monitors["Monitors (cold → StateFlow, WhileSubscribed)"]
            MEM["MemoryMonitor"]
            CPU["CpuMonitor"]
            FPS["FpsMonitor"]
        end
        METRICS["MetricsProvider"]
        PORTS["Ports:\nOverlayFactory / AppDoctorOverlay\nAppDoctorPlugin"]
    end

    subgraph ui["appdoctor-ui  (Compose, debug‑only)"]
        FACTORY["ComposeOverlayFactory"]
        BUTTON["FloatingButtonOverlay\n(WindowManager, draggable)"]
        DASH["DashboardActivity\n+ DashboardViewModel\n+ DashboardScreen"]
    end

    APP -->|install| FACADE --> ENGINE
    ENGINE --> TRACKER --> COORD
    ENGINE --> monitors --> METRICS
    ENGINE -.reflection.-> FACTORY -->|creates| BUTTON
    COORD -->|attach/detach| BUTTON
    BUTTON -->|tap| DASH
    DASH -->|reads| METRICS
    PORTS -. implemented by .- FACTORY
```

**Key design points**

- **No static Activity references.** The current Activity is tracked with a `WeakReference` via `ActivityLifecycleCallbacks`; the overlay coordinator holds only weak references and detaches on `onPause`.
- **DIP seam.** `appdoctor-core` knows nothing about Compose. It talks to an `OverlayFactory` port. The `appdoctor-ui` module provides `ComposeOverlayFactory`, discovered **reflectively** so `install()` needs zero configuration — or inject your own via `AppDoctorConfig.overlayFactory`.
- **Lazy monitors = minimal CPU.** Each monitor is a cold flow shared with `stateIn(scope, WhileSubscribed(), …)`. Polling and the `Choreographer` callback only run **while the dashboard is open**. An idle app pays nothing.
- **Thread‑safe & lifecycle‑aware.** The public facade is safe to call from any thread; the dashboard uses `collectAsStateWithLifecycle` so metrics stop when it leaves the foreground.

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for a deeper dive and the full extension‑point design.

### Modules

| Module | Type | Responsibility | Compose? |
|---|---|---|---|
| **`appdoctor-core`** | Android library | Public API, engine, lifecycle, monitors, ports, plugin SPI. | ❌ |
| **`appdoctor-ui`** | Android library | Floating button overlay + Compose dashboard. Implements the core ports. | ✅ |
| **`appdoctor-network`** | Android library | OkHttp interception, bounded request store, and Network dashboard tab plugin. | ✅ |
| **`sample-app`** | Android app | Demonstrates the one‑line integration. | ✅ |

---

## 📥 Installation

AppDoctor is a multi‑module project. Integrate it so the **tiny, self‑gating core** ships in all variants while the **heavy Compose UI is debug‑only**:

```kotlin
dependencies {
    implementation(project(":appdoctor-core"))     // small; no‑op in release
    debugImplementation(project(":appdoctor-ui"))  // Compose overlay, debug builds only
    debugImplementation(project(":appdoctor-network")) // Network inspector plugin + UI tab
}
```

> **Why this split?** `AppDoctor.install()` lives in `core`, so it compiles in every variant, but in release `core` detects the app is not debuggable and returns immediately. The overlay code in `ui` isn't even present in release. You get the best of both worlds: a clean call site and zero release footprint.

**Requirements**

- `minSdk` **24+**
- AGP **9.0+** (uses built‑in Kotlin), Gradle **9.x**, JDK **17+**
- Jetpack Compose (host app themes are not required — the dashboard is self‑themed)

---

## 🚀 Quick start

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDoctor.install(this)
    }
}
```

Run a **debug** build and you'll see the floating 🩺 button. Drag it anywhere; tap it for the dashboard.

### Network interception (OkHttp)

`appdoctor-network` auto-registers `AppDoctorNetworkPlugin` when present on the classpath (unless `captureNetwork = false`).  
Add its interceptor to your OkHttp client:

```kotlin
val networkPlugin = AppDoctorNetworkPlugin.installed()
val client = OkHttpClient.Builder().apply {
    networkPlugin?.let { addInterceptor(it.createInterceptor()) }
}.build()
```

### Configuration (optional)

```kotlin
AppDoctor.install(
    application = this,
    config = AppDoctorConfig(
        startEnabled = true,             // start hidden with false, then call enable()
        pollingIntervalMillis = 1_000L,  // memory & CPU sample interval
        captureNetwork = true,
        captureRequestBody = true,
        captureResponseBody = true,
        maxCapturedBodyBytes = 262_144,
        maxRequests = 100,
    ),
)
```

---

## 🧩 Public API

| Member | Description |
|---|---|
| `AppDoctor.install(application, config = AppDoctorConfig())` | Installs AppDoctor. Idempotent. **No‑op in release** unless `config.enabledInReleaseBuilds = true`. |
| `AppDoctor.enable()` | Shows the overlay and starts monitoring. |
| `AppDoctor.disable()` | Hides the overlay and stops monitoring (install remains). |
| `AppDoctor.isEnabled(): Boolean` | Whether the overlay is currently active. |
| `AppDoctor.isInstalled(): Boolean` | Whether `install()` took effect in this build. |
| `AppDoctor.registerPlugin(plugin)` | Registers an `AppDoctorPlugin` at runtime (extension point). |
| `AppDoctor.plugins: List<AppDoctorPlugin>` | Snapshot of registered plugins. |
| `AppDoctor.plugin(id: String): AppDoctorPlugin?` | Resolve a plugin by ID. |
| `AppDoctor.metrics: MetricsProvider?` | Live metrics for the UI / plugins; `null` if inactive. |

Every public symbol carries **KDoc**, and the module is compiled with **`explicitApi()`** for a clean, intentional ABI.

---

## 🛡️ Release safety, in detail

AppDoctor decides whether to activate by reading the host app's `ApplicationInfo.FLAG_DEBUGGABLE` — no build‑variant wiring required from you:

1. `install()` checks `isDebuggable`. In a release (non‑debuggable) build it logs one line and returns. **Nothing is constructed.**
2. Because the Compose UI is added with `debugImplementation`, the overlay classes aren't even in your release APK.
3. If you *deliberately* want AppDoctor in a non‑debuggable flavor (e.g. internal QA), set `AppDoctorConfig(enabledInReleaseBuilds = true)`.

This repo verifies the guarantee: `:sample-app:assembleRelease` compiles and packages **without** `appdoctor-ui` on the classpath.

---

## ⚡ Performance

- **Idle cost ≈ 0.** Monitors are `WhileSubscribed` flows; nothing polls until the dashboard is open.
- **Overlay is a plain `View`,** not Compose — negligible memory, no recomposition, works over any Activity type without `ViewTree*Owner` plumbing.
- **Recomposition‑friendly dashboard.** Each metric section collects its own `StateFlow`, so a memory tick doesn't recompose the FPS section.
- **No permissions.** The button lives in the app's own window (`TYPE_APPLICATION`), so **no `SYSTEM_ALERT_WINDOW`** is needed, and `FLAG_NOT_TOUCH_MODAL` lets touches outside the button pass straight through.

---

## 🔮 Extending AppDoctor (future‑proofing)

Phase 1 ships a stable plugin SPI so later capabilities slot in **without touching core**:

```kotlin
class NetworkInspectorPlugin : AppDoctorPlugin {
    override val id = "network-inspector"
    override val title = "Network"
    override fun onInstall(context: PluginContext) { /* hook OkHttp, expose flows */ }
    override fun onEnable() { /* start capturing */ }
    override fun onDisable() { /* pause */ }
}

AppDoctor.registerPlugin(NetworkInspectorPlugin())
```

Planned extension points on the roadmap:

- 🗄️ **Room Inspector** — inspect and query the local database.
- 🧬 **Compose Inspector** — recomposition counts and layout insights.
- 🧩 **Plugin System** — third‑party tabs discovered via the same SPI.

---

## ▶️ Sample app

The [`sample-app`](sample-app) module demonstrates the full integration:

- One‑line `AppDoctor.install(this)` in [`SampleApplication`](sample-app/src/main/java/com/example/appdoctor/SampleApplication.kt).
- A button to **toggle** AppDoctor at runtime (`enable`/`disable`).
- A **second Activity** proving the overlay follows the foreground screen.
- A **"heavy work"** button so you can watch the CPU/FPS meters react.

```bash
./gradlew :sample-app:installDebug   # build & install on a device/emulator
```

---

## 🔨 Building from source

```bash
./gradlew :sample-app:assembleDebug      # debug APK (with overlay)
./gradlew :sample-app:assembleRelease    # release APK (AppDoctor stripped/no‑op)
./gradlew test                           # unit tests
./gradlew lint                           # Android lint
```

**Toolchain used:** AGP 9.2.1 · Gradle 9.4.1 · JDK 21 toolchain · Kotlin 2.2.10 (AGP built‑in) · Compose BOM 2026.06.01 · `minSdk` 24 / `compileSdk` 37.

---

## 🗺️ Roadmap

- [x] **Phase 1 — MVP:** floating button, dashboard, device/app/memory/FPS/CPU, plugin SPI.
- [~] **Phase 2:** Network Inspector ✅, Room Inspector ⏳.
- [ ] **Phase 3:** Compose Inspector, crash/ANR capture, log viewer.
- [ ] **Phase 4:** public plugin system + Maven Central publishing.

---

## 📄 License

Released under the **MIT License**. See [`LICENSE`](LICENSE).
