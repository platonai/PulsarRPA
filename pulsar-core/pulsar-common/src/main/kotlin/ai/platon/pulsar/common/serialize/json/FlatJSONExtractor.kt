package ai.platon.pulsar.common.serialize.json

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.logging.ThrottlingLogger
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * Extracts and merges "flat" JSON objects (string-to-string key/value pairs) embedded in arbitrary text.
 *
 * A flat JSON block here is defined as a top-level JSON object whose values are all strings. Blocks are merged
 * shallowly in encounter order; if the same key appears in multiple valid blocks, the value from the later block
 * overwrites the earlier one.
 *
 * Invalid blocks (malformed JSON or blocks whose deserialized values are not all strings) are ignored.
 * This implementation is intentionally simple and does not attempt to validate braces inside string literals – it
 * relies on the upstream [JSONExtractor] behavior.
 *
 * Thread-safety: Instances are immutable and stateless; the extraction operation itself creates new collections
 * and is safe to invoke multiple times concurrently.
 */
class FlatJSONExtractor(
    private val text: String
) {
    companion object {
        // Removed FLAT_JSON_REGEX: JSONExtractor already returns brace-delimited object blocks.

        /**
         * Convenience one-shot extraction. Returns an empty map if [text] is null/blank or no valid flat JSON blocks exist.
         * Later blocks overwrite earlier keys.
         */
        fun extract(text: String?): Map<String, String> {
            if (text.isNullOrBlank()) {
                return emptyMap()
            }
            return FlatJSONExtractor(text).extract()
        }
    }

    private val throttlingLogger = ThrottlingLogger(getLogger(this::class))

    /**
     * Performs extraction over the instance's [text]. Returns a merged map of string values from all valid flat JSON blocks.
     *
     * Behavior:
     * - Blank input -> empty map
     * - Non-parsable or non-flat (contains non-string value) blocks are skipped
     * - Key collisions resolved by last-wins
     */
    fun extract(): Map<String, String> {
        if (text.isBlank()) {
            return emptyMap()
        }

        val jsonBlocks = JSONExtractor.extractJsonBlocks(text)
        if (jsonBlocks.isEmpty()) {
            return emptyMap()
        }

        val result: MutableMap<String, String> = mutableMapOf()

        for (block in jsonBlocks) {
            try {
                val jsonObject = pulsarObjectMapper().readValue<Map<String, String>>(block)
                result.putAll(jsonObject)
            } catch (e: Exception) {
                // Truncate block to avoid log flooding; use structured placeholders.
                val snippetMax = 512
                val snippet = if (block.length > snippetMax) block.substring(0, snippetMax) + "... (truncated, len=" + block.length + ")" else block
                throttlingLogger.info(
                    "Failed to parse flat JSON block length={} error={} snippet={}",
                    block.length, e.message, snippet, e
                )
            }
        }

        return result
    }
}
