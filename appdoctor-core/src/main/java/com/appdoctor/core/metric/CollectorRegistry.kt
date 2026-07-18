package com.appdoctor.core.metric

/**
 * Read-only view of every registered [MetricCollector] (core monitors plus any contributed
 * by plugins).
 *
 * Obtained via [com.appdoctor.core.AppDoctor.collectors]. Registration is performed
 * internally by AppDoctor as monitors and plugins come online; consumers only read.
 * Future Diagnostics / Timeline / Session Reports enumerate here; the dashboard may too.
 */
public interface CollectorRegistry {

    /** Snapshot of currently registered collectors, in registration order. */
    public val collectors: List<MetricCollector<Metric>>

    /** Looks up a collector by its stable [MetricCollector.id], or `null` if absent. */
    public fun collector(id: String): MetricCollector<Metric>?
}
