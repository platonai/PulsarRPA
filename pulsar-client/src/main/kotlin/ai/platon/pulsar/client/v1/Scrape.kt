package ai.platon.pulsar.client.v1

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

data class ScrapeRequest(
    val authToken: String,
    val sql: String,
    val callbackUrl: String? = null,
    val priority: String = Priority13.HIGHER2.name,
)

data class ScrapeResponse(
    val status: String? = null,
    val pageStatus: String? = null,
    val pageContentBytes: Int = 0,
    val createdAt: String? = null,
    val timestamp: String = Instant.EPOCH.toString(),
    val uuid: String,
    val statusCode: Int? = null,
    val pageStatusCode: Int? = null,
    val version: String = "",
    var resultSet: List<Map<String, Any?>>? = null,
)

class Scraper(val host: String, val authToken: String) {
    private val client = HttpClient.newHttpClient()
    private val baseUri = "http://$host:8182/api/x/a"
    private val scrapeService = "$baseUri/q"
    private val statusService = "$baseUri/status"

    fun scrape(sql: String): String {
        val requestEntity: Any = ScrapeRequest(authToken, sql, priority = "HIGHER5")
        println(jacksonObjectMapper().writeValueAsString(requestEntity))

        val request = post(scrapeService, requestEntity)
        return client.send(request, BodyHandlers.ofString()).body()
    }

    fun await(uuid: String) {
        var responseEntity: ScrapeResponse? = null
        var i = 0
        while (i++ < 30 && responseEntity?.resultSet == null) {
            responseEntity = getStatus(uuid)
            println(jacksonObjectMapper().writeValueAsString(responseEntity))
            sleepSeconds(5)
        }
    }

    private fun getStatus(uuid: String): ScrapeResponse? {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$statusService?uuid=$uuid&authToken=$authToken"))
            .timeout(Duration.ofMinutes(2))
            .GET()
            .build()
        println(request.uri())
        val responseBody = client.send(request, BodyHandlers.ofString()).body()
        return Gson().fromJson(responseBody, ScrapeResponse::class.java)
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
