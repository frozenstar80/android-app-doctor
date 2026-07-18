package com.appdoctor.database.internal

import org.junit.Assert.assertEquals
import org.junit.Test

class SqlTableExtractorTest {

    @Test
    fun `extracts from insert update and join`() {
        assertEquals(listOf("users"), SqlTableExtractor.extract("SELECT * FROM users"))
        assertEquals(listOf("orders"), SqlTableExtractor.extract("INSERT INTO orders VALUES (1)"))
        assertEquals(listOf("users"), SqlTableExtractor.extract("UPDATE users SET a = 1"))
        assertEquals(listOf("a", "b"), SqlTableExtractor.extract("SELECT * FROM a JOIN b ON a.id = b.id"))
    }
}
