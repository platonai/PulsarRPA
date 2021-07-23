package ai.platon.pulsar.ql.h2.utils

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.ql.ResultSets
import ai.platon.pulsar.ql.h2.addColumn
import ai.platon.pulsar.ql.types.ValueDom
import com.google.gson.GsonBuilder
import org.h2.tools.SimpleResultSet
import org.h2.value.DataType
import org.h2.value.Value
import java.nio.file.Files
import java.nio.file.Path
import java.sql.*
import java.util.*
import kotlin.collections.ArrayList

/**
 * Only for small result sets
 * */
object ResultSetUtils {
    private val logger = getLogger(this)
    val sqlLog = getLogger(ResultSetUtils::class.java.packageName, ".Jdbc")

    private val a = AppConstants.SHORTEST_VALID_URL_LENGTH
    private const val b = 2048
    val QUOTED_URL_REGEX = "'https?://.{$a,$b}?'".toRegex()

    fun count(rs: ResultSet): Int {
        var count = 0
        rs.beforeFirst()
        while (rs.next()) ++count
        return count
    }

    fun count(rs: ResultSet, predicate: (ResultSet) -> Boolean): Int {
        var count = 0
        rs.beforeFirst()
        while (rs.next()) {
            if (predicate(rs)) ++count
        }
        return count
    }

    fun countRemaining(rs: ResultSet): Int {
        var count = 0
        while (rs.next()) ++count
        return count
    }

    /**
     * Transpose a result set if the result set has only row and every row is an array
     * */
    fun transpose(rs: ResultSet): ResultSet {
        val newRs = ResultSets.newSimpleResultSet()

        rs.beforeFirst()
        if (!rs.next() || rs.wasNull()) {
            return newRs
        }

        val columnCount = rs.metaData.columnCount
        IntRange(1, columnCount).map { i -> newRs.addColumn(rs.metaData.getColumnName(i)) }

        val array = rs.getArray(1)
        val nativeArray = (array.array as? kotlin.Array<*>) ?: return newRs
        val newRowCount = nativeArray.size

        if (columnCount == 0) {
            return newRs
        }

        val rows = Array<Array<String?>>(newRowCount) { arrayOfNulls(columnCount) }

        IntRange(0, newRowCount - 1).forEach outer@{ i ->
            IntRange(0, columnCount - 1).forEach inner@{ j ->
                val a = rs.getArray(j + 1)
                if (a.array !is kotlin.Array<*>) {
                    logger.warn("The {}th column is expected to be an array, actual {}", j + 1, a.array.javaClass.name)
                    rows[i][j] = null
                    return@inner
                }

                val na = a.array as kotlin.Array<*>
                if (i < na.size) {
                    rows[i][j] = na[i].toString()
                } else {
                    rows[i][j] = null
                }
            }
        }

        rows.forEach { row -> newRs.addRow(*row) }
        return newRs
    }

    /**
     * A very simple and limited result set creator
     * @see [org.springframework.jdbc.core.StatementCreatorUtils]
     * */
    fun copyResultSet(sourceRs: ResultSet, maxRows: Int = 10000): ResultSet {
        val sinkRs = SimpleResultSet().apply { autoClose = false }

        val metaData = sourceRs.metaData
        val columnCount = metaData.columnCount

        IntRange(1, columnCount).forEach { i ->
            val name = metaData.getColumnName(i)
            val type = metaData.getColumnType(i)
            val precision = metaData.getPrecision(i)
            val scale = metaData.getScale(i)

            sinkRs.addColumn(name, type, precision, scale)
        }

        var numRows = 0
        sourceRs.beforeFirst()
        while (sourceRs.next() && numRows++ < maxRows) {
            val row = arrayOfNulls<Any?>(columnCount)
            IntRange(1, columnCount).forEach { i ->
                val type = sourceRs.metaData.getColumnType(i)
                /**
                 * TODO: handle other types
                 * */
                row[i - 1] = when (type) {
                    Types.BOOLEAN -> sourceRs.getBoolean(i)
                    Types.FLOAT -> sourceRs.getFloat(i)
                    Types.INTEGER -> sourceRs.getInt(i)
                    else -> sourceRs.getString(i)
                }
            }

            sinkRs.addRow(*row)
        }

        return sinkRs
    }

    fun joinSimpleTables(tableNames: List<String>, stat: Statement): ResultSet {
        if (tableNames.isEmpty()) {
            return SimpleResultSet().apply { autoClose = true }
        }

        // make a join
        val rows = mutableMapOf<String, ArrayList<String?>>()
        tableNames.forEachIndexed { i, tableName ->
            val rs = stat.executeQuery("SELECT * FROM $tableName")
            val columnCount = rs.metaData.columnCount
            while (rs.next()) {
                val u = rs.getString("URL")
                val row = rows.computeIfAbsent(u) { ArrayList() }
                IntRange(1, columnCount).mapTo(row) { rs.getString(it) }
            }
        }

        val rs2 = SimpleResultSet()
        // remove row if all values are null
        val rows2 = rows.filterNot { it.value.all { it == null || it == "null" } }
        if (rows2.values.isNotEmpty()) {
            rows2.values.first().forEachIndexed { i, _ ->
                rs2.addColumn("C$i", DataType.convertTypeToSQLType(Value.STRING), 0, 0)
            }
            rows2.values.forEach { rs2.addRow(*it.toTypedArray()) }
        }
        return rs2
    }

