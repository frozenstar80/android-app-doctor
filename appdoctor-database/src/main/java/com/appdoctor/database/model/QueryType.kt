package com.appdoctor.database.model

/**
 * Coarse classification of a SQL statement, derived from its leading keyword.
 *
 * @property isRead whether the statement reads data (drives read/write analytics).
 * @property isWrite whether the statement mutates data.
 */
public enum class QueryType(
    public val isRead: Boolean,
    public val isWrite: Boolean,
) {
    SELECT(isRead = true, isWrite = false),
    INSERT(isRead = false, isWrite = true),
    UPDATE(isRead = false, isWrite = true),
    DELETE(isRead = false, isWrite = true),
    OTHER(isRead = false, isWrite = false);

    public companion object {
        /** Classifies [sql] by its first meaningful keyword; never throws. */
        public fun fromSql(sql: String): QueryType {
            val trimmed = sql.trimStart(' ', '\t', '\n', '\r', '(', ';')
            val keyword = trimmed.takeWhile { !it.isWhitespace() && it != '(' && it != ';' }.uppercase()
            return when (keyword) {
                "SELECT", "WITH" -> SELECT
                "INSERT", "REPLACE" -> INSERT
                "UPDATE" -> UPDATE
                "DELETE" -> DELETE
                else -> OTHER
            }
        }
    }
}
