package ai.platon.pulsar.rest.openapi.controller

import ai.platon.pulsar.rest.openapi.dto.*
import ai.platon.pulsar.rest.openapi.service.SessionManager
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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
    private val sessionManager: SessionManager
) {
    private val logger = LoggerFactory.getLogger(ControlController::class.java)

    companion object {
        /** Maximum allowed delay in milliseconds to prevent resource exhaustion. */
        private const val MAX_DELAY_MS = 30_000
    }

    /**
     * Delays execution for a specified duration.
     *
     * @param sessionId The session identifier.
     * @param request The delay request containing duration in milliseconds.
     * @param response The HTTP response to add headers.
     * @return WebDriver-style response.
     */
    @PostMapping("/delay", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun delay(
        @PathVariable sessionId: String,
        @RequestBody request: DelayRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} delaying for {} ms", sessionId, request.ms)
        ControllerUtils.addRequestId(response)

        if (!sessionManager.sessionExists(sessionId)) {
            return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
        }

        // Sleep for the requested duration (capped for safety)
        val delayMs = request.ms.coerceIn(0, MAX_DELAY_MS)
        if (delayMs > 0) {
            Thread.sleep(delayMs.toLong())
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
        ControllerUtils.addRequestId(response)

        val updated = sessionManager.setSessionStatus(sessionId, "paused")
        if (!updated) {
            return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
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
        ControllerUtils.addRequestId(response)

        val updated = sessionManager.setSessionStatus(sessionId, "stopped")
        if (!updated) {
            return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
        }

        return ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
    }
}
