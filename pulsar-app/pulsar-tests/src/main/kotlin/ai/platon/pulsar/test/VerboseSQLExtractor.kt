package ai.platon.pulsar.test

import ai.platon.pulsar.common.sql.ResultSetFormatter
import ai.platon.pulsar.ql.ResultSets
import ai.platon.pulsar.ql.context.SQLContext
import ai.platon.pulsar.ql.h2.utils.ResultSetUtils
import java.sql.ResultSet

open class VerboseSQLExtractor(val sqlContext: SQLContext): VerboseCrawler(sqlContext) {

    fun execute(sql: String, printResult: Boolean = true, formatAsList: Boolean = false) {
        if (sql.isBlank()) {
            throw IllegalArgumentException("Illegal sql: $sql")
        }

        try {
            val regex = "^(SELECT|CALL).+".toRegex()
            if (sql.toUpperCase().filter { it != '\n' }.trimIndent().matches(regex)) {
                val rs = sqlContext.executeQuery(sql)
                if (printResult) {
                    println(ResultSetFormatter(rs, asList = formatAsList))
                }
            } else {
                val r = sqlContext.execute(sql)
                if (printResult) {
                    println(r)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun query(sql: String, printResult: Boolean = true, withHeader: Boolean = true): ResultSet {
        if (sql.isBlank()) {
            throw IllegalArgumentException("Illegal sql: $sql")
        }

        try {
            val rs = sqlContext.executeQuery(sql)

            rs.beforeFirst()
            val count = ResultSetUtils.count(rs)

            if (printResult) {
                rs.beforeFirst()
                println(ResultSetFormatter(rs, withHeader = withHeader, asList = (count == 1)))
            }

            return rs
        } catch (e: Throwable) {
            log.warn("Failed to execute sql: \n{}", sql)
            e.printStackTrace()
        }

        return ResultSets.newSimpleResultSet()
    }
}
