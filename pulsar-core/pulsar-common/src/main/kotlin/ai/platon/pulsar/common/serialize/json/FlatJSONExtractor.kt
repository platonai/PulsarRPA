package ai.platon.pulsar.common.serialize.json

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.logging.ThrottlingLogger
import com.fasterxml.jackson.module.kotlin.readValue

class FlatJSONExtractor(
    private val text: String
) {
    companion object {
        private val FLAT_JSON_REGEX = Regex("""\{.*?\}""", RegexOption.DOT_MATCHES_ALL)

        /**
         * Extracts JSON blocks from the given text and flattens them into a single map.
         * Each JSON block must be a valid JSON object.
         */
        fun extract(text: String?): Map<String, String> {
            if (text.isNullOrBlank()) {
                return emptyMap()
            }

            return FlatJSONExtractor(text).extract()
        }
    }

    private val throttlingLogger = ThrottlingLogger(getLogger(this::class))

    fun extract(): Map<String, String> {
        if (text.isBlank()) {
            return emptyMap()
        }

        val jsonBlocks = JSONExtractor.extractJsonBlocks(text)
        if (jsonBlocks.isEmpty()) {
            return emptyMap()
        }

        val filteredBlocks = jsonBlocks.filter { it.matches(FLAT_JSON_REGEX) }

        val result: MutableMap<String, String> = mutableMapOf()

        for (block in filteredBlocks) {
            try {
                val jsonObject = pulsarObjectMapper().readValue<Map<String, String>>(block)
                result.putAll(jsonObject)
            } catch (e: Exception) {
                throttlingLogger.info("Failed to parse JSON block: $block", e)
            }
        }

        return result
    }
}