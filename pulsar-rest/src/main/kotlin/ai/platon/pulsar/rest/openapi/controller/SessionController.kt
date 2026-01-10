package ai.platon.pulsar.rest.openapi.controller

import ai.platon.pulsar.rest.openapi.dto.*
import ai.platon.pulsar.rest.openapi.service.SessionManager
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller for WebDriver session management.
 */
@RestController
@CrossOrigin
@RequestMapping(
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class SessionController(
    private val sessionManager: SessionManager
) {
    private val logger = LoggerFactory.getLogger(SessionController::class.java)

    /**
     * Creates a new WebDriver session.
     */
    @PostMapping("/session", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun createSession(
        @RequestBody(required = false) request: NewSessionRequest?,
        response: HttpServletResponse
    ): ResponseEntity<NewSessionResponse> {
        logger.debug("Creating new session with capabilities: {}", request?.capabilities)
        ControllerUtils.addRequestId(response)

        val session = sessionManager.createSession(request?.capabilities)
        val responseBody = NewSessionResponse(
            value = NewSessionResponse.SessionValue(
                sessionId = session.sessionId,
                capabilities = session.capabilities
            )
        )
        return ResponseEntity.ok(responseBody)
    }

    /**
     * Retrieves session details.
     */
    @GetMapping("/session/{sessionId}")
    fun getSession(
        @PathVariable sessionId: String,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Getting session: {}", sessionId)
        ControllerUtils.addRequestId(response)

        val session = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        val responseBody = SessionDetails(
            value = SessionDetails.SessionDetailsValue(
                sessionId = session.sessionId,
                url = session.url,
                status = session.status,
                capabilities = session.capabilities
            )
        )
        return ResponseEntity.ok(responseBody)
    }

    /**
     * Deletes a session.
     */
    @DeleteMapping("/session/{sessionId}")
    fun deleteSession(
        @PathVariable sessionId: String,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Deleting session: {}", sessionId)
        ControllerUtils.addRequestId(response)

        val deleted = sessionManager.deleteSession(sessionId)
        if (!deleted) {
            return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
        }

        return ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
    }
}
