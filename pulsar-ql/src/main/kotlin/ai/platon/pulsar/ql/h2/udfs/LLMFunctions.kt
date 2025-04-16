package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.ql.common.annotation.UDFGroup
import ai.platon.pulsar.ql.common.annotation.UDFunction
import ai.platon.pulsar.ql.common.types.ValueDom
import ai.platon.pulsar.skeleton.context.PulsarContexts

@Suppress("unused")
@UDFGroup(namespace = "LLM")
object LLMFunctions {
    private val logger = getLogger(this)
    private val session get() = PulsarContexts.getOrCreateSession()

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
    fun extract(dom: ValueDom, fieldDescriptions: String): String {
        val prompt =
            """
Extract fields from the given HTML content, return the result in JSON format like:

```json
{
  "field1": "value1",
  "field2": "value2"
}
```

Field descriptions:
$fieldDescriptions

""".trimIndent()

        val content = session.chat(dom.element, prompt).content
        val json = Strings.extractFlatJSON(content)
        if (json == null) {
            logger.warn("Failed to extract a json from LLM's response | $content")
        }
        return json
    }
}
