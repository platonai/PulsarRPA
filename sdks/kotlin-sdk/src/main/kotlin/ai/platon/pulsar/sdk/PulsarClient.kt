/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor
 * license agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The ASF licenses this file to
 * you under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required
 * by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package ai.platon.pulsar.sdk

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Thin HTTP client over the Browser4 OpenAPI.
 *
 * Provides low-level API communication with the Browser4 server,
 * handling session management and request/response serialization.
 *
 * Example usage:
 * ```kotlin
 * val client = PulsarClient()
 * val sessionId = client.createSession()
 * // Use client for API calls
 * client.deleteSession()
 * client.close()
 * ```
 *
 * @param baseUrl The base URL of the Browser4 server (default: http://localhost:8182)
 * @param timeout Request timeout in seconds (default: 30.0)
 * @param sessionId Optional initial session ID
 * @param defaultHeaders Optional additional default headers
 */
class PulsarClient(
    private val baseUrl: String = "http://localhost:8182",
    private val timeout: Duration = Duration.ofSeconds(30),
    var sessionId: String? = null,
    private val defaultHeaders: Map<String, String> = emptyMap()
) : AutoCloseable {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(timeout)
        .build()

    private val gson = Gson()

    private val headers: Map<String, String> = buildMap {
        put("Content-Type", "application/json")
        putAll(defaultHeaders)
    }

    private fun url(path: String): String {
        val normalizedBase = baseUrl.trimEnd('/')
        return "$normalizedBase$path"
    }

    private fun requireSession(sessionId: String? = null): String {
        val sid = sessionId ?: this.sessionId
        return sid ?: throw IllegalStateException("session_id is required; call createSession() first or pass sessionId explicitly")
    }

    @Suppress("UNCHECKED_CAST")
    private fun request(
        method: String,
        path: String,
        sessionId: String? = null,
        body: Map<String, Any?>? = null
    ): Any? {
        var resolvedPath = path
        val sid = if ("{sessionId}" in path) {
            requireSession(sessionId)
        } else {
            sessionId ?: this.sessionId
        }

        if (sid != null) {
            resolvedPath = resolvedPath.replace("{sessionId}", sid)
        }

        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url(resolvedPath)))
            .timeout(timeout)

        headers.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }

        when (method.uppercase()) {
            "GET" -> requestBuilder.GET()
            "DELETE" -> requestBuilder.DELETE()
            "POST" -> {
                val jsonBody = if (body != null) gson.toJson(body) else "{}"
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            }
            "PUT" -> {
                val jsonBody = if (body != null) gson.toJson(body) else "{}"
                requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
            }
            else -> throw IllegalArgumentException("Unsupported HTTP method: $method")
        }

        val response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() >= 400) {
            throw RuntimeException("HTTP ${response.statusCode()}: ${response.body()}")
        }

        val responseBody = response.body()
        if (responseBody.isNullOrBlank()) {
            return null
        }

        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val payload: Map<String, Any?> = gson.fromJson(responseBody, type)
            // WebDriver responses typically wrap in { value: ... }
            if (payload.containsKey("value")) payload["value"] else payload
        } catch (e: Exception) {
            responseBody
        }
    }

    /**
     * Creates a new browser session.
     *
     * @param capabilities Optional desired capabilities for the session
     * @return The created session ID
     */
    @Suppress("UNCHECKED_CAST")
    fun createSession(capabilities: Map<String, Any?>? = null): String {
        val response = request("POST", "/session", body = mapOf("capabilities" to (capabilities ?: emptyMap<String, Any?>())))
        val value = response as? Map<String, Any?>
        val newSessionId = value?.get("sessionId") as? String
            ?: throw RuntimeException("createSession response missing sessionId")
        this.sessionId = newSessionId
        return newSessionId
    }

    /**
     * Deletes the current or specified session.
     *
     * @param sessionId Optional session ID to delete (defaults to current session)
     */
    fun deleteSession(sessionId: String? = null) {
        val sid = requireSession(sessionId)
        request("DELETE", "/session/$sid")
        if (sessionId == null || sessionId == this.sessionId) {
            this.sessionId = null
        }
    }

    /**
     * Performs a POST request to the API.
     *
     * @param path API endpoint path (may contain {sessionId} placeholder)
     * @param body Request body as a map
     * @param sessionId Optional session ID
     * @return Response value
     */
    fun post(path: String, body: Map<String, Any?>, sessionId: String? = null): Any? {
        return request("POST", path, sessionId = sessionId, body = body)
    }

    /**
     * Performs a GET request to the API.
     *
     * @param path API endpoint path (may contain {sessionId} placeholder)
     * @param sessionId Optional session ID
     * @return Response value
     */
    fun get(path: String, sessionId: String? = null): Any? {
        return request("GET", path, sessionId = sessionId)
    }

    /**
     * Performs a DELETE request to the API.
     *
     * @param path API endpoint path (may contain {sessionId} placeholder)
     * @param sessionId Optional session ID
     * @return Response value
     */
    fun delete(path: String, sessionId: String? = null): Any? {
        return request("DELETE", path, sessionId = sessionId)
    }

    /**
     * Closes the HTTP client.
     */
    override fun close() {
        // HttpClient in Java 11+ doesn't require explicit closing,
        // but we implement AutoCloseable for consistency
    }
}
