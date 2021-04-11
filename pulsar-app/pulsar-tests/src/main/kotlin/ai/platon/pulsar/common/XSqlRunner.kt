package ai.platon.pulsar.common

import ai.platon.pulsar.common.sql.ResultSetFormatter
import ai.platon.pulsar.common.sql.SqlConverter
import ai.platon.pulsar.common.sql.SQLTemplate
import ai.platon.pulsar.dom.Documents
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.ql.SQLContext
import ai.platon.pulsar.ql.SQLContexts
import ai.platon.pulsar.ql.h2.utils.ResultSetUtils
import org.slf4j.LoggerFactory
import java.sql.ResultSet

class XSqlRunner(
    val cx: SQLContext = SQLContexts.activate()
) {
    private val log = LoggerFactory.getLogger(XSqlRunner::class.java)

    val loadArgs = "-i 1d -retry -nJitRetry 3"
    val extractor = VerboseSqlExtractor(cx)
    val session = extractor.session

    fun execute(url: String, sqlResource: String): ResultSet {
        val name = sqlResource.substringAfterLast("/").substringBeforeLast(".sql")
        val sqlTemplate = SQLTemplate.load(sqlResource, name = name)
        return execute(url, sqlTemplate)
    }

    fun execute(url: String, template: SQLTemplate): ResultSet {
        val document = loadResourceAsDocument(url) ?: session.loadDocument(url, loadArgs)

        val sql = template.createInstance(url)
        if (sql.isBlank()) {
            throw IllegalArgumentException("Illegal sql template: ${template.resource}")
        }

        var rs = extractor.query(sql, printResult = true)

        if (template.resource?.contains("x-similar-items.sql") == true) {
            rs = ResultSetUtils.transpose(rs)
            println("Transposed: ")
            rs.beforeFirst()
            println(ResultSetFormatter(rs, withHeader = true).toString())
        }

        val count = ResultSetUtils.count(rs)
        val path = session.export(document)
        log.info("Extracted $count records, exported://$path")

        return rs
    }

    fun executeAll(sqls: List<Pair<String, String>>) {
        var i = 0
        sqls.forEach { (url, resource) ->
            val name = resource.substringAfterLast("/").substringBeforeLast(".sql")
            val template = SQLTemplate.load(resource, name = name)

            when {
                template.template.isBlank() -> {
                    log.warn("Failed to load SQL template <{}>", resource)
                }
                template.template.contains("create table", ignoreCase = true) -> {
                    log.info(SqlConverter.createSql2extractSql(template.template))
                }
                else -> {
                    execute(url, template)
                }
            }
        }
    }

    private fun loadResourceAsDocument(url: String): FeaturedDocument? {
        val filename = AppPaths.fromUri(url, "", ".htm")
        val resource = "cache/web/export/$filename"

        if (ResourceLoader.exists(resource)) {
            return Documents.parse(ResourceLoader.readString(resource), url)
        }

        return null
    }
}
