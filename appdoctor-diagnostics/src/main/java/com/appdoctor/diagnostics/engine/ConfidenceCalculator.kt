package com.appdoctor.diagnostics.engine

import kotlin.math.roundToInt

/**
 * Converts rule evidence to an integer confidence percentage (`0..100`).
 */
public class ConfidenceCalculator {

    public fun calculate(
        supportingPoints: Int,
        totalPoints: Int,
        consistency: Double,
    ): Int {
        if (supportingPoints <= 0 || totalPoints <= 0) return 0
        val supportRatio = supportingPoints.toDouble() / totalPoints.toDouble()
        val boundedConsistency = consistency.coerceIn(0.0, 1.0)

        val raw = 35.0 +
            (supportRatio * 40.0) +
            (boundedConsistency * 20.0) +
            supportingPoints.coerceAtMost(20)

        return raw.roundToInt().coerceIn(0, 100)
    }
}
