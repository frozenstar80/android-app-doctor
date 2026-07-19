# AppDoctor Diagnostics

`appdoctor-diagnostics` is an optional deterministic rule engine over existing collector data.

## Components

- `DiagnosticsEngine`: periodic rule execution loop
- `IssueDetector` + `IssueRule`: modular rule evaluation
- `IssueRepository`: bounded issue lifecycle store
- `HealthEngine`: score aggregation
- `RecommendationProvider` + `ConfidenceCalculator`: recommendation/confidence normalization

## Guarantees

- Collector-independent (no collector modifications)
- Deterministic scoring and confidence
- Missing collectors do not produce fabricated findings
- Bounded in-memory state

## Output

- `HealthReport` stream
- `DiagnosticIssue` stream

Use `DiagnosticsReadApi` for read-only access from UI or integrations.
