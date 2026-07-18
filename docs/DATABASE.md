# 🗄️ AppDoctor Database Inspector

`appdoctor-database` is Phase 3 of AppDoctor: a **runtime** database metrics collector with
an optional analytics engine and a dedicated dashboard tab.

It is **not** a database browser and **not** a replacement for the Android Studio Database
Inspector. It does not read tables, schemas or rows — it observes queries as they execute.

## What it captures

For every executed statement:

- SQL text and query type (`SELECT` / `INSERT` / `UPDATE` / `DELETE` / `OTHER`)
- execution duration and timestamp
- success / failure + exception message
- thread name and database name
- rows affected (writes) and rows returned (reads), when available
- transaction id (when the statement runs inside a transaction)

## Architecture

```
Room / SupportSQLite
   └─ openHelperFactory(AppDoctorSQLiteOpenHelperFactory)   ← via enableAppDoctor()
        └─ SupportSQLiteDatabase (java.lang.reflect.Proxy)  ← faithful delegation
             • query()            → MeasuredCursor           (times + counts a SELECT)
             • compileStatement() → AppDoctorSupportSQLiteStatement (times writes)
             • insert/update/delete/execSQL/begin*/end
                  └─ DatabaseQueryRecorder → InMemoryDatabaseQueryRepository (bounded, StateFlow)
                        ├─ DatabaseMetricCollector  → CollectorRegistry (id "database")
                        └─ DatabaseAnalyticsEngine (optional) → StateFlow<DatabaseAnalytics>
```

The module self-registers through the existing `ServiceLoader` plugin SPI
(`AppDoctorDatabasePluginFactory` under
`META-INF/services/com.appdoctor.core.plugin.AppDoctorPluginFactory`); no `appdoctor-core`
change is needed to add it. The plugin exposes its collector via `MetricCollectorProvider`,
so it lands in `AppDoctor.collectors` automatically (stable id `"database"`).

## Collector design

`DatabaseMetricCollector` implements the existing `MetricCollector` contract, mapping the
repository's hot `StateFlow<List<DatabaseQuery>>` into `DatabaseMetric` with
`stateIn(WhileSubscribed)` (idle-cost-zero), and overriding `snapshot()` for a fresh
point-in-time read. **The collector performs no analytics** — it only exposes history.

## Analytics architecture (optional)

Analytics is a fully independent consumer, off by default:

- `DatabaseAnalyticsComputer` — a **pure**, stateless function turning a query list into
  `DatabaseAnalytics`. New metrics are additive here and never touch the collector.
- `DatabaseAnalyticsEngine` — recomputes reactively on `Dispatchers.Default` with
  `mapLatest` (which cancels superseded computations), so aggregation never blocks query
  collection. Created only when `enableDatabaseAnalytics = true`.

Analytics includes: total queries, average/min/max duration, slow-query count, failed-query
count, success rate, count by type, read/write split, transactions executed, most queried
tables, most frequent statements, and longest-running queries.

## Integration

Add the module (debug-only recommended) and enable it on your Room builder:

```kotlin
// build.gradle.kts
debugImplementation(project(":appdoctor-database"))
```

```kotlin
import com.appdoctor.database.enableAppDoctor

val db = Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
    .enableAppDoctor()   // wraps the SQLite open-helper factory
    .build()
```

Already using a custom `SupportSQLiteOpenHelper.Factory`? Pass it through:
`.enableAppDoctor(myFactory)`. For raw SupportSQLite / SQLDelight setups, wrap the factory
directly: `AppDoctorDatabase.wrapOpenHelperFactory(myFactory)`.

`enableAppDoctor()` is safe to leave in place — it is a no-op unless the database inspector
is installed and enabled (e.g. release builds, or `captureDatabase = false`).

## Dashboard tab

- **Live Queries** — newest-first list with type badge, collapsed SQL, duration and rows.
- **Search** (SQL) + **Filters** (type: SELECT/INSERT/UPDATE/DELETE, and Success/Failure) +
  **Sort** (Newest / Oldest / Duration).
- **Query Details** — pretty-printed, expand/collapse SQL, duration, thread, database, rows
  affected/returned, timestamp, transaction id, exception, with **Copy** and **Export**.
- **Analytics** — shown only when analytics are enabled.

## Configuration

```kotlin
AppDoctor.install(
    application = this,
    config = AppDoctorConfig(
        captureDatabase = true,           // install the inspector (default true)
        maxDatabaseQueries = 100,         // bounded history size
        slowQueryThresholdMillis = 16L,   // "slow" threshold (≈ one frame)
        enableDatabaseAnalytics = false,  // opt in to runtime analytics
    ),
)
```

All fields are additive and backward compatible.

## How Room is instrumented

Room is built on the `androidx.sqlite` `SupportSQLite*` abstraction. AppDoctor wraps it at
`RoomDatabase.Builder.openHelperFactory(...)` — the officially supported seam:

- **Reads** (`query()`) return a `MeasuredCursor` (a `CursorWrapper`) that times from cursor
  acquisition to `close()` and reports rows returned from the caller's own iteration.
- **Writes** (`compileStatement().executeInsert/executeUpdateDelete/execute`) are timed
  synchronously; `executeUpdateDelete()` gives exact rows affected, `executeInsert()` the
  row id.
- **Transactions** (`beginTransaction*`/`endTransaction`) feed a per-thread
  `TransactionTracker` that tags every statement with a transaction id.

The `SupportSQLiteDatabase` wrapper is a `java.lang.reflect.Proxy` that faithfully delegates
all interface methods and intercepts only the hot paths. This keeps the wrapper resilient
across `androidx.sqlite` versions and engine-agnostic.

## Extending to SQLite / SQLDelight

Everything below `DatabaseQueryRecorder` speaks `DatabaseQuery` and is database-agnostic.
The instrumentation targets `androidx.sqlite`, not Room, so future engines reuse it:

- **Raw SupportSQLite**: `AppDoctorDatabase.wrapOpenHelperFactory(yourFactory)`.
- **SQLDelight**: wrap the `SupportSQLiteOpenHelper.Factory` you pass to
  `AndroidSqliteDriver` the same way.

No core, collector, analytics or UI changes are required — only a thin new entry point.

## Performance considerations

- Wrappers short-circuit to pure delegation when capture is disabled (one volatile read).
- SELECT timing/rows come from the caller's own cursor iteration — nothing is materialised
  eagerly, and **no `Cursor` is ever retained**.
- History is bounded and in-memory; records are immutable data classes holding only
  primitives (no `Cursor`, `Room`, `Activity` or `Context` references).
- Analytics runs on a background dispatcher and coalesces bursts, so it never blocks query
  collection.

## Limitations

- Only databases wired with `enableAppDoctor()` (or a wrapped factory) are observed.
- Bind-argument values are not captured (only the SQL text).
- Rows returned reflect the caller's iteration of the cursor.
- SQL parsing for query type / table names is best-effort (regex-based).
- No table/schema browsing, editing or data modification — runtime metrics only.
