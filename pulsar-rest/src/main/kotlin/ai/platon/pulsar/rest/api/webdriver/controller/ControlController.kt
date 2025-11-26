package ai.platon.pulsar.rest.api.webdriver.controller

import ai.platon.pulsar.rest.api.webdriver.dto.*
import ai.platon.pulsar.rest.api.webdriver.store.InMemoryStore
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * Controller for execution control operations (delay, pause, stop).
 */
@RestController
@CrossOrigin
@RequestMapping(
    "/session/{sessionId}/control",
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class ControlController(
    private val store: InMemoryStore
) {
    private val logger = LoggerFactory.getLogger(ControlController::class.java)

    /**
     * Delays execution for a specified duration.
     */
    @PostMapping("/delay", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun delay(
        @PathVariable sessionId: String,
        @RequestBody request: DelayRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} delaying for {} ms", sessionId, request.ms)
        addRequestId(response)

        if (!store.sessionExists(sessionId)) {
            return notFound("session not found", "No active session with id $sessionId")
        }

        // Mock implementation - sleep for the requested duration
        if (request.ms > 0 && request.ms <= 30000) {
            Thread.sleep(request.ms.toLong())
        }

        return ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
    }

    /**
     * Pauses session execution.
     */
    @PostMapping("/pause")
    fun pause(
        @PathVariable sessionId: String,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} pausing", sessionId)
        addRequestId(response)

        val updated = store.setSessionStatus(sessionId, "paused")
        if (!updated) {
            return notFound("session not found", "No active session with id $sessionId")
        }

        return ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
    }

    /**
     * Stops session execution.
     */
    @PostMapping("/stop")
    fun stop(
        @PathVariable sessionId: String,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} stopping", sessionId)
        addRequestId(response)

        val updated = store.setSessionStatus(sessionId, "stopped")
        if (!updated) {
            return notFound("session not found", "No active session with id $sessionId")
        }

        return ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
    }

    private fun addRequestId(response: HttpServletResponse) {
        response.addHeader("X-Request-Id", UUID.randomUUID().toString())
    }

    private fun notFound(error: String, message: String): ResponseEntity<Any> {
        return ResponseEntity.status(404).body(
            ErrorResponse(
                value = ErrorResponse.ErrorValue(
                    error = error,
                    message = message
                )
            )
        )
    }
}
