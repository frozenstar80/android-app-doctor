# 🌐 AppDoctor Network Inspector

`appdoctor-network` is Phase 2 of AppDoctor: an OkHttp-powered in-app network inspector with a dedicated dashboard tab.

## What it captures

For each intercepted call:

- URL, method, query parameters
- request headers + body (optional)
- response status, headers + body (optional)
- response time, content length, timestamp
- success/failure + failure message

## How interception works

1. Add `debugImplementation(project(":appdoctor-network"))`.
2. AppDoctor auto-loads `AppDoctorNetworkPlugin` when `captureNetwork = true`.
3. Get the installed plugin and add its interceptor to each OkHttp client:

```kotlin
val plugin = AppDoctorNetworkPlugin.installed()
val client = OkHttpClient.Builder().apply {
    plugin?.let { addInterceptor(it.createInterceptor()) }
}.build()
```

Retrofit is covered automatically because it uses OkHttp underneath.

## Storage, limits, and safety

- In-memory bounded repository (thread-safe).
- Keeps only the latest `maxRequests` entries (default `100`).
- Models are immutable data classes.
- No `Activity` references.
- Captured body size is capped (`maxCapturedBodyBytes`, default `262_144` bytes).
- Large payloads are truncated; responses are read with `peekBody(...)` to avoid full copies.

## Body rendering

- JSON: pretty-printed
- XML: pretty-printed
- plain text: shown directly
- binary: displayed as `Binary Data`

## Dashboard tab features

- search (URL/method)
- filters (GET/POST/PUT/DELETE + status code)
- sort (Newest / Oldest / Longest Duration)
- timeline list
- request details with expandable sections:
  - Request
  - Response
  - Headers
  - Body
  - Timing
- actions: Copy, Share, Export

## Configuration

```kotlin
AppDoctor.install(
    application = this,
    config = AppDoctorConfig(
        captureNetwork = true,
        captureRequestBody = true,
        captureResponseBody = true,
        maxCapturedBodyBytes = 262_144,
        maxRequests = 100,
    ),
)
```

## Limitations

- Only calls that pass through an OkHttp client with the AppDoctor interceptor are captured.
- Duplex and one-shot request bodies are not captured.
- Body previews are intentionally bounded for performance.

## Performance impact

- Low per-request overhead (metadata capture + bounded body preview).
- Body capture can be disabled independently for request/response.
- Inspector state is in-memory only; nothing is persisted to disk unless user taps **Export**.
