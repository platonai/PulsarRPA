package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.rest.api.entities.PromptRequest
import ai.platon.pulsar.rest.api.service.ChatService
import ai.platon.pulsar.rest.api.service.ConversationService
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
    "api/conversations",
    consumes = [MediaType.ALL_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class ConversationController(
    val chatService: ChatService
) {
    private val conversationsCache = ConcurrentSkipListMap<String, String>()
    private val executor: ExecutorService = Executors.newCachedThreadPool()

    @GetMapping("")
    fun conversations(@RequestParam(value = "prompt") prompt: String): String {
        return chatService.chat(prompt)
    }

    @GetMapping("/async")
    fun conversationsAsync(@RequestParam(value = "prompt") prompt: String): String {
        val id = UUID.randomUUID().toString()
        executor.submit {
            conversationsCache[id] = chatService.chat(prompt)
        }
        return id
    }

    @PostMapping("")
    fun conversationsPost(@RequestBody prompt: String): String {
        return chatService.chat(prompt)
    }

    // async version
    @PostMapping("/async")
    fun conversationsPostAsync(@RequestBody prompt: String): String {
        val id = UUID.randomUUID().toString()
        executor.submit {
            conversationsCache[id] = chatService.chat(prompt)
        }
        return id
    }

    @PostMapping("/about")
    fun conversationsAbout(@RequestBody request: PromptRequest): String {
        return chatService.chat(request)
    }

    @PostMapping("/about/async")
    fun conversationsAboutAsync(@RequestBody request: PromptRequest): String {
        val id = UUID.randomUUID().toString()
        executor.submit {
            conversationsCache[id] = chatService.chat(request)
        }
        return id
    }

    @GetMapping("/{id}")
    fun conversationResult(@PathVariable id: String): String {
        return conversationsCache[id] ?: "No result found for id: $id"
    }

    @GetMapping("/{id}/status")
    fun conversationStatus(@PathVariable id: String): String {
        return if (conversationsCache.containsKey(id)) {
            "Processing"
        } else {
            "Completed"
        }
    }

    @GetMapping("/{id}/stream")
    fun conversationStream(@PathVariable id: String): SseEmitter {
        val emitter = SseEmitter()
        executor.submit {
            try {
                val result = conversationsCache[id]
                if (result != null) {
                    emitter.send(result)
                } else {
                    emitter.send("No result found for id: $id")
                }
            } catch (e: Exception) {
                emitter.completeWithError(e)
            } finally {
                emitter.complete()
            }
        }
        return emitter
    }
}
