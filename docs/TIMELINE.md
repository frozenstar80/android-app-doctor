# AppDoctor Timeline (Phase 6)

`appdoctor-timeline` is an optional observational module that records a chronological
runtime timeline across collectors and diagnostics.

It does **not** diagnose, score, or recommend. It only captures, correlates, groups and exports events.

---

## 1. Architecture

Core components:

- **`TimelineEngine`**: asynchronous polling loop that consumes `CollectorRegistry` snapshots and
  diagnostics issue updates (when diagnostics is present).
- **`TimelineEventFactory`**: maps collector snapshots / diagnostics objects into typed timeline events.
- **`TimelineRepository`**: thread-safe bounded in-memory timeline store.
- **`TimelineFormatter`**: deterministic event title/summary formatting.
- **`TimelineExporter`**: ordered JSON/Markdown exports with metadata preserved.
- **`TimelineSearch`**: query matching over titles, summaries, collector IDs, categories and metadata.
- **`TimelineSession`**: session identity and lifecycle summary.
- **`TimelineFilter`**: collector/category/severity/time-range filter contract.

All timeline logic is collector-agnostic and observer-only.

---

## 2. Event model

Every `RuntimeTimelineEvent` includes:

- `timestamp`
- `sessionId`
- `source`
- `collectorId`
- `category`
- `title`
- `summary`
- `severity` (optional)
- `relatedIssueId` (optional)
- `metadata`
- `sourceMetric`
- `groupId` (optional correlation group)

The base `TimelineEvent` contract is:

```kotlin
interface TimelineEvent {
    val timestamp: Long
    val sessionId: String
    val source: String
    val metadata: Map<String, Any>
}
```

---

## 3. Grouping strategy

Timeline groups temporally adjacent events using `timelineGroupingWindowMillis`.

- Events are sorted by timestamp.
- If two consecutive events are within the grouping window, they share a `groupId`.
- If the gap exceeds the window, a new group starts.

This is purely chronological correlation and never diagnostic inference.

---

## 4. Storage and performance

`TimelineRepository` is:

- thread-safe (`synchronized` mutation),
- bounded (`maximumTimelineEvents`),
- hot-flow based (`StateFlow<List<RuntimeTimelineEvent>>`) for live dashboard streaming.

This keeps memory bounded and avoids blocking collectors.

---

## 5. Configuration

Additive `AppDoctorConfig` fields:

- `enableTimeline: Boolean = false`
- `maximumTimelineEvents: Int = 1000`
- `timelineGroupingWindowMillis: Long = 2000L`

If the module is absent or disabled, AppDoctor behavior remains unchanged.

---

## 6. Export formats

`TimelineExporter` supports:

- **JSON**: ordered array of events, each including metadata.
- **Markdown**: timeline table + per-event metadata section.

Both preserve event ordering and event metadata.

---

## 7. Dashboard

A top-level **Timeline** tab shows:

- live event stream,
- text search,
- collector filters,
- metadata expansion,
- related issue jump action (navigates to Health tab when `relatedIssueId` is present),
- color-coded event types/severities.

---

## 8. Limitations

- Timeline is in-memory and session-scoped.
- No diagnostics/recommendations are generated here.
- Grouping is temporal, not causal.
