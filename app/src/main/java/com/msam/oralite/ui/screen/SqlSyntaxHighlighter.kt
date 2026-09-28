package com.msam.oralite.ui.screen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.util.regex.Pattern

class SqlSyntaxVisualTransformation(private val isDark: Boolean) : VisualTransformation {

    // Syntax Palette
    private val keywordColor = if (isDark) Color(0xFF79C0FF) else Color(0xFF0969DA)
    private val functionColor = if (isDark) Color(0xFFD2A8FF) else Color(0xFF8250DF)
    private val stringColor = if (isDark) Color(0xFF7EE787) else Color(0xFF1A7F37)
    private val numberColor = if (isDark) Color(0xFFFFA657) else Color(0xFF9A6700)
    private val commentColor = if (isDark) Color(0xFF8B949E) else Color(0xFF6E7681)
    private val operatorColor = if (isDark) Color(0xFFFF7B72) else Color(0xFFCF222E)

    companion object {
        private val KEYWORDS_REGEX = Pattern.compile(
            "\\b(?i)(SELECT|FROM|WHERE|AND|OR|NOT|IN|IS|NULL|LIKE|BETWEEN|ORDER\\s+BY|GROUP\\s+BY|HAVING|LEFT\\s+JOIN|RIGHT\\s+JOIN|INNER\\s+JOIN|OUTER\\s+JOIN|CROSS\\s+JOIN|JOIN|ON|AS|DISTINCT|UNION(\\s+ALL)?|INSERT\\s+INTO|VALUES|UPDATE|SET|DELETE(\\s+FROM)?|CREATE|TABLE|VIEW|INDEX|DROP|ALTER|TRUNCATE|CASE|WHEN|THEN|ELSE|END|LIMIT|OFFSET|ROWNUM|WITH|FETCH\\s+FIRST|ROWS?\\s+ONLY)\\b"
        )
        private val FUNCTIONS_REGEX = Pattern.compile(
            "\\b(?i)(DUAL|SYSDATE|SYSTIMESTAMP|COUNT|SUM|AVG|MIN|MAX|NVL|NVL2|COALESCE|DECODE|TO_CHAR|TO_DATE|TO_NUMBER|SUBSTR|INSTR|LENGTH|TRIM|UPPER|LOWER|ROUND|TRUNC|ROWID)\\b"
        )
        private val NUMBERS_REGEX = Pattern.compile("\\b\\d+(\\.\\d+)?\\b")
        private val STRINGS_REGEX = Pattern.compile("'([^'\\\\]|\\\\.)*'")
        private val LINE_COMMENTS_REGEX = Pattern.compile("--.*$", Pattern.MULTILINE)
        private val BLOCK_COMMENTS_REGEX = Pattern.compile("/\\*[\\s\\S]*?\\*/")
        private val OPERATORS_REGEX = Pattern.compile("(=|!=|<>|<=|>=|<|>|\\+|-|\\*|/|\\|\\|)")
    }

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val builder = AnnotatedString.Builder(raw)

        // 1. Highlight Keywords
        val kwMatcher = KEYWORDS_REGEX.matcher(raw)
        while (kwMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold),
                kwMatcher.start(),
                kwMatcher.end()
            )
        }

        // 2. Highlight Functions / Special Oracle Objects
        val fnMatcher = FUNCTIONS_REGEX.matcher(raw)
        while (fnMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = functionColor, fontWeight = FontWeight.SemiBold),
                fnMatcher.start(),
                fnMatcher.end()
            )
        }

        // 3. Highlight Operators
        val opMatcher = OPERATORS_REGEX.matcher(raw)
        while (opMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = operatorColor),
                opMatcher.start(),
                opMatcher.end()
            )
        }

        // 4. Highlight Numbers
        val numMatcher = NUMBERS_REGEX.matcher(raw)
        while (numMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = numberColor, fontWeight = FontWeight.Medium),
                numMatcher.start(),
                numMatcher.end()
            )
        }

        // 5. Highlight Strings (higher precedence than keywords inside string)
        val strMatcher = STRINGS_REGEX.matcher(raw)
        while (strMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = stringColor),
                strMatcher.start(),
                strMatcher.end()
            )
        }

        // 6. Highlight Comments (highest precedence: overrides all)
        val lineCommentMatcher = LINE_COMMENTS_REGEX.matcher(raw)
        while (lineCommentMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = commentColor, fontStyle = FontStyle.Italic),
                lineCommentMatcher.start(),
                lineCommentMatcher.end()
            )
        }

        val blockCommentMatcher = BLOCK_COMMENTS_REGEX.matcher(raw)
        while (blockCommentMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = commentColor, fontStyle = FontStyle.Italic),
                blockCommentMatcher.start(),
                blockCommentMatcher.end()
            )
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

object SqlFormatter {
    fun format(sql: String): String {
        if (sql.isBlank()) return sql
        var s = sql.trim().replace(Regex("[ \\t]+"), " ")

        val clauses = listOf(
            "SELECT", "FROM", "WHERE", "GROUP BY", "HAVING", "ORDER BY",
            "LEFT JOIN", "RIGHT JOIN", "INNER JOIN", "CROSS JOIN", "JOIN",
            "UNION ALL", "UNION", "INSERT INTO", "VALUES", "UPDATE", "SET", "DELETE FROM"
        )
        for (clause in clauses) {
            val pattern = Regex("(?i)\\b$clause\\b")
            s = s.replace(pattern) { "\n${it.value.uppercase()}" }
        }

        // Break conditions in WHERE/JOIN onto clean indented lines
        s = s.replace(Regex("(?i)\\b(AND|OR)\\b")) { "\n  ${it.value.uppercase()}" }

        val lines = s.lines().map { it.trimEnd() }.filter { it.isNotBlank() }
        val formatted = StringBuilder()
        for (line in lines) {
            val upper = line.trim().uppercase()
            val isMajor = clauses.any { upper.startsWith(it) }
            val isCondition = upper.startsWith("AND ") || upper.startsWith("OR ")
            if (!isMajor && !isCondition && formatted.isNotEmpty()) {
                formatted.append("  ")
            }
            formatted.append(line).append("\n")
        }
        return formatted.toString().trim()
    }
}
