package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.rest.api.entities.ScrapeRequest
import ai.platon.pulsar.rest.api.entities.ScrapeResponse
import ai.platon.pulsar.rest.api.entities.ScrapeStatusRequest
import ai.platon.pulsar.rest.api.service.ScrapeService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux

@RestController
@CrossOrigin
@RequestMapping(
    "api/scrape/tasks",
    consumes = [MediaType.ALL_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class ScrapeController(
    val scrapeService: ScrapeService,
) {
    /**
     * @param sql The sql to execute
     * @return The response
     * */
    @PostMapping("/execute")
    fun execute(@RequestBody sql: String): ScrapeResponse {
        return scrapeService.executeQuery(ScrapeRequest(sql))
    }

    /**
     * @param sql The sql to execute
     * @return The uuid of the scrape task
     * */
    @PostMapping("/submit")
    fun submitJob(@RequestBody sql: String): String {
        return scrapeService.submitJob(ScrapeRequest(sql))
    }

    /**
     * @param status The status of the scrape task to be counted
     * @return The execution result
     * */
    @GetMapping("/count", consumes = [MediaType.ALL_VALUE])
    fun count(
        @RequestParam(value = "status", required = false) status: Int = 0,
        httpRequest: HttpServletRequest,
    ): Int {
        return scrapeService.count(status)
    }

    @GetMapping("/{id}/status", consumes = [MediaType.ALL_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getStatus(
        @PathVariable(value = "id") uuid: String,
        httpRequest: HttpServletRequest,
    ): ScrapeResponse {
        val request = ScrapeStatusRequest(uuid)
        return scrapeService.getStatus(request)
    }

    @GetMapping(value = ["/{id}/stream"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamEvents(@PathVariable id: String): Flux<ServerSentEvent<ScrapeResponse>> {
        return scrapeService.streamEvents(id)
    }
}
