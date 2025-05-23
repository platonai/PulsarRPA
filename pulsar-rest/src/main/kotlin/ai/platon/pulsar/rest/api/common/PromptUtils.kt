package ai.platon.pulsar.rest.api.common

import ai.platon.pulsar.common.serialize.json.JsonExtractor
import ai.platon.pulsar.rest.api.service.CommandService.Companion.MIN_USER_MESSAGE_LENGTH

object PromptUtils {

    fun normalizePageSummaryPrompt(message: String?): String? {
        val message2 = normalizeUserMessage(message) ?: return null

        val suffix = """

### Page Content:

{PLACEHOLDER_PAGE_CONTENT}

        """.trimIndent()

        return "$message2\n$suffix"
    }

    fun normalizeDataExtractionRules(message: String?): String? {
        val message2 = normalizeUserMessage(message) ?: return null

        val suffix = """

### Rules:
- According the request, extract fields from the page content
- Your result should be a JSON object, where the key is the field name and the value is the field value.

### Page Content:

{PLACEHOLDER_PAGE_CONTENT}

        """.trimIndent()

        return "$message2\n$suffix"
    }

    fun normalizeLinkExtractionRules(message: String?): Regex? {
        val message2 = normalizeUserMessage(message) ?: return null
        return try {
            message2.toRegex()
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * Normalizes the user message by trimming whitespace and ensuring it has a minimum length.
     *
     * deprecated, the link extraction rules should be a regex pattern
     * */
    fun normalizeLinkExtractionRulesDeprecated(message: String?): String? {
        val message2 = normalizeUserMessage(message) ?: return null

        val suffix = """

### Rules:
- According the request, extract links from the page content
- The link should be a valid URL
- No duplicate links
- Your result should be a JSON object, where the key is the link and the value is the link text.

### Page Content:

{PLACEHOLDER_PAGE_CONTENT}

        """.trimIndent()

        return "$message2\n$suffix"
    }

    /**
     * Normalizes the user message by trimming whitespace and ensuring it has a minimum length.
     * */
    fun normalizeUserMessage(message: String?): String? {
        return message?.trim()?.takeIf { it.length > MIN_USER_MESSAGE_LENGTH }
    }

}