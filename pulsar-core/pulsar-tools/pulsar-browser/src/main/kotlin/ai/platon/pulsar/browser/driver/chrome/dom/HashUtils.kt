package ai.platon.pulsar.browser.driver.chrome.dom

import java.security.MessageDigest

object HashUtils {
    private fun sha256Hex(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        val sb = StringBuilder(digest.size * 2)
        for (b in digest) {
            val i = b.toInt() and 0xff
            if (i < 0x10) sb.append('0')
            sb.append(i.toString(16))
        }
        return sb.toString()
    }

    fun elementHash(node: EnhancedDOMTreeNode): String {
        val tag = node.nodeName.lowercase()
        val id = node.attributes["id"].orEmpty()
        val classes = node.attributes["class"]?.trim()?.split(Regex("\\s+"))?.sorted()?.joinToString(".") ?: ""
        val nameAttr = node.attributes["name"].orEmpty()
        val typeAttr = node.attributes["type"].orEmpty()
        val role = node.axRole.orEmpty()
        val backend = node.backendNodeId?.toString().orEmpty()
        val key = listOf(tag, id, classes, nameAttr, typeAttr, role, backend).joinToString("|")
        return sha256Hex(key.toByteArray(Charsets.UTF_8))
    }
}
