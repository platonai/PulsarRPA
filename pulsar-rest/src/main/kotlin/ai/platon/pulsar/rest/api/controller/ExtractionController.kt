package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.rest.api.entities.PromptRequest
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
    "api/extractions",
    consumes = [MediaType.ALL_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class ExtractionController(
    val extractService: ExtractService,
) {
    private val extractionsCache = ConcurrentSkipListMap<String, String>()
    private val executor: ExecutorService = Executors.newCachedThreadPool()

    @PostMapping("")
    fun executeExtraction(@RequestBody request: PromptRequest): String {
        return extractService.extract(request)
    }

    @PostMapping("/async")
    fun executeExtractionAsync(@RequestBody request: PromptRequest): String {
        val uuid = UUID.randomUUID().toString()
        executor.submit {
            extractionsCache[uuid] = extractService.extract(request)
        }
        return uuid
    }

    @GetMapping("/{id}")
    fun extractionResult(@PathVariable id: String): String {
        return extractionsCache[id] ?: "Extraction not found"
    }

    @GetMapping("/{id}/status")
    fun extractionStatus(@PathVariable id: String): String {
        return extractionsCache[id] ?: "Extraction not found"
    }

    @GetMapping("/{id}/stream")
    fun extractionStream(@PathVariable id: String): SseEmitter {
        val emitter = SseEmitter()
        executor.submit {
            while (extractionsCache.containsKey(id)) {
                try {
                    val result = extractionsCache[id]
                    if (result != null) {
                        emitter.send(result)
                    } else {
                        emitter.send("Extraction not found")
                    }
                } catch (e: Exception) {
                    emitter.send("Error: ${e.message}")
                } finally {
                    emitter.complete()
                }
            }
        }
        return emitter
    }
}
