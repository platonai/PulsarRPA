package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.rest.api.common.*
import ai.platon.pulsar.rest.api.service.ChatService
import ai.platon.pulsar.rest.api.service.ConversationService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping(
    "api/command-revisions",
    consumes = [MediaType.ALL_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class CommandRevisionController(
    private val chatService: ChatService,
    private val conversationService: ConversationService,
) {
    @PostMapping(produces = [MediaType.TEXT_PLAIN_VALUE])
    fun create(@RequestBody prompt: String): String {
        var urls = LinkExtractors.fromText(prompt)
        if (urls.isEmpty()) {
            urls = setOf(AppConstants.EXAMPLE_URL)
        }

        val json = conversationService.convertPlainCommandToJSON(prompt, urls.first()) ?: return prompt

        val message = COMMAND_REVISION_TEMPLATE
            .replace(PLACEHOLDER_REQUEST_JSON_COMMAND_TEMPLATE, REQUEST_JSON_COMMAND_TEMPLATE)
            .replace(PLACEHOLDER_REQUEST_PLAIN_COMMAND_TEMPLATE, REQUEST_PLAIN_COMMAND_TEMPLATE)
            .replace(PLACEHOLDER_JSON_VALUE, json)

        return chatService.chat(message)
    }
}
