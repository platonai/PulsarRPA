package ai.platon.pulsar.client.examples

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.common.sql.SqlTemplate
import com.google.gson.Gson
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration
import java.time.Instant

data class ScrapeResultSet(
    val name: String,
    val records: List<Map<String, Any?>>? = null
)

data class ScrapeRequestV2(
    val authToken: String,
    val url: String,
    val args: String? = null,
    val sqls: Map<String, String> = mutableMapOf(),
    val callbackUrl: String? = null,
    var priority: String = Priority13.HIGHER2.name
)

data class ScrapeResponseV2(
    val status: String? = null,
    val pageStatus: String? = null,
    val pageContentBytes: Int = 0,
    val createdAt: String? = null,
    val timestamp: String = Instant.EPOCH.toString(),
    val uuid: String,
    val statusCode: Int? = null,
    val pageStatusCode: Int? = null,
    val version: String = "20210312",
    var resultSets: MutableList<ScrapeResultSet>? = mutableListOf()
)

data class ScrapeStatusRequest(
    val authToken: String,
    val uuid: String
)

class Scraper(val host: String, val authToken: String) {
    private val client = HttpClient.newHttpClient()
    private val baseUri = "http://$host:8182/api/x/a/v2"
    private val scrapeService = "$baseUri/q"
    private val statusService = "$baseUri/status"

    fun scrape(url: String, sqls: Map<String, String>) {
        val requestEntity: Any = ScrapeRequestV2(authToken, url, "-i 1s", sqls)
        val request = post(scrapeService, requestEntity)
        val uuid = client.send(request, BodyHandlers.ofString()).body()

        var responseEntity: ScrapeResponseV2? = null
        var i = 0
        while (i++ < 30 && responseEntity?.resultSets == null) {
            responseEntity = getStatus(uuid)
            println(Gson().toJson(responseEntity))
            sleepSeconds(5)
        }
    }

    private fun getStatus(uuid: String): ScrapeResponseV2? {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$statusService?uuid=$uuid&authToken=$authToken"))
            .timeout(Duration.ofMinutes(2))
            .GET()
            .build()
        val responseBody = client.send(request, BodyHandlers.ofString()).body()
        return Gson().fromJson(responseBody, ScrapeResponseV2::class.java)
    }

    private fun post(url: String, requestEntity: Any): HttpRequest {
        val requestBody = Gson().toJson(requestEntity)
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofMinutes(2))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(requestBody))
            .build()
    }
}

fun main() {
    val authToken = "rhlwTRBk-1-de14124c7ace3d93e38a705bae30376c"
    val productUrl = "https://www.amazon.com/dp/B000KETAF2"
    val resourcePrefix = "sql/crawl"
    val sqls = mapOf(
        "asin" to "x-asin.sql",
        "sims-1" to "x-asin-sims-consolidated-1.sql",
        "sims-2" to "x-asin-sims-consolidated-2.sql",
        "sims-3" to "x-asin-sims-consolidated-3.sql",
        "sims-consider" to "x-asin-sims-consider.sql",
        "similar-items" to "x-similar-items.sql",
        "reviews" to "x-asin-reviews.sql"
    ).entries
        .map { it.key to "$resourcePrefix/${it.value}" }
        .associate { it.first to SqlTemplate.load(it.second).template }

    val scraper = Scraper("crawl1", authToken)
    scraper.scrape(productUrl, sqls)
}
