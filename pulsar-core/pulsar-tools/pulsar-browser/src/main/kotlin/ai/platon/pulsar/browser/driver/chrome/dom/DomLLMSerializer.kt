package ai.platon.pulsar.browser.driver.chrome.dom

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object DomLLMSerializer {
    private val mapper = jacksonObjectMapper()

    fun serialize(root: SimplifiedNode, includeAttributes: List<String>): String {
        // TODO: respect includeAttributes filtering like Python serializer
        return mapper.writeValueAsString(root)
    }
}
