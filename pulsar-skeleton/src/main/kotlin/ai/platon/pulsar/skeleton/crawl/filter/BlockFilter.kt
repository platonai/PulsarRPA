package ai.platon.pulsar.skeleton.crawl.filter

import ai.platon.pulsar.common.DomUtil
import com.google.gson.annotations.Expose
import org.apache.commons.collections4.CollectionUtils
import org.w3c.dom.Node

class BlockFilter {
    @Expose
    var allow: HashSet<String> = HashSet()
    @Expose
    var disallow: HashSet<String> = HashSet()

    fun isDisallowed(node: Node): Boolean {
        // TODO : use real css selector
        val simpleSelectors = DomUtil.getSimpleSelectors(node)
        return (disallow.isNotEmpty() && CollectionUtils.containsAny(disallow, simpleSelectors))
    }

    fun isAllowed(node: Node): Boolean {
        val simpleSelectors = DomUtil.getSimpleSelectors(node)
        return (allow.isEmpty() || CollectionUtils.containsAny(allow, simpleSelectors))
    }

    override fun toString(): String {
        return "\n\tallow$allow\n\tdisallow$disallow"
    }
}
