package com.appdoctor.database.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueryTypeTest {

    @Test
    fun `classifies leading keyword`() {
        assertEquals(QueryType.SELECT, QueryType.fromSql("  select * from t"))
        assertEquals(QueryType.SELECT, QueryType.fromSql("WITH cte AS (SELECT 1) SELECT * FROM cte"))
        assertEquals(QueryType.INSERT, QueryType.fromSql("INSERT INTO t VALUES (1)"))
        assertEquals(QueryType.INSERT, QueryType.fromSql("REPLACE INTO t VALUES (1)"))
        assertEquals(QueryType.UPDATE, QueryType.fromSql("update t set a = 1"))
        assertEquals(QueryType.DELETE, QueryType.fromSql("DELETE FROM t"))
        assertEquals(QueryType.OTHER, QueryType.fromSql("PRAGMA foreign_keys = ON"))
    }

    @Test
    fun `read write flags`() {
        assertTrue(QueryType.SELECT.isRead)
        assertTrue(QueryType.INSERT.isWrite)
        assertTrue(QueryType.UPDATE.isWrite)
        assertTrue(QueryType.DELETE.isWrite)
        assertFalse(QueryType.OTHER.isRead)
        assertFalse(QueryType.OTHER.isWrite)
    }
}
