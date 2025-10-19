package ai.platon.pulsar.test

import ai.platon.pulsar.common.sql.ResultSetFormatter
import ai.platon.pulsar.common.sql.SQLConverter
import ai.platon.pulsar.common.sql.SQLInstance
import ai.platon.pulsar.ql.common.ResultSets
import ai.platon.pulsar.ql.context.SQLContext
import ai.platon.pulsar.ql.context.SQLContexts
import ai.platon.pulsar.ql.h2.utils.ResultSetUtils
import java.sql.ResultSet
import java.util.*

open class VerboseSQLExecutor(
    val context: SQLContext = SQLContexts.create(),
) : VerboseCrawler(context) {

    fun execute(sql: String, printResult: Boolean = true, withHeader: Boolean = true, formatAsList: Boolean = false) {
        if (sql.isBlank()) {
            throw IllegalArgumentException("Illegal sql: $sql")
        }

        try {
            val regex = "^(SELECT|CALL).+".toRegex()
            if (sql.uppercase(Locale.getDefault()).filter { it != '\n' }.trimIndent().matches(regex)) {
                executeQuery(sql, printResult, withHeader, formatAsList)
            } else {
                val r = context.execute(sql)
                if (printResult) {
                    println(r)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun executeQuery(
        sql: String,
        printResult: Boolean = true,
        withHeader: Boolean = true,
        formatAsList: Boolean = false,
    ): ResultSet {
        if (sql.isBlank()) {
            throw IllegalArgumentException("Illegal sql: $sql")
        }

        try {
            val rs = context.executeQuery(sql)

            if (printResult) {
                val count = ResultSetUtils.count(rs)
                println(ResultSetFormatter(rs, withHeader = withHeader, asList = (count == 1)))
            }

            rs.beforeFirst()
            return rs
        } catch (e: Throwable) {
            logger.warn("Failed to execute sql: \n{}", sql)
            e.printStackTrace()
        }

        return ResultSets.newSimpleResultSet()
    }

    fun queryAll(sqls: Iterable<String>, printResult: Boolean = true): Map<String, ResultSet> {
        val resultSets = mutableMapOf<String, ResultSet>()
        sqls.forEach { sql ->
            val reviewRs = executeQuery(sql, printResult)
            resultSets[sql] = reviewRs
        }

        return resultSets
    }

    fun execute(url: String, sqlResource: String): ResultSet {
        val name = sqlResource.substringAfterLast("/").substringBeforeLast(".sql")
        val sqlTemplate = SQLInstance.load(url, sqlResource, name = name)
        return execute(sqlTemplate)
    }

    fun execute(sqlInstance: SQLInstance): ResultSet {
        val sql = sqlInstance.sql
        if (sql.isBlank()) {
            throw IllegalArgumentException("Illegal sql template: ${sqlInstance.template.resource}")
        }

        val rs = executeQuery(sql, printResult = true)

        val count = ResultSetUtils.count(rs)
        logger.info("Extracted $count records")

        rs.beforeFirst()
        return rs
    }

    fun executeAll(sqls: Iterable<SQLInstance>) {
        var i = 0
        sqls.forEach { sqlInstance ->
            when {
                sqlInstance.template.template.isBlank() -> {
                    logger.warn("Failed to load SQL template <{}>", sqlInstance)
                }
                sqlInstance.template.template.contains("create table", ignoreCase = true) -> {
                    logger.info(SQLConverter.createSQL2extractSQL(sqlInstance.template.template))
                }
                else -> {
                    execute(sqlInstance)
                }
            }
        }
    }

    fun executeAll(sqls: List<Pair<String, String>>) {
        val sqlInstances = sqls.map { (url, resource) ->
            val name = resource.substringAfterLast("/").substringBeforeLast(".sql")
            SQLInstance.load(url, resource, name = name)
        }
        executeAll(sqlInstances)
    }
}
