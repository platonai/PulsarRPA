package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.dom.model.BrowserState
import ai.platon.pulsar.browser.driver.chrome.dom.model.MicroDOMTree
import ai.platon.pulsar.browser.driver.chrome.dom.model.MicroDOMTreeNode
import ai.platon.pulsar.browser.driver.chrome.dom.model.NanoDOMTree
import ai.platon.pulsar.common.serialize.json.doubleBindModule
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object DOMSerializer {
    val MAPPER: ObjectMapper = jacksonObjectMapper().apply {
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
        setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        registerModule(doubleBindModule())
    }

    fun toJson(root: MicroDOMTree): String {
        return MAPPER.writeValueAsString(root)
    }

    fun toJson(nodes: List<MicroDOMTreeNode>): String {
        return MAPPER.writeValueAsString(nodes)
    }

    fun toJson(browserState: BrowserState): String {
        return MAPPER.writeValueAsString(browserState)
    }

    // serialize nano tree
    fun toJson(nano: NanoDOMTree): String {
        return MAPPER.writeValueAsString(nano)
    }
}
