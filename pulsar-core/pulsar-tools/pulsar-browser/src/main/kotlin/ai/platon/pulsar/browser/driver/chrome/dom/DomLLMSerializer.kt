package ai.platon.pulsar.browser.driver.chrome.dom

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.LinkedHashSet
import java.util.Locale

object DomLLMSerializer {
    private val mapper = jacksonObjectMapper()

    fun serialize(root: SimplifiedNode, includeAttributes: List<String>): String {
        val attributeWhitelist = includeAttributes.map { it.lowercase(Locale.ROOT) }.toSet()
        val serializable = root.toSerializable(attributeWhitelist)
        return mapper.writeValueAsString(serializable)
    }

    private fun SimplifiedNode.toSerializable(attributeWhitelist: Set<String>): SerializableSimplifiedNode {
        val filteredAttributes = if (attributeWhitelist.isEmpty()) {
            emptyMap()
        } else {
            attributes.filterKeys { key -> key.lowercase(Locale.ROOT) in attributeWhitelist }
        }

        val mergedChildren = LinkedHashSet<SimplifiedNode>()
        mergedChildren.addAll(children)
        mergedChildren.addAll(shadowRoots)

        return SerializableSimplifiedNode(
            tag = tag.lowercase(Locale.ROOT),
            id = id,
            classes = classes,
            attributes = filteredAttributes,
            text = text,
            interactiveIndex = interactiveIndex,
            shouldDisplay = shouldDisplay,
            children = mergedChildren.map { child -> child.toSerializable(attributeWhitelist) }
        )
    }

    private data class SerializableSimplifiedNode(
        val tag: String,
        val id: String?,
        val classes: List<String>,
        val attributes: Map<String, String>,
        val text: String?,
        val interactiveIndex: Int?,
        val shouldDisplay: Boolean,
        val children: List<SerializableSimplifiedNode>,
    )
}
