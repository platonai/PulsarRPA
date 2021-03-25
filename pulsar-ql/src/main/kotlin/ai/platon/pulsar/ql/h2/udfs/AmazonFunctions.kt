package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.sites.amazon.AmazonSearcherJsEventHandler
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

    /**
     * @param url       The url to access
     * @param keyword   The keyword
     * @return          The AmazonSuggestion list
     */
    @UDFunction
    @JvmStatic
    fun suggestions(@H2Context conn: Connection, url: String, keyword: String): ResultSet {
        val session = H2SessionFactory.getSession(conn)

        val amazonSearcher = AmazonSearcherJsEventHandler(keyword)
        session.sessionConfig.putBean(amazonSearcher)
        session.load(url, "-i 0s")

        val rs = ResultSets.newSimpleResultSet("alias", "keyword", "isfb", "crid")
        amazonSearcher.suggestions.forEach {
            rs.addRow(it.alias, it.keyword, it.isfb, it.crid)
        }

        return rs
    }
}
