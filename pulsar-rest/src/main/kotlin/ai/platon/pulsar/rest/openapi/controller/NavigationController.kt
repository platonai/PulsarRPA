package ai.platon.pulsar.rest.openapi.controller

import ai.platon.pulsar.rest.openapi.dto.*
import ai.platon.pulsar.rest.openapi.service.SessionManager
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
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
    private val sessionManager: SessionManager
) {
    private val logger = LoggerFactory.getLogger(NavigationController::class.java)

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

        val session = sessionManager.getSession(sessionId)
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

        val session = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        // Return the stored URL from session
        val url = session.url ?: "about:blank"

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

        val session = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        // For now, return the current URL (in real implementation, this could be different from URL)
        val uri = session.url ?: "about:blank"

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

        val session = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        // Extract base URI from current URL (protocol + host)
        val baseUri = session.url?.let { url ->
            try {
                val uri = java.net.URI(url)
                "${uri.scheme}://${uri.host}${if (uri.port > 0 && uri.port != 80 && uri.port != 443) ":${uri.port}" else ""}"
            } catch (e: Exception) {
                url
            }
        } ?: "about:blank"

        return ResponseEntity.ok(WebDriverResponse(value = baseUri))
    }
}
