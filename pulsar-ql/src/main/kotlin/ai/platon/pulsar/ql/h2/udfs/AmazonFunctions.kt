package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.sites.amazon.AmazonSearcherJsEventHandler
import ai.platon.pulsar.common.urls.sites.amazon.AmazonUrls
import ai.platon.pulsar.crawl.DefaultPulsarEventHandler
import ai.platon.pulsar.ql.ResultSets
import ai.platon.pulsar.ql.annotation.H2Context
import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.annotation.UDFunction
import ai.platon.pulsar.ql.h2.H2SessionFactory
import java.sql.Connection
import java.sql.ResultSet

@Suppress("unused")
@UDFGroup(namespace = "AMAZON")
object AmazonFunctions {

    @UDFunction
    @JvmStatic
    fun findAsin(@H2Context conn: Connection, url: String): String {
        return AmazonUrls.findAsin(url) ?: ""
    }

    @UDFunction
    @JvmStatic
    fun findAsinOrNull(@H2Context conn: Connection, url: String): String? {
        return AmazonUrls.findAsin(url)
    }

    /**
     * @param conn      The auto injected jdbc connection by h2 engine
     * @param url       The url to access
     * @param keyword   The keyword
     * @return          The AmazonSuggestion list
     */
    @UDFunction
    @JvmStatic
    fun suggestions(@H2Context conn: Connection, url: String, keyword: String): ResultSet {
        val session = H2SessionFactory.getSession(conn)

        val amazonSearcher = AmazonSearcherJsEventHandler(keyword)
        val options = session.options("-i 0s")
        options.eventHandler = DefaultPulsarEventHandler().also {
            it.simulateEventHandler.onBeforeComputeFeature.addLast(amazonSearcher)
        }
        session.load(url, options)

        val rs = ResultSets.newSimpleResultSet("alias", "keyword", "isfb", "crid")
        amazonSearcher.suggestions.forEach {
            rs.addRow(it.alias, it.keyword, it.isfb, it.crid)
        }

        return rs
    }
}
