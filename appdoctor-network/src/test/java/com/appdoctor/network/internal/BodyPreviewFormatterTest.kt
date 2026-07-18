package com.appdoctor.network.internal

import org.junit.Assert.assertEquals
import org.junit.Test

class BodyPreviewFormatterTest {

    @Test
    fun `json gets pretty printed`() {
        val formatted = BodyPreviewFormatter.format("application/json", """{"a":1,"b":2}""", isBinary = false)
        assertEquals("{\n  \"a\": 1,\n  \"b\": 2\n}", formatted)
    }

    @Test
    fun `xml gets pretty printed`() {
        val formatted = BodyPreviewFormatter.format("application/xml", "<root><item>1</item></root>", isBinary = false)
        assertEquals("<root>\n  <item>1</item>\n</root>", formatted)
    }

    @Test
    fun `binary is replaced with placeholder`() {
        val formatted = BodyPreviewFormatter.format("application/octet-stream", "ignored", isBinary = true)
        assertEquals("Binary Data", formatted)
    }
}
