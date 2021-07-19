package ai.platon.pulsar.test

import ai.platon.pulsar.common.sql.ResultSetFormatter
import ai.platon.pulsar.common.sql.SQLConverter
import ai.platon.pulsar.common.sql.SQLInstance
import ai.platon.pulsar.ql.ResultSets
import ai.platon.pulsar.ql.context.SQLContext
import ai.platon.pulsar.ql.context.SQLContexts
import ai.platon.pulsar.ql.h2.utils.ResultSetUtils
import java.sql.ResultSet

open class VerboseSQLExecutor(
    val sqlContext: SQLContext = SQLContexts.activate()
): VerboseCrawler(sqlContext) {

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
            logger.warn("Failed to execute sql: \n{}", sql)
            e.printStackTrace()
        }

        return ResultSets.newSimpleResultSet()
    }

    fun execute(url: String, sqlResource: String): ResultSet {
        val name = sqlResource.substringAfterLast("/").substringBeforeLast(".sql")
        val sqlTemplate = SQLInstance.load(url, sqlResource, name = name)
        return execute(sqlTemplate)
    }

    fun execute(sqlInstance: SQLInstance): ResultSet {
        val url = sqlInstance.url
        val document = session.loadDocument(url)

        val sql = sqlInstance.sql
        if (sql.isBlank()) {
            throw IllegalArgumentException("Illegal sql template: ${sqlInstance.template.resource}")
        }

        var rs = query(sql, printResult = true)

        if (sqlInstance.template.resource?.contains("x-similar-items.sql") == true) {
            rs = ResultSetUtils.transpose(rs)
            println("Transposed: ")
            rs.beforeFirst()
            println(ResultSetFormatter(rs, withHeader = true).toString())
        }

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
        var i = 0
        val sqlInstances = sqls.map { (url, resource) ->
            val name = resource.substringAfterLast("/").substringBeforeLast(".sql")
            SQLInstance.load(url, resource, name = name)
        }
        executeAll(sqlInstances)
    }
}
