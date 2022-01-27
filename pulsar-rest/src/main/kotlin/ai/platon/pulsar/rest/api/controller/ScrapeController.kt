package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.rest.api.entities.ScrapeRequest
import ai.platon.pulsar.rest.api.entities.ScrapeResponse
import ai.platon.pulsar.rest.api.entities.ScrapeStatusRequest
import ai.platon.pulsar.rest.api.service.ScrapeService
import org.springframework.context.ApplicationContext
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

@RestController
@CrossOrigin
@RequestMapping(
    "x",
    consumes = [MediaType.TEXT_PLAIN_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class ScrapeController(
    val applicationContext: ApplicationContext,
    val scrapeService: ScrapeService,
) {
    /**
     * @param sql The sql to execute
     * @return The response
     * */
    @PostMapping("e")
    fun execute(@RequestBody sql: String): ScrapeResponse {
        return scrapeService.executeQuery(ScrapeRequest(sql))
    }

    /**
     * @param sql The sql to execute
     * @return The uuid of the scrape task
     * */
    @PostMapping("s")
    fun submitJob(@RequestBody sql: String): String {
        return scrapeService.submitJob(ScrapeRequest(sql))
    }

    /**
     * @param uuid The uuid of the task last submitted
     * @return The execution result
     * */
    @GetMapping("status", consumes = [MediaType.ALL_VALUE])
    fun status(
        @RequestParam uuid: String,
        httpRequest: HttpServletRequest,
    ): ScrapeResponse {
        val request = ScrapeStatusRequest(uuid)
        return scrapeService.getStatus(request)
    }
}
