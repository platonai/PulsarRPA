package ai.platon.pulsar.common.sql

import org.apache.commons.lang3.StringUtils
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types
import java.util.*

class ResultSetFormatter(
        private val rs: ResultSet,
        private val asList: Boolean = false,
        private val withHeader: Boolean = false,
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

    private val rows = ArrayList<List<String>>()
    private val columns = IntRange(1, numColumns).map { meta.getColumnLabel(it) ?: "" }

    fun format() {
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
            if (numRows++ > MAX_ROW_BUFFER) {
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

                val th = StringUtils.rightPad(columns[i] + ":", 15 + labelLength)
                val td = rs.getString(i + 1)
                buffer.append(th).append(td)

                ++numFields
                if (td != null) {
                    ++numNonNullFields
                    if (td.isNotBlank()) {
                        ++numNonBlankFields
                    }
                }
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
        IntRange(1, numColumns).map { StringUtils.abbreviateMiddle(formatColumn(it), "..", MAX_COLUMN_LENGTH) }
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
        return when (rs.metaData.getColumnType(columnIndex)) {
            Types.DOUBLE, Types.FLOAT, Types.REAL -> {
                String.format(getColumnFormat(columnIndex), rs.getDouble(columnIndex)).trimEnd('0').trimEnd('.')
            }
            else -> rs.getString(columnIndex) ?: "null"
        }
    }

    private fun getColumnFormat(columnIndex: Int): String {
        val precision = rs.metaData.getPrecision(columnIndex)
        val scale = rs.metaData.getScale(columnIndex)
        return when {
            precision == 0 && scale == 0 -> "%f"
            precision == 0 -> "%.${scale}f"
            scale == 0 -> "%${precision}.f"
            else -> "%f"
        }
    }

    private fun formatRows() {
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

        rows.forEach { row ->
            row.forEachIndexed { j, value ->
                if (j > 0) {
                    buffer.append(' ').append(BOX_VERTICAL).append(' ')
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

    companion object {
        var MAX_ROW_BUFFER = 5000
        var MAX_COLUMN_LENGTH = 1000
        var BOX_VERTICAL = '|'
    }
}
