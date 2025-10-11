package ai.platon.pulsar.ql.h2.utils

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.getLogger
import org.apache.commons.lang3.StringUtils
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types

object JdbcUtils {
    private val logger = getLogger(this)

    @Throws(SQLException::class)
    fun executeInsert(
        sourceRs: ResultSet,
        sinkConnection: Connection,
        sinkTable: String,
        dryRun: Boolean = false
    ): Int {
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
                            else -> stmt.setString(k, Strings.removeNonPrintableChar(sourceRs.getString(k)))
                        }
                    }
                } catch (e: SQLException) {
                    logger.warn("Failed to create stmt {}", e.message)
                }
            }

            if (dryRun) {
                val sql = "insert" + stmt.toString().substringAfter("insert") + ";"
                ResultSetUtils.sqlLog.info(sql)
            } else {
                affectedRows = stmt.executeUpdate()
                sinkConnection.commit()
            }
        }

        return affectedRows
    }

    @Throws(SQLException::class)
    fun executeBatchInsert(
        sourceResultSets: Iterable<ResultSet>,
        sinkConnection: Connection,
        sinkTable: String,
        dryRun: Boolean = false
    ): Int {
        if (!sourceResultSets.iterator().hasNext()) {
            return 0
        }

        sinkConnection.autoCommit = false

        var rows = 0
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
                                else -> stmt.setString(k, Strings.removeNonPrintableChar(rs.getString(k)))
                            }
                        }

                        ++rows
                        stmt.addBatch()
                    } catch (e: SQLException) {
                        logger.warn("Failed to create stmt {}", e.message)
                    }
                }
            }

            if (dryRun) {
                val sql = "insert" + stmt.toString().substringAfter("insert") + ";"
                ResultSetUtils.sqlLog.info(sql)
            } else {
                affectedRows = stmt.executeBatch()
                sinkConnection.commit()
            }
        }

        return affectedRows?.sum() ?: 0
    }
}
