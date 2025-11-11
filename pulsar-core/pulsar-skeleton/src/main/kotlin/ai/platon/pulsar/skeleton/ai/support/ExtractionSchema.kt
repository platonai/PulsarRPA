package ai.platon.pulsar.skeleton.ai.support

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * A richer schema definition than a simple Map<String,String> for extraction output.
 * Supports nested objects and arrays so that callers can express hierarchical expectations.
 */
data class ExtractionField(
    val name: String,
    val type: String = "string",                 // JSON schema primitive or 'object' / 'array'
    val description: String? = null,
    val required: Boolean = true,
    val properties: List<ExtractionField> = emptyList(), // children if object
    val items: ExtractionField? = null                    // item schema if array
) {
    fun toJsonSchemaNode(mapper: ObjectMapper): ObjectNode {
        val node = JsonNodeFactory.instance.objectNode()
        node.put("type", type)
        if (!description.isNullOrBlank()) node.put("description", description)
        when (type) {
            "object" -> {
                val propsNode = JsonNodeFactory.instance.objectNode()
                val requiredList = mutableListOf<String>()
                properties.forEach { child ->
                    propsNode.set<ObjectNode>(child.name, child.toJsonSchemaNode(mapper))
                    if (child.required) requiredList += child.name
                }
                node.set<ObjectNode>("properties", propsNode)
                if (requiredList.isNotEmpty()) {
                    node.set<ArrayNode>("required", mapper.valueToTree<ArrayNode>(requiredList))
                }
            }

            "array" -> {
                val itemNode = items?.toJsonSchemaNode(mapper) ?: JsonNodeFactory.instance.objectNode()
                    .apply { put("type", "string") }
                node.set<ObjectNode>("items", itemNode)
            }
        }
        return node
    }

    companion object {
        /** Convenience for a simple string field. */
        fun string(name: String, description: String? = null, required: Boolean = true): ExtractionField =
            ExtractionField(name = name, type = "string", description = description, required = required)

        /** Convenience for an object field with child properties. */
        fun obj(
            name: String,
            properties: List<ExtractionField>,
            description: String? = null,
            required: Boolean = true
        ): ExtractionField = ExtractionField(
            name = name,
            type = "object",
            description = description,
            required = required,
            properties = properties
        )

        /** Convenience for an array field with a given item schema. */
        fun arrayOf(
            name: String,
            item: ExtractionField,
            description: String? = null,
            required: Boolean = true
        ): ExtractionField = ExtractionField(
            name = name,
            type = "array",
            description = description,
            required = required,
            items = item
        )
    }
}

class ExtractionSchema(val fields: List<ExtractionField>) {
    /**
     * Convert the internal representation to a standard JSON Schema (draft-agnostic) string.
     */
    fun toJsonSchema(): String {
        val mapper = ObjectMapper()
        val root = JsonNodeFactory.instance.objectNode()
        root.put("type", "object")
        val propsNode = JsonNodeFactory.instance.objectNode()
        val required = mutableListOf<String>()
        fields.forEach { f ->
            propsNode.set<ObjectNode>(f.name, f.toJsonSchemaNode(mapper))
            if (f.required) required += f.name
        }
        root.set<ObjectNode>("properties", propsNode)
        if (required.isNotEmpty()) root.set<ArrayNode>("required", mapper.valueToTree<ArrayNode>(required))
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root)
    }

    companion object {
        private val mapper = jacksonObjectMapper()

        /** Default rich extraction schema (JSON Schema string) */
        @JsonIgnore
        val DEFAULT: ExtractionSchema =
            ExtractionSchema(
                listOf(
                    ExtractionField("title", type = "string", description = "Page title"), ExtractionField(
                        "content",
                        type = "string",
                        description = "Primary textual content of the page",
                        required = false
                    ), ExtractionField(
                        name = "links",
                        type = "array",
                        description = "Important hyperlinks on the page",
                        required = false,
                        items = ExtractionField(
                            name = "link", type = "object", properties = listOf(
                                ExtractionField("text", type = "string", description = "Anchor text", required = false),
                                ExtractionField("href", type = "string", description = "Href URL", required = false)
                            ), required = false
                        )
                    )
                )
            )

        fun parse(json: String): ExtractionSchema {
            return mapper.readValue(json)
        }

        /** Utility adapter: build a schema from a legacy Map<String,String> where value = description */
        fun fromMap(map: Map<*, *>): ExtractionSchema {
            val fields = map.entries.map { (k, v) ->
                ExtractionField(
                    name = k.toString(),
                    description = v.toString(),
                    required = false
                )
            }
            return ExtractionSchema(fields)
        }
    }
}
