package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.rest.api.entities.CommandRequest
import ai.platon.pulsar.rest.api.entities.CommandResult
import ai.platon.pulsar.rest.api.entities.CommandStatus
import ai.platon.pulsar.rest.api.service.CommandService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux

@RestController
@CrossOrigin
@RequestMapping(
    "commands",
    consumes = [MediaType.ALL_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class CommandController(
    val commandService: CommandService,
) {
    /**
     * Execute a command with structured JSON input and output.
     *
     * @param request The structured command request
     * @return Structured command response
     * */
    @PostMapping
    fun submitCommand(@RequestBody request: CommandRequest): ResponseEntity<Any> {
        return when (request.mode) {
            "sync" -> ResponseEntity.ok(commandService.executeSync(request))
            "async" -> ResponseEntity.ok(commandService.submitAsync(request))
            else -> ResponseEntity.badRequest().body("Invalid mode: ${request.mode}")
        }
    }

    @GetMapping(value = ["/{id}/status"])
    fun getStatus(@PathVariable id: String): ResponseEntity<CommandStatus> {
        return ResponseEntity.ok(commandService.getStatus(id))
    }

    @GetMapping(value = ["/{id}/result"])
    fun getResult(@PathVariable id: String): ResponseEntity<CommandResult> {
        return ResponseEntity.ok(commandService.getResult(id))
    }

    @GetMapping(value = ["/{id}/stream"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamEvents(@PathVariable id: String): Flux<ServerSentEvent<CommandStatus>> {
        return commandService.streamEvents(id)
    }
}
