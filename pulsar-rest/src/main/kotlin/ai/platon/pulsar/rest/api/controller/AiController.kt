package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.rest.api.entities.PromptRequest
import ai.platon.pulsar.rest.api.entities.CommandRequest
import ai.platon.pulsar.rest.api.entities.CommandStatus
import ai.platon.pulsar.rest.api.entities.ScrapeResponse
import ai.platon.pulsar.rest.api.service.ChatService
import ai.platon.pulsar.rest.api.service.CommandService
import ai.platon.pulsar.rest.api.service.ConversationService
import ai.platon.pulsar.rest.api.service.ExtractService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.*
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@RestController
@CrossOrigin
@RequestMapping(
    "ai",
    consumes = [MediaType.ALL_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class AiController(
    val commandService: CommandService,
    val conversationService: ConversationService,
    val chatService: ChatService,
    val extractService: ExtractService,
) {
    private val commandStatusCache = ConcurrentSkipListMap<String, CommandStatus>()
    private val markdownResultCache = ConcurrentSkipListMap<String, String>()
    private val executor: ExecutorService = Executors.newCachedThreadPool()

    /**
     * Simple chat with the LLM.
     *
     * @param prompt The text prompt
     * @return The LLM response as text
     * */
    @GetMapping("/conversations")
    fun conversations(@RequestParam(value = "prompt") prompt: String): String {
        return chatService.chat(prompt)
    }

    /**
     * Simple chat with the LLM.
     *
     * @param prompt The text prompt
     * @return The LLM response as text
     * */
    @PostMapping("/conversations")
    fun conversationsPost(@RequestBody prompt: String): String {
        return chatService.chat(prompt)
    }

    /**
     * Chat with the LLM about a specific webpage.
     *
     * @param request The request containing URL and prompt
     * @return The LLM response as text
     * */
    @PostMapping("/conversations/about")
    fun conversationsAbout(@RequestBody request: PromptRequest): String {
        return chatService.chat(request)
    }

    @PostMapping("/extractions")
    fun executeExtraction(@RequestBody request: PromptRequest): String {
        return extractService.extract(request)
    }

    /**
     * Execute a command with structured JSON input and output.
     *
     * @param request The structured command request
     * @return Structured command response
     * */
    @PostMapping("/commands")
    fun executeCommand(@RequestBody request: CommandRequest): CommandStatus {
        return commandService.executeCommand(request)
    }

    /**
     * Execute a command asynchronously with structured JSON input and output.
     *
     * @param request The structured command request
     * @return UUID for tracking the command execution
     */
    @PostMapping(
        "/commands",
        consumes = [MediaType.TEXT_PLAIN_VALUE],
        produces = [MediaType.TEXT_MARKDOWN_VALUE]
    )
    fun executeCommandWithSpokenLanguage(@RequestBody request: String): String {
        val response = commandService.executeCommand(request)
        return conversationService.convertResponseToMarkdown(response)
    }

    @PostMapping(
        "/commands/async",
        consumes = [MediaType.TEXT_PLAIN_VALUE],
        produces = [MediaType.TEXT_MARKDOWN_VALUE]
    )
    fun executeCommandWithSpokenLanguageAsync(@RequestBody request: String): String {
        val id = UUID.randomUUID().toString()
        commandStatusCache[id] = CommandStatus(id)
        executor.submit {
            val response = commandService.executeCommand(request)
            commandStatusCache[id] = response
            markdownResultCache[id] = conversationService.convertResponseToMarkdown(response)
        }
        return id
    }

    @GetMapping(value = ["/commands/{id}/status"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun commandStatus(@PathVariable id: String): CommandStatus {
        return commandStatusCache[id] ?: CommandStatus.notFound(id)
    }

    @GetMapping(value = ["/commands/{id}/stream"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun commandStatusStream(@PathVariable id: String): SseEmitter {
        val emitter = SseEmitter(0L)
        executor.submit { emitResponseUntilDone(id, emitter) }
        return emitter
    }

    @PostMapping("/extract")
    fun executeExtractionBackward(@RequestBody request: PromptRequest) = executeExtraction(request)

    @PostMapping("/command")
    fun executeCommandBackward(@RequestBody request: CommandRequest) = executeCommand(request)

    @PostMapping(
        "/command",
        consumes = [MediaType.TEXT_PLAIN_VALUE],
        produces = [MediaType.TEXT_MARKDOWN_VALUE]
    )
    fun executeCommandWithSpokenLanguageBackward(@RequestBody request: String) =
        executeCommandWithSpokenLanguage(request)

    @GetMapping("/chat")
    fun conversationsBackward(@RequestParam(value = "prompt") prompt: String) = conversations(prompt)

    @PostMapping("/chat")
    fun conversationsPostBackward(@RequestBody prompt: String) = conversationsPost(prompt)

    @PostMapping("/chat-about")
    fun chatAboutPageBackward(@RequestBody request: PromptRequest) = conversationsAbout(request)

    private fun emitResponseUntilDone(id: String, emitter: SseEmitter) {
        try {
            while (true) {
                val resp = commandStatusCache[id]
                if (resp == null) {
                    val errorResp = ScrapeResponse()
                    errorResp.id = id
                    errorResp.statusCode = ResourceStatus.SC_INTERNAL_SERVER_ERROR
                    emitter.send(errorResp)
                    emitter.complete()
                    return
                }
                when (resp.statusCode) {
                    ResourceStatus.SC_OK, ResourceStatus.SC_INTERNAL_SERVER_ERROR -> {
                        emitter.send(resp)
                        emitter.complete()
                        return
                    }
                    else -> {
                        emitter.send(resp)
                        Thread.sleep(1000)
                    }
                }
            }
        } catch (e: Exception) {
            try {
                val errorResp = ScrapeResponse()
                errorResp.id = id
                errorResp.statusCode = ResourceStatus.SC_INTERNAL_SERVER_ERROR
                emitter.send(errorResp)
            } catch (ignored: Exception) {}
            emitter.completeWithError(e)
        }
    }
}
