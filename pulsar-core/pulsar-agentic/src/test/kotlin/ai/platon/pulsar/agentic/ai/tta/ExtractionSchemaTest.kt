package ai.platon.pulsar.agentic.ai.tta

import ai.platon.pulsar.skeleton.ai.support.ExtractionField
import ai.platon.pulsar.skeleton.ai.support.ExtractionSchema
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import kotlin.test.assertEquals

class ExtractionSchemaTest {

    private val mapper = ObjectMapper()

    @Test
    fun `simple string field produces required and description`() {
        val schema = ExtractionSchema(
            listOf(
                ExtractionField.string("title", description = "The title", required = true)
            )
        )
        val json = schema.toJsonSchema()
        val root = mapper.readTree(json)

        Assertions.assertEquals("object", root.get("type").asText())
        val props = root.get("properties")
        assertTrue(props.has("title"))
        Assertions.assertEquals("string", props.get("title").get("type").asText())
        Assertions.assertEquals("The title", props.get("title").get("description").asText())
        val required = root.get("required")
        Assertions.assertNotNull(required)
        assertTrue(required.any { it.asText() == "title" })
    }

    @Test
    fun `object with nested array renders children and item schema`() {
        val item = ExtractionField.obj(
            name = "item",
            objectMemberProperties = listOf(
                ExtractionField.string("name"),
                ExtractionField.string("sku", required = false)
            ),
            required = false
        )
        val field = ExtractionField.obj(
            name = "product",
            objectMemberProperties = listOf(
                ExtractionField.arrayOf("variants", item = item)
            )
        )
        val schema = ExtractionSchema(listOf(field))
        val jsonSchema = schema.toJsonSchema()
        val root = mapper.readTree(jsonSchema)

        val product = root.get("properties").get("product")
        Assertions.assertEquals("object", product.get("type").asText())
        val variants = product.get("properties").get("variants")
        Assertions.assertEquals("array", variants.get("type").asText())
        val items = variants.get("items")
        Assertions.assertEquals("object", items.get("type").asText())
        val itemProps = items.get("properties")
        assertTrue(itemProps.has("name"))
        assertTrue(itemProps.has("sku"))
        // child required list should include 'name' only
        val childReq = items.get("required")
        Assertions.assertNotNull(childReq)
        assertTrue(childReq.any { it.asText() == "name" })
        assertFalse(childReq.any { it.asText() == "sku" })
    }

    @Test
    fun `array without items falls back to string item type`() {
        val field = ExtractionField(
            name = "tags",
            type = "array",
            arrayElements = null
        )
        val schema = ExtractionSchema(listOf(field))
        val root = mapper.readTree(schema.toJsonSchema())
        val tags = root.get("properties").get("tags")
        Assertions.assertEquals("array", tags.get("type").asText())
        Assertions.assertEquals("string", tags.get("items").get("type").asText())
    }

    @Test
    fun `When parse from JSON string Then success`() {
        val json = """
{"fields":[{"name":"articles","type":"array","description":"文章列表","arrayElements":{"name":"article","type":"object","objectMemberProperties":[{"name":"title","type":"string","description":"文章标题","required":true},{"name":"comments","type":"number","description":"评论数量","required":true}]}}]}

        """.trimIndent()

        val schema = ExtractionSchema.parse(json)
    }

    @Test
    fun `parse from JSON string`() {
        val json = """
            {
              "fields" : [ {
                "name" : "title",
                "type" : "string",
                "description" : "文章标题"
              }, {
                "name" : "comments",
                "type" : "integer",
                "description" : "评论数量，从评论链接中提取数字部分"
              } ]
            }
        """.trimIndent()

        val schema = ExtractionSchema.parse(json)
        assertEquals(2, schema.fields.size)
        assertEquals("title", schema.fields[0].name)
        assertEquals("integer", schema.fields[1].type)
    }

    // Kotlin
    @Test
    fun `parse from JSON string with nested fields`() {
        val json = """
        {
          "fields": [
            {
              "name": "product",
              "type": "object",
              "description": "Product info",
              "objectMemberProperties": [
                {
                  "name": "name",
                  "type": "string",
                  "description": "Product name",
                  "required": true
                },
                {
                  "name": "variants",
                  "type": "array",
                  "required": false,
                  "arrayElements": {
                    "name": "variant",
                    "type": "object",
                    "required": false,
                    "objectMemberProperties": [
                      { "name": "sku", "type": "string", "required": false },
                      { "name": "price", "type": "number", "required": false }
                    ]
                  }
                }
              ]
            }
          ]
        }
    """.trimIndent()

        val schema = ExtractionSchema.parse(json)
        assertEquals(1, schema.fields.size)

        val product = schema.fields[0]
        assertEquals("product", product.name)
        assertEquals("object", product.type)
        Assertions.assertEquals("Product info", product.description)

        // properties under object
        assertEquals(2, product.objectMemberProperties.size)
        val nameField = product.objectMemberProperties.first { it.name == "name" }
        assertEquals("string", nameField.type)
        assertTrue(nameField.required)

        val variantsField = product.objectMemberProperties.first { it.name == "variants" }
        assertEquals("array", variantsField.type)
        assertFalse(variantsField.required)
        Assertions.assertNotNull(variantsField.arrayElements)

        // items under array
        val item = variantsField.arrayElements!!
        assertEquals("variant", item.name)
        assertEquals("object", item.type)
        assertFalse(item.required)
        assertTrue(item.objectMemberProperties.any { it.name == "sku" && it.type == "string" && !it.required })
        assertTrue(item.objectMemberProperties.any { it.name == "price" && it.type == "number" && !it.required })
    }

    // Kotlin
    @Test
    fun `parse from JSON string `() {
        val json = """
{
  "fields": [
    {
      "name": "articles",
      "type": "array",
      "description": "文章列表",
      "items": {
        "type": "object",
        "fields": [
          {
            "name": "title",
            "type": "string",
            "description": "文章标题",
            "required": true
          },
          {
            "name": "comments",
            "type": "string",
            "description": "评论数",
            "required": true
          }
        ]
      }
    }
  ]
}
        """.trimIndent()
    }

    @Test
    fun `legacy map adapter marks fields optional and sets descriptions`() {
        val map = mapOf(
            "title" to "Title text",
            "price" to "Price number"
        )
        val schema = ExtractionSchema.fromMap(map)
        val root = mapper.readTree(schema.toJsonSchema())

        val props = root.get("properties")
        Assertions.assertEquals("Title text", props.get("title").get("description").asText())
        Assertions.assertEquals("Price number", props.get("price").get("description").asText())
        // no required array expected since legacy fields are optional by default
        assertFalse(root.has("required"))
    }
}
