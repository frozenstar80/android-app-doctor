package com.appdoctor.database.internal

/** Pretty-prints, collapses and normalises SQL for display and grouping. */
internal object SqlFormatter {

    private val WHITESPACE = Regex("\\s+")

    // Broken onto their own line when pretty-printing. Multi-word entries come first so a
    // single-word keyword never splits a longer one.
    private val BREAK_KEYWORDS = listOf(
        "GROUP BY", "ORDER BY", "LEFT JOIN", "RIGHT JOIN", "INNER JOIN", "OUTER JOIN", "CROSS JOIN",
        "SELECT", "FROM", "WHERE", "JOIN", "HAVING", "LIMIT", "OFFSET", "VALUES", "SET", "UNION", "ON", "AND", "OR",
    )

    /** Collapses all whitespace to single spaces and trims. Used to group identical SQL. */
    fun normalize(sql: String): String = sql.replace(WHITESPACE, " ").trim()

    /** A single-line preview, truncated with an ellipsis past [max] characters. */
    fun collapse(sql: String, max: Int = 120): String {
        val normalized = normalize(sql)
        return if (normalized.length <= max) normalized else normalized.take(max - 1).trimEnd() + "…"
    }

    /** Multi-line pretty print with major keywords starting each clause. */
    fun format(sql: String): String {
        var result = normalize(sql)
        for (keyword in BREAK_KEYWORDS) {
            result = Regex("(?i)(?<![A-Za-z0-9_])${Regex.escape(keyword)}(?![A-Za-z0-9_])")
                .replace(result) { "\n" + it.value.uppercase() }
        }
        return result
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }
}
