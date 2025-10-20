package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMTreeNodeEx

enum class LocatorType(val value: String) {
    HASH("hash"),
    XPATH("xpath"),
    BACKEND_NODE_ID("backend"),
    FRAME_BACKEND_NODE_ID("fbn"),
    NODE_ID("node"),
    INDEX("index");

    override fun toString() = value
}

data class Locator(
    val type: LocatorType,
    val selector: String,
) {
    override fun toString(): String = "${type}:${selector}"
}

class LocatorMap {
    private val map = mutableMapOf<Locator, DOMTreeNodeEx>()

    fun add(type: LocatorType, selector: String, node: DOMTreeNodeEx) {
        map.putIfAbsent(Locator(type, selector), node)
    }

    fun select(locator: Locator): DOMTreeNodeEx? {
        return map[locator]
    }

    fun select(key: String): DOMTreeNodeEx? {
        // Support legacy string keys like plain hash, and prefixed forms like xpath:, backend:, node:, index:
        // Try prefixed first
        val colon = key.indexOf(':')
        if (colon > 0) {
            val typeStr = key.substring(0, colon)
            val selector = key.substring(colon + 1)
            val type = LocatorType.values().firstOrNull { it.value == typeStr }
            if (type != null) return select(Locator(type, selector))
        }
        // Fallback: treat as element hash without prefix
        return map.entries.firstOrNull { it.key.type == LocatorType.HASH && it.key.selector == key }?.value
    }

    fun toStringMap(): Map<String, DOMTreeNodeEx> {
        // Preserve insertion order similar to linkedMapOf in previous implementation
        val out = LinkedHashMap<String, DOMTreeNodeEx>(map.size)
        map.forEach { (k, v) ->
            val legacyKey = when (k.type) {
                LocatorType.HASH -> k.selector // no prefix for hash for backward compatibility
                LocatorType.XPATH -> "xpath:${k.selector}"
                LocatorType.BACKEND_NODE_ID -> "backend:${k.selector}"
                LocatorType.FRAME_BACKEND_NODE_ID -> "fbn:${k.selector}"
                LocatorType.NODE_ID -> "node:${k.selector}"
                LocatorType.INDEX -> "index:${k.selector}"
            }
            out.putIfAbsent(legacyKey, v)
        }
        return out
    }
}
