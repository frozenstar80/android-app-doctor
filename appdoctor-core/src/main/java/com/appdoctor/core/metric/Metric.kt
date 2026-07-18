package com.appdoctor.core.metric

/**
 * Marker for a single, immutable unit of diagnostic data produced by a [MetricCollector].
 *
 * Kept intentionally memberless: a metric is a plain value (heap usage, CPU %, a set of
 * captured requests, …). Identity, liveness and history belong to the [MetricCollector]
 * that produces it, not to the value itself. This lets future consumers (Diagnostics,
 * Timeline, Session Reports) enumerate heterogeneous metrics uniformly and branch on the
 * concrete subtype (`when (metric) { is MemoryInfo -> … }`).
 *
 * Existing Phase 1/2 models implement this directly (e.g.
 * [com.appdoctor.core.monitor.memory.MemoryInfo]); aggregate metrics (e.g. the network
 * module's `NetworkMetric`) wrap their payload.
 */
public interface Metric
