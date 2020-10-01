package ai.platon.pulsar.rest.api.resources

import ai.platon.pulsar.common.url.Urls
import ai.platon.pulsar.common.config.CapabilityTypes.INDEXER_URL
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import org.apache.http.impl.client.SystemDefaultHttpClient
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocument
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.IOException
import java.util.*

@RestController
@RequestMapping("/solr")
class SolrResource(
        @Autowired private val conf: ImmutableConfig
) {
    private val solrClient: SolrClient
    private val rows = 100
    private val sort = "publish_time desc"
    private val fields = "article_title,encoding,resource_category,author,director,last_crawl_time,publish_time,domain,url"

    init {
        val indexerUrl = conf.get(INDEXER_URL)
        // solrClient = SolrUtils.getSolrClient(indexerUrl);
        solrClient = HttpSolrClient.Builder(INDEXER_URL)
                .withHttpClient(HTTP_CLIENT)
                .build()
    }

    @GetMapping("/get")
    @Throws(IOException::class, SolrServerException::class)
    operator fun get(@PathVariable("key") key: String): String {
        val doc = solrClient.getById(Urls.reverseUrlOrEmpty(key))
        return format(doc)
    }

    @GetMapping("/p24h")
    @Throws(IOException::class, SolrServerException::class)
    fun p24h(): ArrayList<SolrDocument> {
        val response = runQuery(Params.of("publish_time", "[NOW-DAY/DAY TO NOW]"))
        return response.results
    }

    @GetMapping("/p48h")
    @Throws(IOException::class, SolrServerException::class)
    fun p48h(): ArrayList<SolrDocument> {
        val response = runQuery(Params.of("publish_time", "[NOW-2DAY/DAY TO NOW]"))
        return response.results
    }

    @GetMapping("/c24h")
    @Throws(IOException::class, SolrServerException::class)
    fun c24h(): ArrayList<SolrDocument> {
        val response = runQuery(Params.of("last_crawl_time", "[NOW-DAY/DAY TO NOW]"))
        return response.results
    }

    @GetMapping("/c48h")
    @Throws(IOException::class, SolrServerException::class)
    fun c48h(): ArrayList<SolrDocument> {
        val response = runQuery(Params.of("last_crawl_time", "[NOW-2DAY/DAY TO NOW]"))
        return response.results
    }

    @GetMapping("/truncate")
    @Throws(IOException::class, SolrServerException::class)
    fun truncate(@PathVariable("collection") collection: String): Boolean {
        solrClient.deleteByQuery(collection, "*:*")
        return true
    }

    private fun format(document: SolrDocument): String {
        return document.entries.joinToString(",\n") { it.key + ":\t" + it.value }
    }

    @Throws(IOException::class, SolrServerException::class)
    private fun runQuery(queryParams: Params): QueryResponse {
        val q = queryParams.asMap().entries.joinToString(" AND ") { it.key + ":" + it.value }
        val query = newSolrQuery().setQuery(q)
        return solrClient.query(query)
    }

    private fun newSolrQuery(): SolrQuery {
        val solrQuery = SolrQuery()
        solrQuery
                .setParam("q", "publish_time:[NOW-1DAY/DAY TO NOW] AND article_title:[\"\" TO *]")
                .setParam("fl", fields)
                .setParam("sort", sort)
                .setParam("rows", rows.toString())
                .setParam("TZ", "Asia/Shanghai")
                .setParam("indent", "on")
                .setParam("wt", "json")
        return solrQuery
    }

    companion object {
        private val HTTP_CLIENT = SystemDefaultHttpClient()
    }
}
