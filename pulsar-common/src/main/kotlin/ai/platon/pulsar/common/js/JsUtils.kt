package ai.platon.pulsar.common.js

object JsUtils {
    fun toIIFE(jsFunctionCode: String, args: String = ""): String {
        val trimmed = jsFunctionCode.trim { it.isWhitespace() || it == ';' }

        return if (trimmed.startsWith("function") || trimmed.startsWith("(") || trimmed.startsWith("async") || trimmed.startsWith("x") || trimmed.startsWith("{")) {
            "(${trimmed})($args);"
        } else if (trimmed.contains("=>")) {
            "(${trimmed})($args);"
        } else {
            "// ❌ Unsupported format: not a valid JS function"
        }
    }
}
