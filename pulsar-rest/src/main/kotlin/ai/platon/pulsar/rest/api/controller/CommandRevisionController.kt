package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.rest.api.service.ChatService
import ai.platon.pulsar.rest.api.service.CommandService
import ai.platon.pulsar.rest.api.service.ConversationService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping(
    "commands/revision",
    consumes = [MediaType.ALL_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class CommandRevisionController(
    private val commandService: CommandService,
    private val chatService: ChatService,
    private val conversationService: ConversationService,
) {
    @PostMapping(produces = [MediaType.TEXT_PLAIN_VALUE])
    fun create(@RequestBody prompt: String): String {
        var urls = LinkExtractors.fromText(prompt)
        if (urls.isEmpty()) {
            urls = setOf(AppConstants.EXAMPLE_URL)
        }

        val json = conversationService.convertAPIRequestCommandToJSON(prompt, urls.first()) ?: return prompt

        val message = """
You will receive a JSON object with the following fixed format:

{
  "url": "https://keep-me-unchanged.com",
  "pageSummaryPrompt": "Summarize or analyze...",     // optional
  "dataExtractionRules": "Extract fields like...",    // optional
  "linkExtractionRules": "Extract links like...",     // optional
  "onPageReadyActions": [                             // optional
    "scroll down",
    "click 'Sign In' button"
  ]
}

Please convert it into clear, step-by-step spoken instructions.

Instructions:
- Keep the URL exactly as is and mention it first.
- Then list each "onPageReadyActions" step individually, if any.
- If "pageSummaryPrompt" exists, add a step to summarize or analyze the page using that prompt.
- If "dataExtractionRules" exists, add a step describing how to extract the specified data.
- If "linkExtractionRules" exists, add a step describing how to extract those links.

Use simple, direct language. Number each step clearly and keep explanations short and easy to follow.

Example output format:

1. Open your browser and go to https://keep-me-unchanged.com.
2. After the page loads, scroll down.
3. Then, click the "Sign In" button.
4. Summarize or analyze the page using this prompt: "Summarize or analyze..."
5. Extract data as follows: "Extract fields like..."
6. Collect links as follows: "Extract links like..."

---

Now convert the following JSON:

```json
$json
```
        """

        return chatService.chat(message)
    }
}
