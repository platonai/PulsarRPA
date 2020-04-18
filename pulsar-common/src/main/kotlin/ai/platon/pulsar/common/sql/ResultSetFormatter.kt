package ai.platon.pulsar.common.sql

import org.apache.commons.lang3.StringUtils
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types
import java.util.*

class ResultSetFormatter(
        private val rs: ResultSet,
        private val asList: Boolean = false,
        val buffer: StringBuilder = StringBuilder()
) {
    private val meta = rs.metaData
    private val numColumns = meta.columnCount
    var numRows: Int = 0
        private set
    var numNonBlankFields: Int = 0
        private set

    private val rows = ArrayList<List<String>>()
    private val columns = IntRange(1, numColumns).map { meta.getColumnLabel(it) ?: "" }

    fun format() {
        buffer.setLength(0)
        try {
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
        rows.add(columns)

        while (rs.next()) {
            formatCurrentRow()
            if (numRows++ > MAX_ROW_BUFFER) {
                overflow()
            }
        }

        buffer.append(formatRows())
    }

    private fun overflow() {
        buffer.append(formatRows())
        buffer.append("\n")
        rows.clear()
    }

    @Throws(SQLException::class)
    private fun formatResultAsList() {
        var longestLabel = 0
        val columns = arrayOfNulls<String>(numColumns)
        for (i in 0 until numColumns) {
            val s = meta.getColumnLabel(i + 1)
            columns[i] = s
            longestLabel = longestLabel.coerceAtLeast(s.length)
        }

        val sb = StringBuilder()
        while (rs.next()) {
            numRows++
            sb.setLength(0)

            IntRange(0, numColumns - 1).forEach { i ->
                if (i > 0) {
                    sb.append('\n')
                }

                val padding = StringUtils.rightPad(columns[i] + ":", 15 + longestLabel)
                val value = rs.getString(i + 1)
                if (value.isNotBlank()) {
                    ++numNonBlankFields
                }
                sb.append(padding).append(value)
            }

            sb.append("\n")
        }

        if (numRows == 0) {
            val s = columns.joinToString("\n")
            sb.append(s).append("\n")
        }
    }

    @Throws(SQLException::class)
    private fun formatCurrentRow() {
        IntRange(1, numColumns)
                .map { StringUtils.abbreviateMiddle(formatColumn(it), "..", MAX_COLUMN_LENGTH) }
                .also { rows.add(it) }
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
    private fun formatColumn(columnIndex: Int): String {
        var s: String?
        when (rs.metaData.getColumnType(columnIndex)) {
            Types.DOUBLE, Types.FLOAT, Types.REAL -> {
                var precision = rs.metaData.getPrecision(columnIndex)
                var scale = rs.metaData.getScale(columnIndex)
                if (precision !in 0..10) precision = 10
                if (scale !in 0..10) scale = 10
                val d = rs.getDouble(columnIndex)
                s = String.format("%" + precision + "." + scale + "f", d)
            }
            else -> s = rs.getString(columnIndex)
        }

        if (s == null) {
            s = "null"
        }

        return s
    }

    private fun formatRows(): String {
        val columnSizes = IntArray(numColumns)
        for (i in 0 until numColumns) {
            var max = 0
            for (row in rows) {
                max = max.coerceAtLeast(row[i].length)
            }
            if (numColumns > 1) {
                max = MAX_COLUMN_LENGTH.coerceAtMost(max)
            }
            columnSizes[i] = max
        }

        val buff = StringBuilder()
        for (row in rows) {
            var i = 0
            while (i < numColumns) {
                if (i > 0) {
                    buff.append(' ').append(BOX_VERTICAL).append(' ')
                }

                val s = row[i]
                if (s.isNotBlank()) {
                    ++numNonBlankFields
                }

                buff.append(s)
                if (i < numColumns - 1) {
                    repeat(columnSizes[i] - s.length) { buff.append(' ') }
//                    for (j in s.length until columnSizes[i]) {
//                        buff.append(' ')
//                    }
                }

                ++i
            }

            buff.append("\n")
        }

        return buff.toString()
    }

    companion object {
        var MAX_ROW_BUFFER = 5000
        var MAX_COLUMN_LENGTH = 1000
        var BOX_VERTICAL = '|'
    }
}
