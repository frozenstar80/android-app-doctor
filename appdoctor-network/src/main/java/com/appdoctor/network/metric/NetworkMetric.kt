package com.appdoctor.network.metric

import com.appdoctor.core.metric.Metric
import com.appdoctor.network.model.NetworkRequestRecord

/**
 * Aggregate [Metric] snapshot of captured network traffic. Network is a stream-of-many, so
 * unlike the single-value core metrics it wraps its payload (latest-first records).
 */
public data class NetworkMetric(
    public val requests: List<NetworkRequestRecord>,
) : Metric
