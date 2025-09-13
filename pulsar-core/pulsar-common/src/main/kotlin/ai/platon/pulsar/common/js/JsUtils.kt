package ai.platon.pulsar.common.js

object JsUtils {
    /**
     * Convert a JS function to IIFE (Immediately Invoked Function Expression)
     *
     * @param jsFunctionCode JS function code
     * @param args arguments for the function
     * @return IIFE code, or empty string if the function is not valid
     * */
    fun toIIFE(jsFunctionCode: String, args: String = ""): String {
        return toIIFEOrNull(jsFunctionCode, args) ?: ""
    }

    /**
     * Convert a JS function to IIFE (Immediately Invoked Function Expression)
     *
     * @param jsFunctionCode JS function code
     * @param args arguments for the function
     * @return IIFE code, or null if the function is not valid
     * */
    fun toIIFEOrNull(jsFunctionCode: String, args: String = ""): String? {
        val trimmed = jsFunctionCode.trim { it.isWhitespace() || it == ';' }

        return if (trimmed.startsWith("function") || trimmed.startsWith("(") || trimmed.startsWith("async") || trimmed.startsWith("x") || trimmed.startsWith("{")) {
            "(${trimmed})($args);"
        } else if (trimmed.contains("=>")) {
            "(${trimmed})($args);"
        } else {
            // ❌ Unsupported format: not a valid JS function
            null
        }
    }
}
