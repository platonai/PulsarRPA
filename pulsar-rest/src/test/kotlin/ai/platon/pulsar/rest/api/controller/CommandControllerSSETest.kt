package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.rest.api.entities.CommandRequest
import okhttp3.OkHttpClient
import okhttp3.Request
import org.joda.time.LocalTime
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.util.concurrent.TimeUnit
import kotlin.test.Ignore
import kotlin.test.assertNotNull

@Ignore("TimeConsumingTest")
@Tag("TimeConsumingTest")
class CommandControllerSSETest : ScrapeControllerTestBase() {

    /**
     * Test [CommandController.submitCommand]
     * Test [CommandController.streamEvents]
     * */
    @Test
    fun `Test submitCommand with pageSummaryPrompt + SSE`() {
        val pageType = "productDetailPage"
        val url = requireNotNull(urls[pageType])

        val request = CommandRequest(url,
            "",
            pageSummaryPrompt = "Summarize the product.",
            mode = "async",
        )

        val id = restTemplate.postForObject("$baseUri/api/commands", request, String::class.java)
        printlnPro("id: $id")
        assertNotNull(id)

        receiveSSE(id)
    }

    /**
     * Test [CommandController.submitCommand]
     * Test [CommandController.streamEvents]
     * */
    @Test
    fun `Test submitCommand with pageSummaryPrompt, dataExtractionRules + SSE`() {
        val pageType = "productDetailPage"
        val url = requireNotNull(urls[pageType])

        val request = CommandRequest(url,
            "",
            pageSummaryPrompt = "Summarize the product.",
            dataExtractionRules = "product name, ratings, price",
            mode = "async",
        )

        val id = restTemplate.postForObject("$baseUri/api/commands", request, String::class.java)
        printlnPro("id: $id")
        assertNotNull(id)

        receiveSSE(id)
    }

    private fun receiveSSE(id: String) {
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        // 2. Connect to SSE stream
        val sseRequest = Request.Builder()
            .url("$baseUri/api/commands/$id/stream")
            .build()

        client.newCall(sseRequest).execute().body?.charStream()?.use { inputStream ->
            BufferedReader(inputStream).lineSequence().forEach { line ->
                if (line.startsWith("data:")) {
                    val data = line.removePrefix("data:").trim()
                    printlnPro("[${LocalTime.now()}] $data")
                }
            }
        }
    }
}

