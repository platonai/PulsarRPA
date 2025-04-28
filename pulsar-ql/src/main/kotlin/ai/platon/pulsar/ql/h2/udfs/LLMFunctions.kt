package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.serialize.json.JsonExtractor
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.ql.common.annotation.UDFGroup
import ai.platon.pulsar.ql.common.annotation.UDFunction
import ai.platon.pulsar.ql.common.types.ValueDom
import ai.platon.pulsar.ql.common.types.ValueStringJSON
import ai.platon.pulsar.skeleton.context.PulsarContexts
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.concurrent.atomic.AtomicInteger

@Suppress("unused")
@UDFGroup(namespace = "LLM")
object LLMFunctions {
    private val logger = getLogger(this)
    private val session get() = PulsarContexts.getOrCreateSession()
    private val llmFailureWarnings = AtomicInteger(0)

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
    @UDFunction(description = "Extract fields from the content of the given DOM with the LLM model")
    fun extract(dom: ValueDom, fieldDescriptions: String): ValueStringJSON {
        val result = extractInternal(dom.element.text(), fieldDescriptions)
        return ValueStringJSON.get(pulsarObjectMapper().writeValueAsString(result), Map::class.qualifiedName)
    }

    internal fun extractInternal(domContent: String, fieldDescriptions: String): Map<String, String> {
        val prompt = EXTRACT_PROMPT.replace("{fieldDescriptions}", fieldDescriptions)
        val content = session.chat(domContent, prompt).content

        val jsonBlocks = JsonExtractor.extractJsonBlocks(content)
        if (jsonBlocks.isEmpty()) {
            if (llmFailureWarnings.get() % 50 == 0) {
                logger.warn("{}th failure to extract a JSON from LLM's response | {}", llmFailureWarnings.get().inc(), content)
            }
            llmFailureWarnings.incrementAndGet()
            return mapOf()
        }

        return try {
            val jsonBlock = jsonBlocks[0]
            val result: Map<String, String> = pulsarObjectMapper().readValue(jsonBlock)
            result
        } catch (e: Exception) {
            // e.printStackTrace()
            logger.warn("Failed to parse JSON from LLM's response | $content", e)
            mapOf()
        }
    }
}
