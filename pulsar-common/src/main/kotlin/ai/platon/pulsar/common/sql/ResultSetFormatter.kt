package ai.platon.pulsar.common.sql

import ai.platon.pulsar.common.Strings
import org.apache.commons.lang3.StringUtils
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types
import java.util.*

class ResultSetFormatter(
    private val rs: ResultSet,
    private val asList: Boolean = false,
    private val withHeader: Boolean = false,
    @Deprecated("Not used anymore")
    private val textOnly: Boolean = false,
    private val maxColumnLength: Int = 120,
    val buffer: StringBuilder = StringBuilder()
) {
    private val meta = rs.metaData
    private val numColumns = meta.columnCount
    var numRows: Int = 0
        private set
    var numNonBlankFields: Int = 0
        private set
    var numNonNullFields = 0
        private set
    var numFields = 0
        private set

    var fieldSeparator = Strings.COMMA
    var fieldSeparatorReplace = Strings.FULL_WIDTH_COMMA
    var maxRowBuffer = 5000
    var boxVertical = '|'

    private val rows = ArrayList<List<String>>()
    private val columns = IntRange(1, numColumns).map { meta.getColumnLabel(it) ?: "" }

    fun format() {
        try {
            rs.beforeFirst()
            if (asList) formatResultAsList() else formatResultAsTable()
        } catch (e: SQLException) {
            "(Exception)" + e.message
        }
    }

    override fun toString(): String {
        if (buffer.isEmpty()) {
            format()
        }
        return buffer.toString()
    }

    @Throws(SQLException::class)
    private fun formatResultAsTable() {
        if (withHeader) {
            rows.add(columns)
        }

        var i = 0
        while (rs.next()) {
            // Overflow occurs, clear buffer
            if (i++ > 0 && rows.isEmpty()) {
                buffer.setLength(0)
            }

            formatCurrentRow()
            if (numRows++ > maxRowBuffer) {
                overflow()
            }
        }

        formatRows()
    }

    private fun overflow() {
        formatRows()
        buffer.append("\n")
        rows.clear()
    }

    @Throws(SQLException::class)
    private fun formatResultAsList() {
        var labelLength = 0
        val columns = arrayOfNulls<String>(numColumns)
        for (i in 0 until numColumns) {
            val label = meta.getColumnLabel(i + 1).also { columns[i] = it }
            labelLength = labelLength.coerceAtLeast(label.length)
        }

        while (rs.next()) {
            numRows++

            IntRange(0, numColumns - 1).forEach { i ->
                if (i > 0) {
                    buffer.append('\n')
                }

                val key = StringUtils.rightPad(columns[i] + ":", 15 + labelLength)
                val value = rs.getString(i + 1)

                ++numFields
                if (value != null) {
                    ++numNonNullFields
                    if (value.isNotBlank()) {
                        ++numNonBlankFields
                    }
                }

                val cellText = abbreviateTextCell(value)
                buffer.append(key).append(cellText)
            }

            buffer.append("\n")
        }

        // generate an empty list
        if (numRows == 0) {
            columns.joinTo(buffer, "\n")
            buffer.append("\n")
        }
    }

    @Throws(SQLException::class)
    private fun formatCurrentRow() {
        IntRange(1, numColumns).map { abbreviateTextCell(formatCell(it)) }.also { rows.add(it) }
    }

    private fun abbreviateTextCell(cellText: String): String {
        return StringUtils.abbreviateMiddle(cellText, "...", maxColumnLength)
    }

    /**
     * Precision and scale:
     *
     * Precision is the number of digits in a number.
     * Scale is the number of digits to the right of the decimal point in a number.
     * For example, the number 123.45 has a precision of 5 and a scale of 2.
     * @link {https://docs.microsoft.com/en-us/sql/t-sql/data-types/precision-scale-and-length-transact-sql?view=sql-server-2017}
     */
    @Throws(SQLException::class)
    private fun formatCell(columnIndex: Int): String {
//        if (textOnly) {
//            return rs.getString(columnIndex)?.replace("\n", "") ?: "null"
//        }

        return when (rs.metaData.getColumnType(columnIndex)) {
            Types.DOUBLE, Types.FLOAT, Types.REAL -> {
                val fmt = getFloatColumnFormat(columnIndex)
                val value = rs.getDouble(columnIndex)
                String.format(fmt, value)
            }
            Types.ARRAY -> {
                when (val array = rs.getArray(columnIndex)?.array) {
                    null -> "null"
                    is Array<*> -> array.joinToString("$fieldSeparator", "(", ")") { sanitize(it.toString()) }
                    else -> array.toString()
                }
            }
            else -> sanitize(rs.getString(columnIndex))
        }
    }

    private fun sanitize(value: String?): String {
        if (value == null) return "null"
        return value.replace("\n", "\t").replace(fieldSeparator, fieldSeparatorReplace)
    }

    private fun getFloatColumnFormat(columnIndex: Int): String {
        val precision = rs.metaData.getPrecision(columnIndex).coerceIn(6, 10)
        val scale = rs.metaData.getScale(columnIndex).coerceIn(2, 6)
        return "%${precision}.${scale}f"
    }

    private fun formatRows() {
        val columnSizes = IntArray(numColumns)
        for (i in 0 until numColumns) {
            var max = 0
            for (row in rows) {
                max = max.coerceAtLeast(row[i].length)
            }
            if (numColumns > 1) {
                max = maxColumnLength.coerceAtMost(max)
            }
            columnSizes[i] = max
        }

        rows.forEach { row ->
            row.forEachIndexed { j, value ->
                if (j > 0) {
                    buffer.append(' ').append(boxVertical).append(' ')
                }

                buffer.append(value)

                if (j < numColumns - 1) {
                    // padding
                    repeat(columnSizes[j] - value.length) { buffer.append(' ') }
                }

                if (value.isNotBlank()) {
                    ++numNonBlankFields
                }
            }

            buffer.append("\n")
        }
    }
}
