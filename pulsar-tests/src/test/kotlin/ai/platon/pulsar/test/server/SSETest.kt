package ai.platon.pulsar.test.server

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Test SSE with OkHttp for [MockAICommandController].
 * */
fun main() {
    val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Disable read timeout for SSE
        .build()

    // 1. Submit command
    val command = "Test command"
    val request = Request.Builder()
        .url("http://localhost:8182/mock/api/ai/command")
        .post(command.toRequestBody("text/plain".toMediaTypeOrNull()))
        .build()

    val uuid = client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) error("Unexpected code $response")
        response.body?.string()?.trim() ?: error("No UUID returned")
    }
    println("Submitted, UUID: $uuid")

    // 2. Connect to SSE stream
    val sseRequest = Request.Builder()
        .url("http://localhost:8182/mock/api/ai/command/stream/$uuid")
        .build()

    client.newCall(sseRequest).execute().use { response ->
        if (!response.isSuccessful) error("Unexpected code $response")
        val source = response.body?.source() ?: error("No response body")
        while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            if (line.startsWith("data:")) {
                println("SSE Event: ${line.removePrefix("data:").trim()}")
            }
        }
    }
}
