package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.serialize.json.JsonExtractor
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.ql.common.annotation.UDFGroup
import ai.platon.pulsar.ql.common.annotation.UDFunction
import ai.platon.pulsar.ql.common.types.ValueDom
import ai.platon.pulsar.skeleton.context.PulsarContexts
import com.fasterxml.jackson.module.kotlin.readValue
import org.h2.value.ValueJavaObject

@Suppress("unused")
@UDFGroup(namespace = "LLM")
object LLMFunctions {
    private val logger = getLogger(this)
    private val session get() = PulsarContexts.getOrCreateSession()

    val EXTRACT_PROMPT =
        """
Extract fields from the given HTML content, return the result in JSON format like:

```json
{
  "field1": "value1",
  "field2": "value2"
}
```

Field descriptions:
{fieldDescriptions}

""".trimIndent()

    @JvmStatic
    @UDFunction(description = "Get the LLM model name")
    fun modelName(): String {
        return session.unmodifiedConfig["llm.name"] ?: "unknown"
    }

    @JvmStatic
    @UDFunction(description = "Chat with the LLM model")
    fun chat(prompt: String): String {
        return session.chat(prompt).content
    }

    @JvmStatic
    @UDFunction(description = "Chat with the LLM model")
    fun chat(dom: ValueDom, prompt: String): String {
        return session.chat(dom.element, prompt).content
    }

    @JvmStatic
    @UDFunction(description = "Extract fields from the given HTML content with the LLM model")
    fun extractToString(dom: ValueDom, fieldDescriptions: String): String {
        val prompt = EXTRACT_PROMPT.replace("{fieldDescriptions}", fieldDescriptions)

        val content = session.chat(dom.element, prompt).content
        val json = JsonExtractor.extractJsonBlocks(content)
        if (json.isEmpty()) {
            logger.warn("No json in LLM's response | $content")
            return ""
        }
        return json[0]
    }

    @JvmStatic
    @UDFunction(description = "Extract fields from the content of the given DOM with the LLM model")
    fun extract(dom: ValueDom, fieldDescriptions: String): Map<String, String?> {
        val prompt = EXTRACT_PROMPT.replace("{fieldDescriptions}", fieldDescriptions)
        val content = session.chat(dom.element, prompt).content
        val json = JsonExtractor.extractJsonBlocks(content)
        if (json.isEmpty()) {
            logger.warn("Failed to extract a json from LLM's response | $content")
            return mapOf()
        }

        return try {
            pulsarObjectMapper().readValue(json[0])
        } catch (e: Exception) {
            mapOf()
        }
    }
}
