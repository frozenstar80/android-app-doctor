package com.appdoctor.diagnostics.model

/**
 * Deterministic health summary produced from observed runtime metrics.
 *
 * All scores are in `0..100` where higher is healthier.
 */
public data class HealthReport(
    public val overallScore: Int,
    public val performanceScore: Int,
    public val memoryScore: Int,
    public val networkScore: Int,
    public val databaseScore: Int,
    public val composeScore: Int,
    public val timestampMillis: Long,
) {
    public companion object {
        /** Zero-data baseline shown before the first analysis cycle. */
        public val Empty: HealthReport = HealthReport(
            overallScore = 100,
            performanceScore = 100,
            memoryScore = 100,
            networkScore = 100,
            databaseScore = 100,
            composeScore = 100,
            timestampMillis = 0L,
        )
    }
}
