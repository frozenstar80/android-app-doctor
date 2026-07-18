package com.appdoctor.database.internal

/** Best-effort extraction of referenced table names from a SQL statement. */
internal object SqlTableExtractor {

    private val TABLE = Regex(
        "(?i)(?:\\bfrom\\b|\\bjoin\\b|\\binto\\b|\\bupdate\\b)\\s+[\"'`\\[]?([A-Za-z_][A-Za-z0-9_]*)",
    )

    /** Distinct, lower-cased table names referenced by [sql]. */
    fun extract(sql: String): List<String> =
        TABLE.findAll(sql)
            .map { it.groupValues[1].lowercase() }
            .distinct()
            .toList()
}
