package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.rest.api.entities.ScrapeRequest
import ai.platon.pulsar.rest.api.entities.ScrapeResponse
import ai.platon.pulsar.rest.api.entities.ScrapeStatusRequest
import ai.platon.pulsar.rest.api.service.ScrapeService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.ApplicationContext
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

/**
 * Old style API, reserved for backward compatibility
 * */
@RestController
@CrossOrigin
@RequestMapping(
    "api/x",
    consumes = [MediaType.ALL_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class ScrapeLegacyController(
    val applicationContext: ApplicationContext,
    val scrapeService: ScrapeService,
) {
    /**
     * @param sql The sql to execute
     * @return The response
     * */
    @PostMapping("/e")
    fun execute(@RequestBody sql: String): ScrapeResponse {
        return scrapeService.executeQuery(ScrapeRequest(sql))
    }

    /**
     * @param sql The sql to execute
     * @return The uuid of the scrape task
     * */
    @PostMapping("s")
    fun submitJob(@RequestBody sql: String): String {
        return scrapeService.submitJob(ScrapeRequest(sql)).uuid
    }

    /**
     * @param status The status of the scrape task to be counted, 0 means all tasks.
     * @return The execution result
     * */
    @GetMapping("c", consumes = [MediaType.ALL_VALUE])
    fun count(
        @RequestParam(value = "status", required = false) status: Int = 0,
        httpRequest: HttpServletRequest,
    ): Int {
        return scrapeService.count(status)
    }

    /**
     * @param uuid The uuid of the task last submitted
     * @return The execution result
     * */
    @GetMapping("status", consumes = [MediaType.ALL_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun status(
        @RequestParam(value = "uuid") uuid: String,
        httpRequest: HttpServletRequest,
    ): ScrapeResponse {
        val request = ScrapeStatusRequest(uuid)
        return scrapeService.getStatus(request)
    }
}
