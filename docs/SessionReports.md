# AppDoctor Session Reports (Phase 7)

`appdoctor-session` is an **optional**, local-only aggregation module that builds a complete
debug report for the current app process session.

It never generates diagnostics, never modifies collectors, and never modifies timeline data.

## Architecture

- `AppDoctorSessionPlugin` (ServiceLoader module entry point)
- `SessionRecorder` (asynchronous collector snapshot sampler)
- `SessionBuilder` (pure aggregation assembler)
- `SessionMetadataProvider` (session/app/device metadata capture)
- `SessionFormatter` (JSON/Markdown formatting)
- `SessionExporter` (JSON/Markdown/ZIP export)
- `SessionRepository` (bounded in-memory optional storage)
- `SessionManager` (public helper APIs: `generate`, `save`, `share`, `export`)

## Aggregation model

Session Reports consume existing outputs only:

- `CollectorRegistry` snapshots + sampled history (for collector summaries)
- optional diagnostics plugin outputs (`healthReport`, `issues`)
- optional timeline plugin outputs (`events`)
- optional analytics from compose/database plugins
- core app/device/build/config snapshots

By default reports are summary-first: they keep aggregate counts/latencies and avoid embedding
large request/response or SQL payloads directly into the report.

No collector contracts are changed. No module receives a back-reference to session reports.

## Missing-module behavior

All external module reads are best-effort:

- If diagnostics is absent, `diagnostics` / `healthReport` sections are omitted.
- If timeline is absent, `timeline` section is omitted.
- If compose/database analytics are disabled or absent, analytics summaries are omitted.

Report generation succeeds with partial data.

## Export formats

Supported formats:

- `JSON` (`session-<id>.json`)
- `MARKDOWN` (`session-<id>.md`)
- `ZIP` (`session-<id>.zip`)

ZIP contents:

- `report.json`
- `report.md`
- `timeline.json`
- `health.json`
- `diagnostics.json`
- `metadata.json`

`SessionExporter` is format-extensible via additive enum/branch additions.

## Performance

- Sampling and report generation run off the main thread.
- Collector reads use point-in-time `snapshot()` only.
- No collector execution path is blocked by report generation.
- ZIP writing uses buffered streaming via `ZipOutputStream`.

## Configuration

`AppDoctorConfig` additive flags:

- `enableSessionReports` (default: `false`)
- `maximumStoredReports` (default: `10`)
- `includeFullSessionPayloads` (default: `false`)
- `autoGenerateOnCrash` (placeholder, default: `false`)

## Limitations

- Local-only storage/export (no upload/sync/cloud APIs).
- `share()` returns local file + MIME metadata; host app controls final share intent/transport.
- `autoGenerateOnCrash` is declared but intentionally not implemented yet.
