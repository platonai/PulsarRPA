package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMTreeNodeEx
import org.apache.commons.lang3.StringUtils

open class Locator(
    val type: Type,
    val selector: String,
) {
    enum class Type(val text: String) {
        CSS_PATH(""),
        XPATH("xpath"),
        HASH("hash"),
        BACKEND_NODE_ID("backend"),
        FRAME_BACKEND_NODE_ID("fbn"),
        NODE_ID("node"),
        INDEX("index");

        override fun toString() = text

        companion object {
            fun parse(str: String): Type? {
                return when (str) {
                    "" -> CSS_PATH
                    "css" -> CSS_PATH
                    "xpath" -> XPATH
                    "hash" -> HASH
                    "backend" -> BACKEND_NODE_ID
                    "node" -> NODE_ID
                    "fbn" -> FRAME_BACKEND_NODE_ID
                    "index" -> INDEX
                    else -> null
                }
            }
        }
    }

    val prefix get() = when (type) {
        Type.CSS_PATH -> ""
        else -> this@Locator.type.text + ":"
    }

    val absoluteSelector get() = "$prefix$selector"

    override fun hashCode() = absoluteSelector.hashCode()

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Locator -> absoluteSelector == other.absoluteSelector
            else -> false
        }
    }

    override fun toString(): String = absoluteSelector

    companion object {
        fun parse(selector: String): Locator? {
            val trimmed = selector.trim()
            val parts = trimmed.split(':')
            if (parts.size == 1) {
                return Locator(Type.CSS_PATH, parts[0])
            }

            val type = Type.parse(parts[0]) ?: return null
            return Locator(type, trimmed.substringAfter(":"))
        }
    }
}

class FBNLocator(
    val frameId: String,
    val backendNodeId: Int
): Locator(Type.FRAME_BACKEND_NODE_ID, "$frameId$SEPARATOR$backendNodeId") {

    constructor(frameId: Int, backendNodeId: Int): this(frameId.toString(), backendNodeId)

    val isRelative: Boolean get() = StringUtils.isNumeric(frameId)

    val isAbsolute: Boolean get() = !isRelative

    companion object {
        const val SEPARATOR = ","
        const val PREFIX = "fbn:"
        const val SIMPLIFIED_PATTERN = "\\d+$SEPARATOR\\d+"
        val SIMPLIFIED_REGEX = SIMPLIFIED_PATTERN.toRegex()
        const val PATTERN = "$PREFIX$SIMPLIFIED_PATTERN"
        val REGEX = PATTERN.toRegex()

        fun parse(str: String): FBNLocator? {
            val trimmed = str.trim()
            val frameId = StringUtils.substringBetween(trimmed, ":", SEPARATOR).toIntOrNull() ?: 0
            val backendNodeId = trimmed.substringAfterLast(SEPARATOR).toIntOrNull() ?: return null
            return FBNLocator(frameId, backendNodeId)
        }

        fun parseRelaxed(selector: String?): FBNLocator? {
            var trimmed = selector?.trim() ?: return null

            // Multi selectors are supported: `cssPath`, `xpath:`, `backend:`, `node:`, `hash:`, `fbn`, `index`
            if (!trimmed.startsWith(PREFIX)) {
                trimmed = "$PREFIX$selector"
            }

            return parse(trimmed)
        }
    }
}

class LocatorMap {
    private val map = mutableMapOf<Locator, DOMTreeNodeEx>()

    fun put(locator: Locator, node: DOMTreeNodeEx): DOMTreeNodeEx? {
        return map.put(locator, node)
    }

    operator fun get(locator: Locator): DOMTreeNodeEx? {
        return map[locator]
    }

    fun put(type: Locator.Type, selector: String, node: DOMTreeNodeEx) {
        map[Locator(type, selector)] = node
    }

    fun select(key: String): DOMTreeNodeEx? {
        // Support legacy string keys like plain hash, and prefixed forms like xpath:, backend:, node:, index:
        // Try prefixed first
        val colon = key.indexOf(':')
        if (colon > 0) {
            val typeStr = key.take(colon)
            val selector = key.substring(colon + 1)
            val type = Locator.Type.entries.firstOrNull { it.text == typeStr }
            if (type != null) return get(Locator(type, selector))
        }
        // Fallback: treat as element hash without prefix
        return map.entries.firstOrNull { it.key.type == Locator.Type.HASH && it.key.selector == key }?.value
    }

    fun toStringMap(): Map<String, DOMTreeNodeEx> {
        // Preserve insertion order similar to linkedMapOf in previous implementation
        val out = LinkedHashMap<String, DOMTreeNodeEx>(map.size)
        map.forEach { (k, v) ->
            out[k.absoluteSelector] = v
        }
        return out
    }
}
