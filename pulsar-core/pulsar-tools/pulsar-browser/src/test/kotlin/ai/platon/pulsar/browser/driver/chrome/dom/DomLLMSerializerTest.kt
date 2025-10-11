package ai.platon.pulsar.browser.driver.chrome.dom

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class DomLLMSerializerTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `serialize filters attributes to whitelist`() {
        val child = SimplifiedNode(tag = "span", attributes = mapOf("data-test" to "value", "aria-label" to "ok"))
        val root = SimplifiedNode(
            tag = "div",
            attributes = mapOf("id" to "card", "data-id" to "123"),
            children = listOf(child)
        )

        val json = DomLLMSerializer.serialize(root, listOf("data-id", "aria-label"))
        val tree = mapper.readTree(json)

        val rootAttrs = tree.get("attributes")
        assertEquals(1, rootAttrs.size(), "Only whitelisted root attribute should be present")
        assertEquals("123", rootAttrs.get("data-id").asText())

        val childAttrs = tree.get("children").first().get("attributes")
        assertEquals(1, childAttrs.size(), "Child should also honor whitelist")
        assertEquals("ok", childAttrs.get("aria-label").asText())
        assertFalse(childAttrs.has("data-test"), "Non-whitelisted attribute must be omitted")
    }

    @Test
    fun `serialize merges shadow roots without duplication`() {
        val shared = SimplifiedNode(tag = "button", id = "submit")
        val root = SimplifiedNode(
            tag = "div",
            children = listOf(shared),
            shadowRoots = listOf(shared)
        )

        val json = DomLLMSerializer.serialize(root, emptyList())
        val tree = mapper.readTree(json)
        val children = tree.get("children")
        assertEquals(1, children.size(), "Shadow roots should be merged with children without duplicates")
        assertEquals("button", children.first().get("tag").asText())
    }
}
