# AppDoctor Collectors

This document centralizes the runtime collector modules:

- `appdoctor-network`
- `appdoctor-database`
- `appdoctor-compose`

All collector modules are optional and additive. They register through `ServiceLoader` using
`AppDoctorPluginFactory` and expose metrics through `MetricCollectorProvider` into
`AppDoctor.collectors`.

## Collector guarantees

- Bounded in-memory storage only
- No `Activity` retention
- Deterministic, side-effect-free snapshots
- Optional analytics engines are decoupled from collectors
- Backward-compatible additive configuration in `AppDoctorConfig`

## Network collector (`appdoctor-network`)

- Captures URL/method/headers/bodies (optional), timing, status, and failures.
- Integrates through `AppDoctorNetworkPlugin.createInterceptor()`.
- Uses bounded repository size (`maxRequests`) and bounded body preview
  (`maxCapturedBodyBytes`).

## Database collector (`appdoctor-database`)

- Instruments `SupportSQLite*` via wrapped open-helper factories (`enableAppDoctor()`).
- Captures SQL text/type, duration, rows, success/failure, transaction id, thread/db name.
- Collector and analytics are decoupled:
  - `DatabaseMetricCollector` for live metric history
  - `DatabaseAnalyticsEngine` (optional) for aggregate analysis

## Compose collector (`appdoctor-compose`)

- Uses stable Compose runtime APIs (`Recomposer.runningRecomposers`) and `Choreographer`.
- Captures recomposition totals/rates, frame behavior, and optional tracked composables.
- Collector and analytics are decoupled:
  - `ComposeMetricCollector` for runtime snapshots
  - `ComposeAnalyticsEngine` (optional) for aggregates

## Legacy deep-dive references

For extended design details retained from phase docs:

- `docs/NETWORK.md`
- `docs/DATABASE.md`
- `docs/COMPOSE.md`
