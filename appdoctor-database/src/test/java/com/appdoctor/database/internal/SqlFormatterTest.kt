package com.appdoctor.database.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SqlFormatterTest {

    @Test
    fun `normalize collapses whitespace`() {
        assertEquals("SELECT a FROM t", SqlFormatter.normalize("SELECT   a\n  FROM\tt"))
    }

    @Test
    fun `collapse truncates long sql with ellipsis`() {
        val collapsed = SqlFormatter.collapse("SELECT " + "x".repeat(200), max = 20)
        assertEquals(20, collapsed.length)
        assertTrue(collapsed.endsWith("…"))
    }

    @Test
    fun `format breaks major clauses onto new lines`() {
        val formatted = SqlFormatter.format("select a from t where a = 1 order by a")
        assertEquals(listOf("SELECT a", "FROM t", "WHERE a = 1", "ORDER BY a"), formatted.lines())
    }
}
