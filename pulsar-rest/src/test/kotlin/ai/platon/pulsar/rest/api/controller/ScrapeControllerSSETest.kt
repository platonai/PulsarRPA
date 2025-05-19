package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.ql.h2.udfs.LLMFunctions
import okhttp3.OkHttpClient
import okhttp3.Request
import org.joda.time.LocalTime
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.util.concurrent.TimeUnit
import kotlin.test.assertNotNull

class ScrapeControllerSSETest : ScrapeControllerTestBase() {

    /**
     * Test [ScrapeController.submitJob]
     * Test [ScrapeController.streamResult]
     * Test [LLMFunctions.extract]
     * */
    @Test
    fun `Test scraping with LLM + X-SQL + SSE`() {
        val pageType = "productDetailPage"
        val url = requireNotNull(urls[pageType])
        val sql = requireNotNull(sqlTemplates[pageType]).createSQL(url)

        val id = restTemplate.postForObject("$baseUri/x/s", sql, String::class.java)
        println("id: $id")
        assertNotNull(id)

        receiveSSE(id)
    }

    private fun receiveSSE(id: String) {
        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS) // Disable read timeout for SSE
            .build()

        // 2. Connect to SSE stream
        val sseRequest = Request.Builder()
            .url("$baseUri/x/stream/$id")
            .build()

        client.newCall(sseRequest).execute().body?.charStream()?.use { inputStream ->
            BufferedReader(inputStream).lineSequence().forEach { line ->
                if (line.startsWith("data:")) {
                    val data = line.removePrefix("data:").trim()
                    println("[${LocalTime.now()}] $data")
                }
            }
        }
    }
}

