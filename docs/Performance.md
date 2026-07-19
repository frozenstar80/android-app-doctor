# AppDoctor Performance

## Runtime design

- Monitors and collectors are exposed as `StateFlow` and use bounded in-memory storage.
- Sampling and analytics run off the main thread.
- UI observation drives runtime work (`WhileSubscribed`) where supported.

## Practical guidance

- Keep request/response body capture enabled only when needed.
- Tune history caps (`maxRequests`, `maxDatabaseQueries`, `trackedComposableLimit`).
- Enable analytics modules only when actively diagnosing.

## Safety boundaries

- No collector retains Android UI objects.
- Export is explicit user action; no background uploads.
