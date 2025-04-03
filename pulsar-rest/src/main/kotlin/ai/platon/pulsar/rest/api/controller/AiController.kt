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
    @PostMapping("chat")
    fun chat(@RequestBody request: PromptRequest): String {
        return promptService.chat(request)
    }

    @PostMapping("map")
    fun map(@RequestBody request: PromptRequest): String {
        return promptService.chat(request)
    }

    @PostMapping("scrape")
    fun scrape(@RequestBody request: PromptRequest): String {
        return promptService.chat(request)
    }

    @PostMapping("crawl")
    fun crawl(@RequestBody request: PromptRequest): String {
        return promptService.chat(request)
    }

    @PostMapping("extract")
    fun extract(@RequestBody request: PromptRequest): String {
        return promptService.chat(request)
    }
}
