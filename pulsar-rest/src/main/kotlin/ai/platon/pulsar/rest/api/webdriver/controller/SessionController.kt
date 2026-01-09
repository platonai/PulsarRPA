package ai.platon.pulsar.rest.api.webdriver.controller

import ai.platon.pulsar.rest.api.webdriver.dto.*
import ai.platon.pulsar.rest.api.webdriver.service.SessionManager
import ai.platon.pulsar.rest.api.webdriver.store.InMemoryStore
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller for WebDriver session management.
 * Supports both mock mode (InMemoryStore) and real mode (SessionManager).
 */
@RestController
@CrossOrigin
@RequestMapping(
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class SessionController(
    @Autowired(required = false) private val sessionManager: SessionManager?,
    @Autowired(required = false) private val store: InMemoryStore?
) {
    private val logger = LoggerFactory.getLogger(SessionController::class.java)
    
    private val useRealSessions: Boolean = sessionManager != null

    init {
        if (useRealSessions) {
            logger.info("SessionController initialized with real browser sessions")
        } else {
            logger.info("SessionController initialized with mock sessions")
        }
    }

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

        val responseBody = if (useRealSessions) {
            val session = sessionManager!!.createSession(request?.capabilities)
            NewSessionResponse(
                value = NewSessionResponse.SessionValue(
                    sessionId = session.sessionId,
                    capabilities = session.capabilities
                )
            )
        } else {
            val session = store!!.createSession(request?.capabilities)
            NewSessionResponse(
                value = NewSessionResponse.SessionValue(
                    sessionId = session.sessionId,
                    capabilities = session.capabilities
                )
            )
        }
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

        val responseBody = if (useRealSessions) {
            val session = sessionManager!!.getSession(sessionId)
                ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
            SessionDetails(
                value = SessionDetails.SessionDetailsValue(
                    sessionId = session.sessionId,
                    url = session.url,
                    status = session.status,
                    capabilities = session.capabilities
                )
            )
        } else {
            val session = store!!.getSession(sessionId)
                ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
            SessionDetails(
                value = SessionDetails.SessionDetailsValue(
                    sessionId = session.sessionId,
                    url = session.url,
                    status = session.status,
                    capabilities = session.capabilities
                )
            )
        }
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

        val deleted = if (useRealSessions) {
            sessionManager!!.deleteSession(sessionId)
        } else {
            store!!.deleteSession(sessionId)
        }
        
        if (!deleted) {
            return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
        }

        return ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
    }
}
