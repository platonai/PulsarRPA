package ai.platon.pulsar.client

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.sleepSeconds
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
    val records: List<Map<String, Any?>>? = null,
)

data class ScrapeRequestV2(
    val authToken: String,
    val url: String,
    val args: String? = null,
    val sqls: Map<String, String> = mutableMapOf(),
    val callbackUrl: String? = null,
    val priority: String = Priority13.HIGHER2.name,
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
    var resultSets: MutableList<ScrapeResultSet>? = mutableListOf(),
)

class Scraper(val host: String, val authToken: String) {
    private val client = HttpClient.newHttpClient()
    private val baseUri = "http://$host:8182/api/x/a/v2"
    private val scrapeService = "$baseUri/q"
    private val statusService = "$baseUri/status"

    fun scrape(url: String, sqls: Map<String, String>): String {
        val requestEntity: Any = ScrapeRequestV2(authToken, url, "-i 1s", sqls, priority = "HIGHER5")
        println(jacksonObjectMapper().writeValueAsString(requestEntity))

        val request = post(scrapeService, requestEntity)
        return client.send(request, BodyHandlers.ofString()).body()
    }

    fun await(uuid: String) {
        var responseEntity: ScrapeResponseV2? = null
        var i = 0
        while (i++ < 30 && responseEntity?.resultSets == null) {
            responseEntity = getStatus(uuid)
            println(jacksonObjectMapper().writeValueAsString(responseEntity))
            sleepSeconds(5)
        }
    }

    fun scrapeAll(urls: Collection<String>, sqls: Map<String, String>): List<String> {
        return urls.map { scrape(it, sqls) }
    }

    fun awaitAll(uuids: Collection<String>) {
        val responses = mutableMapOf<String, ScrapeResponseV2?>()
        val finishedUUIDs = mutableListOf<String>()

        var i = 0
        while (i++ < 1000 && finishedUUIDs.size < uuids.size) {
            uuids.forEach { uuid ->
                if (responses[uuid]?.resultSets == null) {
                    val response = getStatus(uuid)
                    responses[uuid] = response
                    if (response?.resultSets != null) {
                        finishedUUIDs.add(uuid)
                        println(jacksonObjectMapper().writeValueAsString(response))
                    }
                }
            }
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
