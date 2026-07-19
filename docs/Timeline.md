# AppDoctor Timeline

`appdoctor-timeline` is an optional observational event layer over collectors and diagnostics.

## Components

- `TimelineEngine`: asynchronous event creation loop
- `TimelineEventFactory`: metric/issue to event mapping
- `TimelineRepository`: bounded thread-safe event store
- `TimelineSearch`: text/metadata filtering
- `TimelineExporter`: JSON/Markdown export

## Event model

Events include timestamp, source, category, collector id, title/summary, severity, metadata,
and optional issue/group linkage.

## Guarantees

- Observer-only (no diagnostics inference)
- Chronological grouping with configurable window
- Bounded in-memory storage
- Additive integration with session reports and UI tabs
