package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.serialize.json.JsonExtractor
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.common.urls.URLUtils
import ai.platon.pulsar.rest.api.common.*
import ai.platon.pulsar.rest.api.entities.CommandRequest
import ai.platon.pulsar.rest.api.entities.CommandStatus
import ai.platon.pulsar.skeleton.session.PulsarSession
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service

@Service
class ConversationService(
    val session: PulsarSession
) {
    fun convertAPIRequestCommandToJSON(request: String, url: String): String? {
        require(URLUtils.isStandard(url)) { "URL must not be blank" }

        // Replace the URL in the request with a placeholder, so the result from the LLM can be cached.
        val processedRequest = request.replace(url, PLACEHOLDER_URL)
        val prompt = API_REQUEST_COMMAND_CONVERSION_TEMPLATE
            .replace(PLACEHOLDER_REQUEST, processedRequest)

        var content = session.chat(prompt).content
        if (content.isBlank()) {
            return null
        }
        content = content.replace(PLACEHOLDER_URL, url)

        return JsonExtractor.extractJsonBlocks(content).firstOrNull()
    }

    /**
     * Converts a request string into a PromptRequestL2 object.
     *
     * This method extracts a URL from the request string and uses it to create a PromptRequestL2 object.
     * The URL is replaced with a placeholder in the request string to allow for caching of the result from the LLM.
     *
     * @param request The request string containing a URL.
     * @return A PromptRequestL2 object if a URL is found in the request string, null otherwise.
     * */
    fun convertPromptToRequest(request: String): CommandRequest? {
        if (request.isBlank()) {
            return null
        }

        val urls = LinkExtractors.fromText(request)
        if (urls.isEmpty()) {
            return null
        }
        val url = urls.first()

        val json = convertAPIRequestCommandToJSON(request, url)
        if (json.isNullOrBlank()) {
            return null
        }

        val request2: CommandRequest = pulsarObjectMapper().readValue(json)
        request2.url = url
        return request2
    }

    fun convertResponseToMarkdown(jsonResponse: String): String {
        val userMessage = CONVERT_RESPONSE_TO_MARKDOWN_PROMPT_TEMPLATE.replace(JSON_STRING_PLACEHOLDER, jsonResponse)
        return session.chat(userMessage).content
    }

    fun convertResponseToMarkdown(status: CommandStatus): String {
        val jsonResponse = pulsarObjectMapper().writeValueAsString(status)
        return convertResponseToMarkdown(jsonResponse)
    }
}
