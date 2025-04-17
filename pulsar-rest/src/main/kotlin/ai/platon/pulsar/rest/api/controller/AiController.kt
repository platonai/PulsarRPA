package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.rest.api.entities.PromptRequest
import ai.platon.pulsar.rest.api.service.PromptService
import org.springframework.context.ApplicationContext
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping(
    "ai",
    consumes = [MediaType.ALL_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class AiController(
    val applicationContext: ApplicationContext,
    val promptService: PromptService,
) {
    /**
     * Chat with the LLM about anything.
     *
     * @param prompt The request
     * @return The response
     * */
    @GetMapping("/chat")
    fun simpleChat(@RequestParam(value = "prompt") prompt: String): String {
        return promptService.chat(prompt)
    }
    /**
     * Chat with the LLM about anything.
     *
     * @param prompt The request
     * @return The response
     * */
    @PostMapping("/chat")
    fun chat(@RequestBody prompt: String): String {
        return promptService.chat(prompt)
    }

    /**
     * Chat with the LLM about the page specified by the url.
     *
     * @param request The request
     * @return The response
     * */
    @PostMapping("/chat-about")
    fun chatAboutPage(@RequestBody request: PromptRequest): String {
        return promptService.chat(request)
    }

    /**
     * Extract fields from the page specified by the url.
     *
     * @param request The request
     * @return The response
     * */
    @PostMapping("/extract")
    fun extractFieldsFromPage(@RequestBody request: PromptRequest): String {
        return promptService.extract(request)
    }
}
