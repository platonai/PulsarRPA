package ai.platon.pulsar.common

import ai.platon.pulsar.common.sql.ResultSetFormatter
import ai.platon.pulsar.context.PulsarContext
import ai.platon.pulsar.ql.ResultSets
import ai.platon.pulsar.ql.h2.H2MemoryDb
import ai.platon.pulsar.ql.h2.SqlUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.sql.Connection
import java.sql.ResultSet
import java.util.concurrent.ArrayBlockingQueue

/**
 * The base class for all tests.
 */
open class VerboseSqlExtractor(context: PulsarContext): VerboseCrawler(context) {

    val connection = H2MemoryDb().getRandomConnection()
    val stat = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)
    val connectionPool = ArrayBlockingQueue<Connection>(1000)
    val randomConnection get() = H2MemoryDb().getRandomConnection()

    fun allocateDbConnections(concurrent: Int) {
        runBlocking {
            repeat(concurrent) { launch { connectionPool.add(randomConnection) } }
        }
    }

    fun execute(sql: String, printResult: Boolean = true, formatAsList: Boolean = false) {
        try {
            val regex = "^(SELECT|CALL).+".toRegex()
            if (sql.toUpperCase().filter { it != '\n' }.trimIndent().matches(regex)) {
                val rs = stat.executeQuery(sql)
                if (printResult) {
                    println(ResultSetFormatter(rs, asList = formatAsList))
                }
            } else {
                val r = stat.execute(sql)
                if (printResult) {
                    println(r)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun query(sql: String, printResult: Boolean = true, withHeader: Boolean = true): ResultSet {
        try {
            val rs = stat.executeQuery(sql)

            rs.beforeFirst()
            val count = SqlUtils.count(rs)

            if (printResult) {
                rs.beforeFirst()
                println(ResultSetFormatter(rs, withHeader = withHeader, asList = (count == 1)))
            }

            return rs
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return ResultSets.newSimpleResultSet()
    }

    override fun close() {
        connectionPool.forEach { it.runCatching { it.close() }.onFailure { log.warn(it.message) } }
        super.close()
    }
}
