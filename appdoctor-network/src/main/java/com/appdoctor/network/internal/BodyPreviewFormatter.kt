package com.appdoctor.network.internal

import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

internal object BodyPreviewFormatter {
    private const val BINARY_PLACEHOLDER = "Binary Data"

    fun format(contentType: String?, text: String?, isBinary: Boolean): String {
        if (isBinary) return BINARY_PLACEHOLDER
        if (text.isNullOrBlank()) return ""
        val trimmed = text.trim()
        if (looksLikeJson(contentType, trimmed)) {
            val json = prettyJson(trimmed)
            if (json != null) return json
        }
        if (looksLikeXml(contentType, trimmed)) {
            val xml = prettyXml(trimmed)
            if (xml != null) return xml
        }
        return text
    }

    private fun looksLikeJson(contentType: String?, body: String): Boolean {
        if (contentType?.contains("json", ignoreCase = true) == true) return true
        return body.startsWith("{") || body.startsWith("[")
    }

    private fun looksLikeXml(contentType: String?, body: String): Boolean {
        if (contentType?.contains("xml", ignoreCase = true) == true) return true
        return body.startsWith("<")
    }

    private fun prettyJson(body: String): String? {
        if (!(body.startsWith("{") || body.startsWith("["))) return null
        val output = StringBuilder(body.length + 16)
        var indent = 0
        var inString = false
        var escaping = false
        body.forEach { ch ->
            when {
                inString -> {
                    output.append(ch)
                    when {
                        escaping -> escaping = false
                        ch == '\\' -> escaping = true
                        ch == '"' -> inString = false
                    }
                }

                ch == '"' -> {
                    inString = true
                    output.append(ch)
                }

                ch == '{' || ch == '[' -> {
                    output.append(ch).append('\n')
                    indent++
                    output.append("  ".repeat(indent))
                }

                ch == '}' || ch == ']' -> {
                    output.append('\n')
                    indent = (indent - 1).coerceAtLeast(0)
                    output.append("  ".repeat(indent)).append(ch)
                }

                ch == ',' -> {
                    output.append(ch).append('\n')
                    output.append("  ".repeat(indent))
                }

                ch == ':' -> output.append(": ")
                ch.isWhitespace() -> Unit
                else -> output.append(ch)
            }
        }
        return output.toString().trim()
    }

    private fun prettyXml(body: String): String? = try {
        val documentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }
        val document = documentBuilderFactory.newDocumentBuilder().parse(InputSource(StringReader(body)))
        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        }
        StringWriter().use { writer ->
            transformer.transform(DOMSource(document), StreamResult(writer))
            writer.toString().trim()
        }
    } catch (_: ParserConfigurationException) {
        null
    } catch (_: SAXException) {
        null
    } catch (_: IOException) {
        null
    } catch (_: TransformerConfigurationException) {
        null
    } catch (_: TransformerException) {
        null
    }
}
