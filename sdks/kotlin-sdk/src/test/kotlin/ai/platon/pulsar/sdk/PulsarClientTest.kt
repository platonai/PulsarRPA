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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for PulsarClient.
 * These tests verify client configuration and error handling.
 * Note: Integration tests with a real server are in a separate test class.
 */
class PulsarClientTest {

    @Test
    fun `PulsarClient can be created with default settings`() {
        val client = PulsarClient()
        assertNull(client.sessionId)
    }

    @Test
    fun `PulsarClient can be created with custom base URL`() {
        val client = PulsarClient(baseUrl = "http://custom-server:9999")
        assertNull(client.sessionId)
    }

    @Test
    fun `PulsarClient can be created with initial session ID`() {
        val client = PulsarClient(sessionId = "test-session-123")
        assertEquals("test-session-123", client.sessionId)
    }

    @Test
    fun `PulsarClient requires session for session-dependent operations`() {
        val client = PulsarClient()
        
        // post with {sessionId} placeholder should throw without session
        assertThrows<IllegalStateException> {
            client.post("/session/{sessionId}/url", mapOf("url" to "https://example.com"))
        }
    }

    @Test
    fun `PulsarClient session ID can be updated`() {
        val client = PulsarClient()
        assertNull(client.sessionId)
        
        client.sessionId = "new-session-id"
        assertEquals("new-session-id", client.sessionId)
    }

    @Test
    fun `PulsarClient close does not throw`() {
        val client = PulsarClient()
        client.close()
        // Should complete without exception
    }
}
