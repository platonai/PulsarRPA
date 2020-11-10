package ai.platon.pulsar.ql.h2

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.ql.ResultSets
import com.google.gson.GsonBuilder
import org.apache.commons.lang3.StringUtils
import org.h2.tools.SimpleResultSet
import org.h2.value.DataType
import org.h2.value.Value
import org.slf4j.LoggerFactory
import java.sql.*

object SqlUtils {
    val sqlLog = LoggerFactory.getLogger("ai.platon.pulsar.common.sql.log")
    private val log = LoggerFactory.getLogger(SqlUtils::class.java)

    fun count(rs: ResultSet): Int {
        var count = 0
        rs.beforeFirst()
        while (rs.next()) ++count
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
                    log.warn("The {}th column is expected to be an array, actual {}", j + 1, a.array.javaClass.name)
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

    @Throws(SQLException::class)
    fun executeInsert(sourceRs: ResultSet, sinkConnection: Connection, sinkTable: String, verbose: Boolean = false): Int {
        sinkConnection.autoCommit = false

        var affectedRows = 0
        val columnCount = sourceRs.metaData.columnCount
        val prefix = "insert into `$sinkTable`("
        val postfix = ") values(" + StringUtils.repeat("?", ", ", columnCount) + ")"
        val insertSql = IntRange(1, columnCount).joinToString(", ", prefix, postfix) {
            "`" + sourceRs.metaData.getColumnLabel(it) + "`"
        }

        sinkConnection.prepareStatement(insertSql).use { stmt ->
            sourceRs.beforeFirst()

            while (sourceRs.next()) {
                try {
                    IntRange(1, columnCount).forEach { k ->
                        when (sourceRs.metaData.getColumnType(k)) {
                            Types.BOOLEAN -> stmt.setBoolean(k, sourceRs.getBoolean(k))
                            Types.FLOAT -> stmt.setFloat(k, sourceRs.getFloat(k))
                            Types.INTEGER -> stmt.setInt(k, sourceRs.getInt(k))
                            else -> stmt.setString(k, Strings.stripNonPrintableChar(sourceRs.getString(k)))
                        }
                    }
                } catch (e: SQLException) {
                    log.warn("Failed to create stmt {}", e.message)
                }
            }

            if (verbose) {
                sqlLog.info(stmt.toString())
            }

            affectedRows = stmt.executeUpdate()
            sinkConnection.commit()
        }

        return affectedRows
    }

    @Throws(SQLException::class)
    fun executeBatchInsert(sourceResultSets: Iterable<ResultSet>, sinkConnection: Connection, sinkTable: String, verbose: Boolean = false): Int {
        if (!sourceResultSets.iterator().hasNext()) {
            return 0
        }

        sinkConnection.autoCommit = false

        var affectedRows: IntArray? = null
        val sample = sourceResultSets.first()
        val columnCount = sample.metaData.columnCount
        val prefix = "insert into `$sinkTable`("
        val postfix = ") values(" + StringUtils.repeat("?", ", ", columnCount) + ")"
        val insertSql = IntRange(1, columnCount).joinToString(", ", prefix, postfix) {
            "`" + sample.metaData.getColumnLabel(it) + "`"
        }

        sinkConnection.prepareStatement(insertSql).use { stmt ->
            sourceResultSets.forEach { rs ->
                rs.beforeFirst()
                while (rs.next()) {
                    try {
                        IntRange(1, columnCount).forEach { k ->
                            when (rs.metaData.getColumnType(k)) {
                                Types.BOOLEAN -> stmt.setBoolean(k, rs.getBoolean(k))
                                Types.FLOAT -> stmt.setFloat(k, rs.getFloat(k))
                                Types.INTEGER -> stmt.setInt(k, rs.getInt(k))
                                else -> stmt.setString(k, Strings.stripNonPrintableChar(rs.getString(k)))
                            }
                        }

                        stmt.addBatch()
                    } catch (e: SQLException) {
                        log.warn("Failed to create stmt {}", e.message)
                    }
                }
            }

            if (verbose) {
                sqlLog.info(stmt.toString())
            }

            affectedRows = stmt.executeBatch()
            sinkConnection.commit()
        }

        return affectedRows?.sum()?:0
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
                rs2.addColumn("C$i", DataType.convertTypeToSQLType(Value.STRING), 0, 0) }
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
    fun getEntitiesFromResultSets(resultSets: List<ResultSet>): List<Map<String, Any?>> {
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
            val columnName = metaData.getColumnName(i).toLowerCase()
            record[columnName] = resultSet.getObject(i)
        }
        return record
    }

    @Throws(SQLException::class)
    fun toJson(resultSet: ResultSet): String {
        val entities = getEntitiesFromResultSet(resultSet)
        val gson = GsonBuilder().serializeNulls().create()
        return gson.toJson(entities)
    }

    @Throws(SQLException::class)
    fun toJson(resultSets: List<ResultSet>): String {
        val entities = resultSets.map {
            mapOf(
                    "result" to getEntitiesFromResultSet(it),
                    "statement" to it.statement,
                    "columnCount" to it.metaData.columnCount
            )
        }
        val gson = GsonBuilder().serializeNulls().create()
        return gson.toJson(entities)
    }
}
