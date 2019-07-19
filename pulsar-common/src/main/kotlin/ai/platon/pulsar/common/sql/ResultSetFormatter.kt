package ai.platon.pulsar.common.sql

import org.apache.commons.lang3.StringUtils
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types
import java.util.*

class ResultSetFormatter(private val rs: ResultSet, private val asList: Boolean = false) {
    private val meta = rs.metaData
    private val numColumns = meta.columnCount
    private var numRows: Int = 0

    private val rows = ArrayList<List<String>>()
    private val columns = IntRange(1, numColumns).map { meta.getColumnLabel(it) ?: "" }

    private val sb = StringBuilder()

    fun format(): String {
        sb.setLength(0)
        return try {
            if (asList) formatResultAsList() else formatResultAsTable()
        } catch (e: SQLException) {
            "(Exception)" + e.message
        }
    }

    @Throws(SQLException::class)
    private fun formatResultAsTable(): String {
        rows.add(columns)

        while (rs.next()) {
            formatCurrentRow()
            if (numRows++ > MAX_ROW_BUFFER) {
                overflow()
            }
        }

        sb.append(formatRows())

        return sb.toString()
    }

    private fun overflow() {
        sb.append(formatRows())
        sb.append("\n")
        rows.clear()
    }

    @Throws(SQLException::class)
    private fun formatResultAsList(): String {
        var longestLabel = 0
        val columns = arrayOfNulls<String>(numColumns)
        for (i in 0 until numColumns) {
            val s = meta.getColumnLabel(i + 1)
            columns[i] = s
            longestLabel = Math.max(longestLabel, s.length)
        }

        val sb = StringBuilder()
        while (rs.next()) {
            numRows++
            sb.setLength(0)
            if (numRows > 1) {
                sb.append("")
            }

            for (i in 0 until numColumns) {
                if (i > 0) {
                    sb.append('\n')
                }

                sb.append(StringUtils.rightPad(columns[i] + ":", 15 + longestLabel))
                        .append(rs.getString(i + 1))
            }
            sb.append("\n")
        }

        if (numRows == 0) {
            val s = columns.joinToString("\n")
            sb.append(s).append("\n")
        }
        return sb.toString()
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
                max = Math.max(max, row[i].length)
            }
            if (numColumns > 1) {
                max = Math.min(MAX_COLUMN_LENGTH, max)
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
                buff.append(s)
                if (i < numColumns - 1) {
                    for (j in s.length until columnSizes[i]) {
                        buff.append(' ')
                    }
                }

                ++i
            }

            buff.append("\n")
        }

        return buff.toString()
    }

    override fun toString(): String {
        return format() + "Total " + numRows + " rows"
    }

    companion object {
        var MAX_ROW_BUFFER = 5000
        var MAX_COLUMN_LENGTH = 1000
        var BOX_VERTICAL = '|'
    }
}
