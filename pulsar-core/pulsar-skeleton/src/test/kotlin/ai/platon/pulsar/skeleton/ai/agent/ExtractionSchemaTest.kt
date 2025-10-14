package ai.platon.pulsar.skeleton.ai.agent

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ExtractionSchemaTest {

    private val mapper = ObjectMapper()

    @Test
    fun `simple string field produces required and description`() {
        val schema = ExtractionSchema(listOf(
            ExtractionField.string("title", description = "The title", required = true)
        ))
        val json = schema.toJsonSchema()
        val root = mapper.readTree(json)

        assertEquals("object", root.get("type").asText())
        val props = root.get("properties")
        assertTrue(props.has("title"))
        assertEquals("string", props.get("title").get("type").asText())
        assertEquals("The title", props.get("title").get("description").asText())
        val required = root.get("required")
        assertNotNull(required)
        assertTrue(required.any { it.asText() == "title" })
    }

    @Test
    fun `object with nested array renders children and item schema`() {
        val item = ExtractionField.obj(
            name = "item",
            properties = listOf(
                ExtractionField.string("name"),
                ExtractionField.string("sku", required = false)
            ),
            required = false
        )
        val field = ExtractionField.obj(
            name = "product",
            properties = listOf(
                ExtractionField.arrayOf("variants", item = item)
            )
        )
        val schema = ExtractionSchema(listOf(field))
        val root = mapper.readTree(schema.toJsonSchema())

        val product = root.get("properties").get("product")
        assertEquals("object", product.get("type").asText())
        val variants = product.get("properties").get("variants")
        assertEquals("array", variants.get("type").asText())
        val items = variants.get("items")
        assertEquals("object", items.get("type").asText())
        val itemProps = items.get("properties")
        assertTrue(itemProps.has("name"))
        assertTrue(itemProps.has("sku"))
        // child required list should include 'name' only
        val childReq = items.get("required")
        assertNotNull(childReq)
        assertTrue(childReq.any { it.asText() == "name" })
        assertFalse(childReq.any { it.asText() == "sku" })
    }

    @Test
    fun `array without items falls back to string item type`() {
        val field = ExtractionField(
            name = "tags",
            type = "array",
            items = null
        )
        val schema = ExtractionSchema(listOf(field))
        val root = mapper.readTree(schema.toJsonSchema())
        val tags = root.get("properties").get("tags")
        assertEquals("array", tags.get("type").asText())
        assertEquals("string", tags.get("items").get("type").asText())
    }

    @Test
    fun `legacy map adapter marks fields optional and sets descriptions`() {
        val legacy = mapOf(
            "title" to "Title text",
            "price" to "Price number"
        )
        val schema = legacyMapToExtractionSchema(legacy)
        val root = mapper.readTree(schema.toJsonSchema())

        val props = root.get("properties")
        assertEquals("Title text", props.get("title").get("description").asText())
        assertEquals("Price number", props.get("price").get("description").asText())
        // no required array expected since legacy fields are optional by default
        assertFalse(root.has("required"))
    }
}

