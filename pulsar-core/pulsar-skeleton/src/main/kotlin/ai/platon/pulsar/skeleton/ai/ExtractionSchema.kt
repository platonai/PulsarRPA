package ai.platon.pulsar.skeleton.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode

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
                if (requiredList.isNotEmpty()) node.set("required", mapper.valueToTree(requiredList))
            }
            "array" -> {
                val itemNode = items?.toJsonSchemaNode(mapper) ?: JsonNodeFactory.instance.objectNode().apply { put("type", "string") }
                node.set<ObjectNode>("items", itemNode)
            }
        }
        return node
    }
}

class ExtractionSchema(private val fields: List<ExtractionField>) {
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
        if (required.isNotEmpty()) root.set("required", mapper.valueToTree(required))
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root)
    }
}

/** Utility adapter: build a schema from a legacy Map<String,String> where value = description */
fun legacyMapToExtractionSchema(map: Map<String,String>): ExtractionSchema {
    val fields = map.entries.map { (k,v) -> ExtractionField(name = k, description = v, required = false) }
    return ExtractionSchema(fields)
}

