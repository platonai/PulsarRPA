package ai.platon.pulsar.common.serialize.json

class JSONExtractor {
    companion object {
        /**
         * Extracts JSON blocks from the given text.
         * Each JSON block must be a valid JSON object enclosed in curly braces.
         * */
        fun extractJsonBlocks(text: String?): List<String> {
            if (text.isNullOrBlank()) {
                return emptyList()
            }

            return JSONExtractor().extractJsonBlocks(text)
        }
    }

    /**
     * Extracts JSON blocks from the given text.
     * Each JSON block must be a valid JSON object enclosed in curly braces.
     * */
    fun extractJsonBlocks(text: String): List<String> {
        val jsonBlocks: MutableList<String> = ArrayList()
        var braceCount = 0
        var start = -1

        for (i in text.indices) {
            val ch = text[i]
            if (ch == '{') {
                if (braceCount == 0) {
                    start = i
                }
                braceCount++
            } else if (ch == '}') {
                braceCount--
                if (braceCount == 0 && start != -1) {
                    jsonBlocks.add(text.substring(start, i + 1))
                    start = -1
                }
            }
        }

        return jsonBlocks
    }
}

