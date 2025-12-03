package ai.platon.pulsar.rest.api.webdriver.controller

import ai.platon.pulsar.rest.api.webdriver.dto.*
import ai.platon.pulsar.rest.api.webdriver.service.SessionManager
import ai.platon.pulsar.rest.api.webdriver.store.InMemoryStore
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller for page navigation operations.
 */
@RestController
@CrossOrigin
@RequestMapping(
    "/session/{sessionId}",
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class NavigationController(
    @Autowired(required = false) private val sessionManager: SessionManager?,
    @Autowired(required = false) private val store: InMemoryStore?
) {
    private val logger = LoggerFactory.getLogger(NavigationController::class.java)
    
    private val useRealSessions: Boolean = sessionManager != null

    /**
     * Navigates to a URL.
     */
    @PostMapping("/url", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun navigateTo(
        @PathVariable sessionId: String,
        @RequestBody request: SetUrlRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} navigating to: {}", sessionId, request.url)
        ControllerUtils.addRequestId(response)

        if (useRealSessions) {
            val session = sessionManager!!.getSession(sessionId)
                ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
            
            try {
                // Use real PulsarSession to load the page
                runBlocking {
                    session.pulsarSession.load(request.url)
                }
                sessionManager.setSessionUrl(sessionId, request.url)
            } catch (e: Exception) {
                logger.error("Error navigating to URL: {}", e.message, e)
                return ControllerUtils.errorResponse("navigation error", "Failed to navigate: ${e.message}")
            }
        } else {
            if (!store!!.sessionExists(sessionId)) {
                return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
            }
            store.setSessionUrl(sessionId, request.url)
        }
        
        return ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
    }

    /**
     * Gets the current URL.
     */
    @GetMapping("/url")
    fun getCurrentUrl(
        @PathVariable sessionId: String,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} getting current URL", sessionId)
        ControllerUtils.addRequestId(response)

        val url = if (useRealSessions) {
            val session = sessionManager!!.getSession(sessionId)
                ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
            
            // Return the stored URL from session
            session.url ?: "about:blank"
        } else {
            if (!store!!.sessionExists(sessionId)) {
                return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
            }
            store.getSession(sessionId)?.url ?: "about:blank"
        }
        
        return ResponseEntity.ok(WebDriverResponse(value = url))
    }

    /**
     * Gets the document URI.
     */
    @GetMapping("/documentUri")
    fun getDocumentUri(
        @PathVariable sessionId: String,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} getting document URI", sessionId)
        ControllerUtils.addRequestId(response)

        val uri = if (useRealSessions) {
            val session = sessionManager!!.getSession(sessionId)
                ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
            
            // For now, return the current URL (in real implementation, this could be different from URL)
            session.url ?: "about:blank"
        } else {
            if (!store!!.sessionExists(sessionId)) {
                return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
            }
            store.getSession(sessionId)?.url ?: "about:blank"
        }
        
        return ResponseEntity.ok(WebDriverResponse(value = uri))
    }

    /**
     * Gets the base URI.
     */
    @GetMapping("/baseUri")
    fun getBaseUri(
        @PathVariable sessionId: String,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} getting base URI", sessionId)
        ControllerUtils.addRequestId(response)

        val baseUri = if (useRealSessions) {
            val session = sessionManager!!.getSession(sessionId)
                ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
            
            // Extract base URI from current URL (protocol + host)
            session.url?.let { url ->
                try {
                    val uri = java.net.URI(url)
                    "${uri.scheme}://${uri.host}${if (uri.port > 0 && uri.port != 80 && uri.port != 443) ":${uri.port}" else ""}"
                } catch (e: Exception) {
                    url
                }
            } ?: "about:blank"
        } else {
            if (!store!!.sessionExists(sessionId)) {
                return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
            }
            val url = store.getSession(sessionId)?.url
            url?.let {
                try {
                    val uri = java.net.URI(it)
                    "${uri.scheme}://${uri.host}${if (uri.port > 0 && uri.port != 80 && uri.port != 443) ":${uri.port}" else ""}"
                } catch (e: Exception) {
                    it
                }
            } ?: "about:blank"
        }
        
        return ResponseEntity.ok(WebDriverResponse(value = baseUri))
    }
}
