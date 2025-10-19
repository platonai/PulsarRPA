package ai.platon.pulsar.util.server

import ai.platon.pulsar.common.printlnPro
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Test SSE with OkHttp for [ai.platon.pulsar.test.mock2.server.MockAICommandController].
 * */
fun main() {
    val baseUrl = "http://localhost:8182"
    val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Disable read timeout for SSE
        .build()

    // 1. Submit command
    val command = "Test command"
    val request = Request.Builder()
        .url("$baseUrl/mock/api/ai/command")
        .post(command.toRequestBody("text/plain".toMediaTypeOrNull()))
        .build()

    val uuid: String
    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) error("Unexpected code $response")
        val body = response.body?.string()?.trim() ?: error("No response body")
        printlnPro("Submit response: $body")
        // Parse uuid from ScrapeResponse JSON
        val regex = "\"uuid\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        uuid = regex.find(body)?.groupValues?.get(1) ?: error("No UUID in response")
    }
    printlnPro("Submitted, UUID: $uuid")

    // 2. Connect to SSE stream
    val sseRequest = Request.Builder()
        .url("$baseUrl/mock/api/ai/command/stream/$uuid")
        .build()

    client.newCall(sseRequest).execute().use { response ->
        if (!response.isSuccessful) error("Unexpected code $response")
        val source = response.body?.source() ?: error("No response body")
        while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            if (line.startsWith("data:")) {
                val json = line.removePrefix("data:").trim()
                printlnPro("SSE Event: $json")
                // Optionally, parse statusCode to determine completion
                val codeRegex = "\"statusCode\"\\s*:\\s*(\\d+)".toRegex()
                val code = codeRegex.find(json)?.groupValues?.get(1)?.toIntOrNull()
                if (code == 200 || code == 500) {
                    printlnPro("Stream finished with statusCode: $code")
                    break
                }
            }
        }
    }
}
