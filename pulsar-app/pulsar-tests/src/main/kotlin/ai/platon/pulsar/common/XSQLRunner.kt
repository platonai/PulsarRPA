package ai.platon.pulsar.common

import ai.platon.pulsar.common.sql.ResultSetFormatter
import ai.platon.pulsar.common.sql.SQLConverter
import ai.platon.pulsar.common.sql.SQLInstance
import ai.platon.pulsar.ql.SQLContext
import ai.platon.pulsar.ql.SQLContexts
import ai.platon.pulsar.ql.h2.utils.ResultSetUtils
import org.slf4j.LoggerFactory
import java.sql.ResultSet

class XSQLRunner(
    val cx: SQLContext = SQLContexts.activate(),
) {
    private val log = LoggerFactory.getLogger(XSQLRunner::class.java)

    val loadArgs = "-retry -nJitRetry 3"
    val extractor = VerboseSQLExtractor(cx)
    val session = extractor.session

    fun execute(url: String, sqlResource: String): ResultSet {
        val name = sqlResource.substringAfterLast("/").substringBeforeLast(".sql")
        val sqlTemplate = SQLInstance.load(url, sqlResource, name = name)
        return execute(sqlTemplate)
    }

    fun execute(sqlInstance: SQLInstance): ResultSet {
        val url = sqlInstance.url
        val document = session.loadDocument(url, loadArgs)

        val sql = sqlInstance.sql
        if (sql.isBlank()) {
            throw IllegalArgumentException("Illegal sql template: ${sqlInstance.template.resource}")
        }

        var rs = extractor.query(sql, printResult = true)

        if (sqlInstance.template.resource?.contains("x-similar-items.sql") == true) {
            rs = ResultSetUtils.transpose(rs)
            println("Transposed: ")
            rs.beforeFirst()
            println(ResultSetFormatter(rs, withHeader = true).toString())
        }

        val count = ResultSetUtils.count(rs)
        log.info("Extracted $count records")

        return rs
    }

    fun executeAll(sqls: List<Pair<String, String>>) {
        var i = 0
        sqls.forEach { (url, resource) ->
            val name = resource.substringAfterLast("/").substringBeforeLast(".sql")
            val sqlInstance = SQLInstance.load(url, resource, name = name)

            when {
                sqlInstance.template.template.isBlank() -> {
                    log.warn("Failed to load SQL template <{}>", resource)
                }
                sqlInstance.template.template.contains("create table", ignoreCase = true) -> {
                    log.info(SQLConverter.createSQL2extractSQL(sqlInstance.template.template))
                }
                else -> {
                    execute(sqlInstance)
                }
            }
        }
    }
}
