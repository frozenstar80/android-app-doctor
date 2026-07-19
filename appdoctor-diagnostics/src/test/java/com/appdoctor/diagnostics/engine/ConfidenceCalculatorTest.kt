package com.appdoctor.diagnostics.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfidenceCalculatorTest {

    private val calculator = ConfidenceCalculator()

    @Test
    fun `returns zero when evidence missing`() {
        assertEquals(0, calculator.calculate(supportingPoints = 0, totalPoints = 10, consistency = 0.8))
        assertEquals(0, calculator.calculate(supportingPoints = 2, totalPoints = 0, consistency = 0.8))
    }

    @Test
    fun `increases with more support and consistency`() {
        val low = calculator.calculate(supportingPoints = 3, totalPoints = 10, consistency = 0.2)
        val medium = calculator.calculate(supportingPoints = 6, totalPoints = 10, consistency = 0.6)
        val high = calculator.calculate(supportingPoints = 9, totalPoints = 10, consistency = 0.9)
        assertTrue(low in 0..100)
        assertTrue(high in 0..100)
        assertTrue(low < medium)
        assertTrue(medium < high)
    }
}
