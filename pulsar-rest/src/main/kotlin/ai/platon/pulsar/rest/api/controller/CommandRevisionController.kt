package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.rest.api.common.COMMAND_REQUEST_TEMPLATE
import ai.platon.pulsar.rest.api.common.PLACEHOLDER_JSON_VALUE
import ai.platon.pulsar.rest.api.service.ChatService
import ai.platon.pulsar.rest.api.service.ConversationService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

const val COMMAND_REVISION_TEMPLATE = """
    Your task is to convert a JSON command into simple, numbered steps in plain language.

The JSON format looks like this:
```json
$COMMAND_REQUEST_TEMPLATE
```

Guidelines:
- Start with "Visit [url]"
- Convert each action in "onBrowserLaunchedActions" to a separate step
- Convert each action in "onPageReadyActions" to a separate step
- Add steps for summarizing, data extraction, and link collection if specified
- Use clear, concise numbered instructions

Example:
1. Visit https://example.com
2. Scroll down 
3. Click the "Sign In" button
4. Summarize the page content
5. Extract product name, price, ratings
6. Collect all product links

JSON to convert:
```json
$PLACEHOLDER_JSON_VALUE
```

"""

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

        val message = COMMAND_REVISION_TEMPLATE.replace(PLACEHOLDER_JSON_VALUE, json)

        return chatService.chat(message)
    }
}
