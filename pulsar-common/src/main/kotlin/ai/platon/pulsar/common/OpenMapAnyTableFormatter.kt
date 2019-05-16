package ai.platon.pulsar.common

import org.apache.commons.lang3.StringUtils
import java.sql.SQLException
import java.util.ArrayList

class OpenMapAnyTableFormatter(val table: OpenMapAnyTable) {
    private var rowCount: Int = 0

    @JvmOverloads
    fun format(asList: Boolean = false): String {
        return try {
            if (asList) {
                resultAsList
            } else resultAsTable
        } catch (e: SQLException) {
            "(Exception)" + e.message
        }
    }

    fun formatAsLine(): String {
        return format(true)
    }

    private val resultAsTable: String
        get() {
            val sb = StringBuilder()

            val metadata = table.metadata
            val numColumns = metadata.columns.size
            val rows = ArrayList<Array<String>>()

            val columns = metadata.columns.map { it.name }.toTypedArray()
            rows.add(columns)

            table.rows.forEach {
                it.cells.map { StringUtils.abbreviate(it?.toString()?:"null", MAX_COLUMN_LENGTH) }
                        .toTypedArray().also { rows.add(it) }

                if (++rowCount > MAX_ROW_BUFFER) {
                    sb.append(formatRows(rows, numColumns))
                    sb.append("\n")
                    rows.clear()
                }
            }

            sb.append(formatRows(rows, numColumns))
            sb.append("\n")
            rows.clear()

            return sb.toString()
        }

    private val resultAsList: String
        get() {
            val meta = table.metadata
            var longestLabel = 0
            val numColumns = meta.columns.size
            val columns = mutableListOf<String>()
            for (i in 0 until numColumns) {
                val name = meta.columns[i + 1].name
                columns[i] = name
                longestLabel = Math.max(longestLabel, name.length)
            }

            val sb = StringBuilder()
            table.rows.forEach {
                rowCount++
                sb.setLength(0)
                if (rowCount > 1) {
                    sb.append("")
                }

                for (i in 0 until numColumns) {
                    if (i > 0) {
                        sb.append('\n')
                    }

                    sb.append(StringUtils.rightPad(columns[i] + ":", 15 + longestLabel))
                            .append(it[i + 1].toString())
                }
                sb.append("\n")
            }
            if (rowCount == 0) {
                val s = columns.joinToString("\n") { it }
                sb.append(s).append("\n")
            }
            return sb.toString()
        }

    private fun formatRows(rows: ArrayList<Array<String>>, numColumns: Int): String {
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
        return format() + "Total " + rowCount + " rows"
    }

    companion object {
        var MAX_ROW_BUFFER = 5000
        var MAX_COLUMN_LENGTH = 1000
        var BOX_VERTICAL = '|'
    }
}
