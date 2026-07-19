# AppDoctor Architecture

AppDoctor follows a modular, dependency-inverted architecture:

- `appdoctor-core` is UI-free and owns lifecycle, collectors, monitors, plugin contracts, and
  overlay/plugin ports.
- UI and optional capability modules (`ui`, `network`, `database`, `compose`, `diagnostics`,
  `timeline`, `session`, `ai`) attach through `ServiceLoader` and stable plugin/collector APIs.
- `appdoctor-extension` exposes third-party extension contracts.

## Runtime model

1. `AppDoctor.install()` initializes core when the app is debuggable.
2. Core resolves optional overlay and plugins.
3. Collectors expose `StateFlow` + snapshot contracts.
4. Dashboard tabs consume read-only flows.

## Design constraints

- Release safety by default (no-op when non-debuggable).
- No static activity retention.
- Bounded in-memory stores.
- Thread-safe background analysis/aggregation.
- Additive module evolution without core rewrites.

For collector internals, see `docs/Collectors.md`.
