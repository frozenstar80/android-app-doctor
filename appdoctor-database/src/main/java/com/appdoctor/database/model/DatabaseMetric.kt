package com.appdoctor.database.model

import com.appdoctor.core.metric.Metric

/**
 * Aggregate [Metric] snapshot of captured SQL queries. Database is a stream-of-many, so
 * like the network module it wraps its payload (latest-first records).
 */
public data class DatabaseMetric(
    public val queries: List<DatabaseQuery>,
) : Metric
