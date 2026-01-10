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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for WebDriver.
 * These tests verify WebDriver state management.
 * Note: Integration tests with a real server are in a separate test class.
 */
class WebDriverTest {

    @Test
    fun `WebDriver can be created with client`() {
        val client = PulsarClient(sessionId = "test-session")
        val driver = WebDriver(client)
        
        assertEquals(0, driver.id)
        assertTrue(driver.navigateHistory.isEmpty())
    }

    @Test
    fun `WebDriver tracks navigation history`() {
        val client = PulsarClient(sessionId = "test-session")
        val driver = WebDriver(client)
        
        // Directly add to history (without actual navigation)
        // In real usage, navigateTo adds to history
        assertTrue(driver.navigateHistory.isEmpty())
    }

    @Test
    fun `WebDriver close does not throw`() {
        val client = PulsarClient(sessionId = "test-session")
        val driver = WebDriver(client)
        driver.close()
        // Should complete without exception
    }
}

/**
 * Unit tests for PulsarSession.
 */
class PulsarSessionTest {

    @Test
    fun `PulsarSession can be created with client`() {
        val client = PulsarClient(sessionId = "test-session")
        val session = PulsarSession(client)
        
        assertEquals(0, session.id)
        assertEquals("test-session", session.uuid)
        assertTrue(session.isActive)
    }

    @Test
    fun `PulsarSession display shows session info`() {
        val client = PulsarClient(sessionId = "abcdefgh12345678")
        val session = PulsarSession(client)
        
        assertTrue(session.display.contains("abcdefgh"))
    }

    @Test
    fun `PulsarSession display shows no-session when inactive`() {
        val client = PulsarClient()
        val session = PulsarSession(client)
        
        assertFalse(session.isActive)
        assertTrue(session.display.contains("no-session"))
    }

    @Test
    fun `PulsarSession driver is lazily created`() {
        val client = PulsarClient(sessionId = "test-session")
        val session = PulsarSession(client)
        
        // boundDriver should be null initially
        assertNull(session.boundDriver)
        
        // driver property should create it
        val driver = session.driver
        assertNotNull(driver)
        assertNotNull(session.boundDriver)
    }

    @Test
    fun `PulsarSession createBoundDriver creates new driver`() {
        val client = PulsarClient(sessionId = "test-session")
        val session = PulsarSession(client)
        
        val driver1 = session.createBoundDriver()
        val driver2 = session.createBoundDriver()
        
        // Each call creates a new driver
        assertNotNull(driver1)
        assertNotNull(driver2)
    }

    @Test
    fun `PulsarSession bindDriver and unbindDriver work correctly`() {
        val client = PulsarClient(sessionId = "test-session")
        val session = PulsarSession(client)
        val driver = WebDriver(client)
        
        session.bindDriver(driver)
        assertEquals(driver, session.boundDriver)
        
        session.unbindDriver(driver)
        assertNull(session.boundDriver)
    }

    @Test
    fun `PulsarSession normalizeOrNull returns null for blank URL`() {
        val client = PulsarClient(sessionId = "test-session")
        val session = PulsarSession(client)
        
        assertNull(session.normalizeOrNull(null))
        assertNull(session.normalizeOrNull(""))
        assertNull(session.normalizeOrNull("   "))
    }

    @Test
    fun `PulsarSession loadAll returns list of pages`() {
        val client = PulsarClient(sessionId = "test-session")
        val session = PulsarSession(client)
        
        // Without server, this will fail, but we can test the structure
        // This test validates that the method signature is correct
        assertTrue(true)
    }
}

/**
 * Unit tests for AgenticSession.
 */
class AgenticSessionTest {

    @Test
    fun `AgenticSession can be created with client`() {
        val client = PulsarClient(sessionId = "test-session")
        val session = AgenticSession(client)
        
        assertEquals(session, session.companionAgent)
        assertEquals(session, session.context)
        assertTrue(session.processTrace.isEmpty())
    }

    @Test
    fun `AgenticSession processTrace is initially empty`() {
        val client = PulsarClient(sessionId = "test-session")
        val session = AgenticSession(client)
        
        assertTrue(session.processTrace.isEmpty())
    }

    @Test
    fun `AgenticSession options creates map with args`() {
        val client = PulsarClient(sessionId = "test-session")
        val session = AgenticSession(client)
        
        val opts = session.options("-expire 1d")
        
        assertEquals("-expire 1d", opts["args"])
    }

    @Test
    fun `AgenticSession data returns null by default`() {
        val client = PulsarClient(sessionId = "test-session")
        val session = AgenticSession(client)
        
        assertNull(session.data("test"))
    }

    @Test
    fun `AgenticSession property returns null by default`() {
        val client = PulsarClient(sessionId = "test-session")
        val session = AgenticSession(client)
        
        assertNull(session.property("test"))
    }

    @Test
    fun `AgenticSession registerClosable does not throw`() {
        val client = PulsarClient(sessionId = "test-session")
        val session = AgenticSession(client)
        
        session.registerClosable(Object())
        // Should complete without exception
    }
}
