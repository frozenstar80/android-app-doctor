# AppDoctor AI (Phase 8)

`appdoctor-ai` is an **optional** module that analyzes AppDoctor runtime behavior using a
strictly bounded input: `SessionReport`.

## Architecture

Core runtime:

- `AppDoctorAiPlugin` (ServiceLoader plugin + AI dashboard tab)
- `AiEngine` (orchestration: sanitization, provider call, cache/history)
- `AiPromptBuilder` (compact prompt generation from `SessionReport`)
- `AiSessionAnalyzer` (response-to-structured-analysis parsing + fallback)
- `AiSummaryGenerator` / `AiRecommendationFormatter` (deterministic structured output)
- `AiCache` (session-id cache)
- `AiHistoryRepository` (bounded analysis history)
- `AiExportFormatter` (markdown/json export payloads)

Provider layer:

- `AiProvider` abstraction
- Built-ins: `OpenAiProvider`, `GeminiProvider`, `LocalModelProvider` (stub),
  `CustomEndpointProvider`

Privacy layer:

- `ReportSanitizer` hook
- Built-ins for key/body/url/device/application redaction
- `CompositeReportSanitizer` pipeline

## Data contract

The module consumes only:

- `SessionReport`

It does **not** consume:

- `CollectorRegistry`
- `MetricCollector`
- `DiagnosticsEngine`
- `TimelineEngine`
- any collector implementation

This keeps AI analysis decoupled from live runtime internals and preserves module boundaries.

## Provider model

`AiProvider` is the extension seam:

```kotlin
interface AiProvider {
    suspend fun analyze(report: SessionReport): AiResponse
}
```

Adding a provider requires one class implementing this interface; `AiEngine` does not need to
change.

Built-in providers are selected via additive `AppDoctorConfig` fields:

- `enableAi`
- `aiProvider`
- `aiApiKey`
- `aiBaseUrl`
- `aiModel`
- `aiTemperature`
- `aiTimeoutMillis`
- `aiCacheEnabled`
- `aiCacheSize`
- `aiLocalOnly`

## Prompt generation

`AiPromptBuilder` produces a compact prompt with:

- health report summary
- issue summary
- timeline summary
- collector summaries
- configuration snapshot
- app metadata
- device metadata

Raw runtime metric streams are never emitted by this module; prompt payload is intentionally
token-minimized.

## Privacy and sanitization

Before any provider call, `AiEngine` sanitizes the report through a pipeline.

`ReportSanitizer` allows custom host-app redaction policies:

```kotlin
interface ReportSanitizer {
    fun sanitize(report: SessionReport): SessionReport
}
```

Built-ins redact/mask common sensitive fields:

- auth headers/tokens/cookies/api keys
- request/response bodies
- URL query strings
- device identifiers
- application identifiers (opt-in sanitizer)
- emails / phone-like values

## Caching and history

- Cache key: `SessionReport.sessionId`
- Cache hit skips provider call unless manual refresh is requested
- History stores bounded generated responses
- Cache/history bounds are controlled by `aiCacheSize`

## Offline mode

If no provider is configured, AI tab shows:

`No AI provider configured.`

No network call is attempted and the rest of AppDoctor continues unchanged.

## Dashboard actions

AI tab supports:

- Generate Analysis
- Refresh
- History
- Copy
- Share
- Export Markdown
- Export JSON

AI analysis runs only on explicit user action in the tab; no automatic upload or background
push is performed.
