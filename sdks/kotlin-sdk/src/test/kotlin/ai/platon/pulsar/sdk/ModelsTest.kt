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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for the SDK data models.
 * These tests verify model creation and fromMap methods.
 */
class ModelsTest {

    @Test
    fun `WebPage fromMap creates instance correctly`() {
        val data = mapOf(
            "url" to "https://example.com",
            "location" to "https://example.com/page",
            "contentType" to "text/html",
            "contentLength" to 1024,
            "protocolStatus" to "200",
            "isNil" to false,
            "html" to "<html></html>"
        )

        val page = WebPage.fromMap(data)

        assertEquals("https://example.com", page.url)
        assertEquals("https://example.com/page", page.location)
        assertEquals("text/html", page.contentType)
        assertEquals(1024, page.contentLength)
        assertEquals("200", page.protocolStatus)
        assertFalse(page.isNil)
        assertEquals("<html></html>", page.html)
    }

    @Test
    fun `WebPage fromMap handles missing optional fields`() {
        val data = mapOf<String, Any?>(
            "url" to "https://example.com"
        )

        val page = WebPage.fromMap(data)

        assertEquals("https://example.com", page.url)
        assertNull(page.location)
        assertNull(page.contentType)
        assertEquals(0, page.contentLength)
        assertFalse(page.isNil)
    }

    @Test
    fun `NormURL fromMap creates instance correctly`() {
        val data = mapOf(
            "spec" to "https://example.com -expire 1d",
            "url" to "https://example.com",
            "args" to "-expire 1d",
            "isNil" to false
        )

        val normUrl = NormURL.fromMap(data)

        assertEquals("https://example.com -expire 1d", normUrl.spec)
        assertEquals("https://example.com", normUrl.url)
        assertEquals("-expire 1d", normUrl.args)
        assertFalse(normUrl.isNil)
    }

    @Test
    fun `AgentRunResult fromMap creates instance correctly`() {
        val data = mapOf(
            "success" to true,
            "message" to "Task completed",
            "historySize" to 5,
            "processTraceSize" to 10,
            "finalResult" to mapOf("key" to "value"),
            "trace" to listOf("action1", "action2")
        )

        val result = AgentRunResult.fromMap(data)

        assertTrue(result.success)
        assertEquals("Task completed", result.message)
        assertEquals(5, result.historySize)
        assertEquals(10, result.processTraceSize)
        assertEquals(mapOf("key" to "value"), result.finalResult)
        assertEquals(listOf("action1", "action2"), result.trace)
    }

    @Test
    fun `AgentActResult fromMap creates instance correctly`() {
        val data = mapOf(
            "success" to true,
            "message" to "Action executed",
            "action" to "click button",
            "isComplete" to true,
            "expression" to "click('button')"
        )

        val result = AgentActResult.fromMap(data)

        assertTrue(result.success)
        assertEquals("Action executed", result.message)
        assertEquals("click button", result.action)
        assertTrue(result.isComplete)
        assertEquals("click('button')", result.expression)
    }

    @Test
    fun `ObserveResult fromMap creates instance correctly`() {
        val data = mapOf(
            "locator" to "0,123",
            "domain" to "driver",
            "method" to "click",
            "arguments" to mapOf("selector" to "button"),
            "description" to "Click the submit button",
            "nextGoal" to "Submit the form",
            "thinking" to "I should click the button"
        )

        val result = ObserveResult.fromMap(data)

        assertEquals("0,123", result.locator)
        assertEquals("driver", result.domain)
        assertEquals("click", result.method)
        assertEquals(mapOf("selector" to "button"), result.arguments)
        assertEquals("Click the submit button", result.description)
        assertEquals("Submit the form", result.nextGoal)
        assertEquals("I should click the button", result.thinking)
    }

    @Test
    fun `AgentObservation fromAny handles list correctly`() {
        val data = listOf(
            mapOf(
                "locator" to "0,1",
                "description" to "Button 1"
            ),
            mapOf(
                "locator" to "0,2",
                "description" to "Button 2"
            )
        )

        val observation = AgentObservation.fromAny(data)

        assertEquals(2, observation.observations.size)
        assertEquals("0,1", observation.observations[0].locator)
        assertEquals("Button 1", observation.observations[0].description)
        assertEquals("0,2", observation.observations[1].locator)
        assertEquals("Button 2", observation.observations[1].description)
    }

    @Test
    fun `AgentObservation fromAny handles null correctly`() {
        val observation = AgentObservation.fromAny(null)
        assertTrue(observation.observations.isEmpty())
    }

    @Test
    fun `ExtractionResult fromMap creates instance correctly`() {
        val data = mapOf(
            "success" to true,
            "message" to "Extraction successful",
            "data" to mapOf("title" to "Hello World")
        )

        val result = ExtractionResult.fromMap(data)

        assertTrue(result.success)
        assertEquals("Extraction successful", result.message)
        assertEquals(mapOf("title" to "Hello World"), result.data)
    }

    @Test
    fun `ToolCallResult fromMap creates instance correctly`() {
        val data = mapOf(
            "success" to true,
            "message" to "Tool executed",
            "data" to "result value"
        )

        val result = ToolCallResult.fromMap(data)

        assertTrue(result.success)
        assertEquals("Tool executed", result.message)
        assertEquals("result value", result.data)
    }

    @Test
    fun `PageSnapshot creation works correctly`() {
        val snapshot = PageSnapshot(
            url = "https://example.com",
            html = "<html><body>Hello</body></html>"
        )

        assertEquals("https://example.com", snapshot.url)
        assertEquals("<html><body>Hello</body></html>", snapshot.html)
    }

    @Test
    fun `ElementRef creation works correctly`() {
        val ref = ElementRef(elementId = "element-123")
        assertEquals("element-123", ref.elementId)
    }

    @Test
    fun `FieldsExtraction default is empty`() {
        val extraction = FieldsExtraction()
        assertTrue(extraction.fields.isEmpty())
    }

    @Test
    fun `ActionDescription creation works correctly`() {
        val action = ActionDescription(
            description = "Click the button",
            parameters = mapOf("selector" to "button.submit")
        )

        assertEquals("Click the button", action.description)
        assertEquals(mapOf("selector" to "button.submit"), action.parameters)
    }

    @Test
    fun `PageEventHandlers returns empty maps by default`() {
        val handlers = PageEventHandlers()

        assertTrue(handlers.getBrowseEventHandlers().isEmpty())
        assertTrue(handlers.getLoadEventHandlers().isEmpty())
        assertTrue(handlers.getCrawlEventHandlers().isEmpty())
    }
}