    @Throws(SQLException::class)
    fun getEntitiesFromResultSet(resultSet: ResultSet): List<Map<String, Any?>> {
        val entities = arrayListOf<Map<String, Any?>>()
        resultSet.beforeFirst()
        while (resultSet.next()) {
            entities.add(getEntityFromCurrentRecord(resultSet))
        }
        return entities
    }

    @Throws(SQLException::class)
    fun getEntitiesFromResultSetTo(
        resultSet: ResultSet,
        destination: MutableList<Map<String, Any?>>,
    ): MutableList<Map<String, Any?>> {
        resultSet.beforeFirst()
        while (resultSet.next()) {
            destination.add(getEntityFromCurrentRecord(resultSet))
        }
        return destination
    }

    @Throws(SQLException::class)
    fun getEntitiesFromResultSets(resultSets: Iterable<ResultSet>): List<Map<String, Any?>> {
        return resultSets.map<ResultSet, Map<String, Any>> {
            mapOf(
                "result" to getEntitiesFromResultSet(it),
                "statement" to it.statement,
                "columnCount" to it.metaData.columnCount
            )
        }
    }

    @Throws(SQLException::class)
    private fun getEntityFromCurrentRecord(resultSet: ResultSet): Map<String, Any?> {
        val metaData = resultSet.metaData
        val columnCount: Int = metaData.columnCount
        val record = mutableMapOf<String, Any?>()
        for (i in 1..columnCount) {
            val columnName = metaData.getColumnName(i)
            val columnType = metaData.getColumnType(i)
            // remove ValueDom from the result
            if (columnType != ValueDom.type && columnName !in arrayOf("DOC", "DOM")) {
                record[columnName.toLowerCase()] = resultSet.getObject(i)
            }
        }
        return record
    }

    @Throws(SQLException::class)
    fun getTextEntitiesFromResultSet(resultSet: ResultSet): List<Map<String, Any?>> {
        val entities = arrayListOf<Map<String, Any?>>()
        resultSet.beforeFirst()
        while (resultSet.next()) {
            entities.add(getTextEntityFromCurrentRecord(resultSet))
        }
        return entities
    }

    @Throws(SQLException::class)
    private fun getTextEntityFromCurrentRecord(resultSet: ResultSet): Map<String, String?> {
        val metaData = resultSet.metaData
        val columnCount: Int = metaData.columnCount
        val record = mutableMapOf<String, String?>()
        for (i in 1..columnCount) {
            val columnName = metaData.getColumnName(i)
            val columnType = metaData.getColumnType(i)
            // remove ValueDom from the result
            if (columnType != ValueDom.type && columnName !in arrayOf("DOC", "DOM")) {
                record[columnName.toLowerCase()] = resultSet.getString(i)
            }
        }
        return record
    }

    @Throws(SQLException::class)
    fun toJson(resultSet: ResultSet): String {
        val entities = getTextEntitiesFromResultSet(resultSet)
        val gson = GsonBuilder().serializeNulls().create()
        return gson.toJson(entities)
    }

    @Throws(SQLException::class)
    fun toJson(resultSets: List<ResultSet>): String {
        val entities = resultSets.map {
            mapOf(
                "result" to getTextEntitiesFromResultSet(it),
                "statement" to it.statement,
                "columnCount" to it.metaData.columnCount
            )
        }
        val gson = GsonBuilder().serializeNulls().create()
        return gson.toJson(entities)
    }

    @Throws(SQLException::class)
    fun toCSV(rs: ResultSet, separator: String = "|"): String {
        val entities = getTextEntitiesFromResultSet(rs)
        return entities.joinToString("\n") {
            it.values.joinToString(separator)
        }
    }

    @Throws(SQLException::class)
    fun toCSV(resultsets: Iterable<ResultSet>, separator: String = "|"): String {
        val sb = StringBuilder()
        resultsets.forEach { rs ->
            val entities = getTextEntitiesFromResultSet(rs)
            entities.forEach { entity ->
                entity.values.joinTo(sb, separator) { it.toString().replace(separator, "^^") }
                sb.appendLine()
            }
        }
        return sb.toString()
    }

    fun exportToCSV(resultsets: Iterable<ResultSet>, separator: String = "|"): Path {
        val now = System.currentTimeMillis()
        val path = AppPaths.getTmp("rs").resolve("$now.csv")
        Files.createDirectories(path.parent)
        Files.writeString(path, toCSV(resultsets, separator))
        return path
    }

    /**
     * A simple method to extract url from a sql's from clause, for example,
     * extract `https://jd.com/` from the following sql:
     *
     * > select dom_first_text(dom, '#container'), dom_first_text(dom, '.price')
     * > from load_and_select('https://jd.com/', ':root body');
     *
     * TODO: use a simple SQL parser
     *
     * @param sql The sql to extract an url from
     * @return The url extracted from the sql, null if no such url
     * */
    fun extractUrlFromFromClause(sql: String): String? {
        val len = sql.length
        var i = 0
        var j = 0
        // find the last 'from'
        i = sql.lastIndexOf("from", ignoreCase = true)
        if (i <= 0) {
            return null
        }

        // find "(" in from clause "from load_and_select('https:"
        while (sql[i] != '(' && i < len) {
            ++i
        }
        if (i == len) {
            return null
        }

        // find the first single quote
        while (sql[i] != '\'' && i < len) {
            ++i
        }
        // the start of the url
        ++i
        if (i >= len) {
            return null
        }

        // find the second single quote
        j = i + 1
        while (sql[j] != '\'' && i < len) {
            ++j
        }
        if (j == len) {
            return null
        }

        return sql.substring(i, j)
    }
}
