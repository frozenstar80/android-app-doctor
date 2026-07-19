# AppDoctor Diagnostics (Phase 5)

`appdoctor-diagnostics` is an **optional** intelligence module that consumes runtime metrics and
derives deterministic health insights.

- No collector is modified.
- Collectors do not depend on diagnostics.
- Diagnostics depends only on `appdoctor-core` collector contracts (`CollectorRegistry`,
  `MetricCollector`, `Metric`).

---

## 1. Architecture

### Runtime components

- **`DiagnosticsEngine`**: asynchronous analysis loop; reads collector snapshots at
  `analysisInterval`.
- **`IssueDetector`**: applies modular rules and builds issues.
- **`IssueRule`**: per-rule contract; each rule is independent.
- **`IssueRepository`**: open/update/resolve/ignore lifecycle + bounded issue history.
- **`HealthEngine`**: deterministic score calculation from observed metrics + open issues.
- **`RecommendationProvider`**: deterministic recommendation catalog per rule id.
- **`ConfidenceCalculator`**: converts evidence strength/consistency to a confidence percentage.

### Data models

- **`HealthReport`**: `overall`, `performance`, `memory`, `network`, `database`, `compose` scores.
- **`DiagnosticIssue`**: id, title, description, category, severity, confidence, timestamp,
  collector IDs, recommendation, optional docs link, status (`OPEN`, `RESOLVED`, `IGNORED`).

---

## 2. Rule engine independence

The rule engine is pure Kotlin and Android-free:

- Rules read `RuleContext` value objects, not collectors directly.
- Rule classes are deterministic and stateless.
- Rules only use metrics that are actually present and measurable.
- Missing collectors/metrics never fabricate conclusions; the corresponding rules simply do not fire.

This keeps rule tests fast and isolated from platform/runtime concerns.

---

## 3. Adding new rules

Add a new rule by:

1. Implementing `IssueRule`.
2. Returning a `RuleMatch` when deterministic threshold conditions are met.
3. Registering it in `AppDoctorDiagnosticsPlugin` rule list.
4. Adding/adjusting deterministic recommendation entry in `RecommendationProvider`.
5. Adding unit tests for trigger and non-trigger paths.

No existing rule class needs modification.

---

## 4. Health score calculation

Scores are strictly deterministic and metric-derived (0–100):

- **Memory score**: based on sustained usage, short-window growth, and open memory/cross issues.
- **Network score**: based on latency, failure ratio, and open network-related issues.
- **Database score**: based on query duration, failure ratio, and open database issues.
- **Compose score**: based on recomposition rate, frame drop rate, and open compose issues.
- **Performance score**: based on FPS plus cross/performance issue penalties.
- **Overall score**: arithmetic mean of the five component scores.

No random scoring or AI inference is used.

---

## 5. Confidence model

Confidence is a deterministic function of:

- supporting evidence points,
- total evidence points,
- consistency (0..1).

Current formula (clamped `0..100`):

`35 + supportRatio*40 + consistency*20 + min(supportingPoints, 20)`

`minimumConfidence` is applied before publishing issues, so weak evidence is not shown as fact.

---

## 6. Issue lifecycle

- **Open**: when a rule condition is currently met.
- **Update**: when the same issue id is observed again with newer evidence.
- **Resolve**: automatically when the rule condition no longer holds.
- **Ignore**: user-dismissed for current session (`dismissIssue`); auto-clears when condition disappears.

`IssueRepository` deduplicates by stable issue id and stores bounded history
(`maximumIssueHistory`).

---

## 7. Current deterministic rule set

Implemented initial rules:

- Memory:
  - sustained high usage
  - rapid growth
- Network:
  - high average latency
  - repeated failures
  - excessive request volume
- Database:
  - slow queries
  - high query frequency
  - high failure rate
- Compose:
  - high recomposition rate
- Cross-collector:
  - FPS drops with slow DB queries
  - FPS drops with slow network requests
  - high memory with repeated DB activity

Rules fire only when corresponding metrics are available.

---

## 8. Dashboard integration

The dashboard adds a top-level **Health** tab that shows:

- overall health,
- component scores,
- severity counts,
- latest recommendations,
- issue details with filter/sort/expand/dismiss (session-only).

Diagnostics remains optional: if module is absent or disabled, the tab shows an inactive message.

---

## 9. Limitations

- Diagnostics does not infer unmeasured behavior.
- Rules based on unavailable metrics (e.g., GC frequency in current collectors) are intentionally not emitted.
- Confidence reflects evidence consistency, not ground-truth certainty.
- History is in-memory and session-scoped.
