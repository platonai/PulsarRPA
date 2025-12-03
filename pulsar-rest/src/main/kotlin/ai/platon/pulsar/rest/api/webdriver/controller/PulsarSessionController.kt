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
 * Controller for PulsarSession operations.
 * Provides URL normalization, page loading, and URL submission capabilities.
 */
@RestController
@CrossOrigin
@RequestMapping(
    "/session/{sessionId}",
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class PulsarSessionController(
    @Autowired(required = false) private val sessionManager: SessionManager?,
    @Autowired(required = false) private val store: InMemoryStore?
) {
    private val logger = LoggerFactory.getLogger(PulsarSessionController::class.java)
    
    private val useRealSessions: Boolean = sessionManager != null

    companion object {
        /** Minimum number of slashes in a valid URL (protocol:// + domain = 3 slashes). */
        private const val MIN_URL_SLASH_COUNT = 3
    }

    /**
     * Normalizes a URL with optional load arguments.
     */
    @PostMapping("/normalize", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun normalize(
        @PathVariable sessionId: String,
        @RequestBody request: NormalizeRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} normalizing URL: {}", sessionId, request.url)
        ControllerUtils.addRequestId(response)

        val result = if (useRealSessions) {
            val session = sessionManager!!.getSession(sessionId)
                ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
            
            // Use real PulsarSession.normalize
            val normUrl = session.pulsarSession.normalize(request.url, request.args ?: "")
            NormUrlResult(
                spec = normUrl.spec,
                url = normUrl.url.toString(),
                args = normUrl.args,
                isNil = normUrl.isNil
            )
        } else {
            if (!store!!.sessionExists(sessionId)) {
                return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
            }
            // Mock implementation - normalizes the URL
            val normalizedUrl = normalizeUrl(request.url)
            NormUrlResult(
                spec = normalizedUrl,
                url = normalizedUrl,
                args = request.args,
                isNil = normalizedUrl.isBlank()
            )
        }
        return ResponseEntity.ok(NormalizeResponse(value = result))
    }

    /**
     * Opens a URL immediately, bypassing the local cache.
     */
    @PostMapping("/open", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun open(
        @PathVariable sessionId: String,
        @RequestBody request: OpenRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} opening URL: {}", sessionId, request.url)
        ControllerUtils.addRequestId(response)

        val result = if (useRealSessions) {
            val session = sessionManager!!.getSession(sessionId)
                ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
            
            // Use real PulsarSession.open (which fetches fresh from internet)
            val page = runBlocking {
                session.pulsarSession.open(request.url)
            }
            
            sessionManager.setSessionUrl(sessionId, request.url)
            
            WebPageResult(
                url = page.url,
                location = page.location ?: page.url,
                contentType = page.contentType ?: "text/html",
                contentLength = page.contentLength.toInt(),
                protocolStatus = page.protocolStatus?.toString() ?: "200 OK",
                isNil = page.isNil
            )
        } else {
            if (!store!!.sessionExists(sessionId)) {
                return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
            }
            // Mock implementation
            store.setSessionUrl(sessionId, request.url)
            WebPageResult(
                url = request.url,
                location = request.url,
                contentType = "text/html",
                contentLength = 1024,
                protocolStatus = "200 OK",
                isNil = false
            )
        }
        return ResponseEntity.ok(OpenResponse(value = result))
    }

    /**
     * Loads a URL from local storage or fetches from the internet.
     * Checks local cache first; if page exists and meets criteria, returns cached version.
     * Otherwise, fetches from the internet.
     */
    @PostMapping("/load", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun load(
        @PathVariable sessionId: String,
        @RequestBody request: LoadRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} loading URL: {} with args: {}", sessionId, request.url, request.args)
        ControllerUtils.addRequestId(response)

        val result = if (useRealSessions) {
            val session = sessionManager!!.getSession(sessionId)
                ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
            
            // Use real PulsarSession.load (checks cache first)
            val page = if (request.args != null) {
                session.pulsarSession.load(request.url, request.args)
            } else {
                session.pulsarSession.load(request.url)
            }
            
            sessionManager.setSessionUrl(sessionId, request.url)
            
            WebPageResult(
                url = page.url,
                location = page.location ?: page.url,
                contentType = page.contentType ?: "text/html",
                contentLength = page.contentLength.toInt(),
                protocolStatus = page.protocolStatus?.toString() ?: "200 OK",
                isNil = page.isNil
            )
        } else {
            if (!store!!.sessionExists(sessionId)) {
                return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
            }
            // Mock implementation
            store.setSessionUrl(sessionId, request.url)
            WebPageResult(
                url = request.url,
                location = request.url,
                contentType = "text/html",
                contentLength = 2048,
                protocolStatus = "200 OK (from cache: mock)",
                isNil = false
            )
        }
        return ResponseEntity.ok(LoadResponse(value = result))
    }

    /**
     * Submits a URL to the crawl pool for asynchronous processing.
     * This is a non-blocking operation that returns immediately.
     */
    @PostMapping("/submit", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun submit(
        @PathVariable sessionId: String,
        @RequestBody request: SubmitRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} submitting URL: {} with args: {}", sessionId, request.url, request.args)
        ControllerUtils.addRequestId(response)

        val success = if (useRealSessions) {
            val session = sessionManager!!.getSession(sessionId)
                ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
            
            // Use real PulsarSession.submit
            if (request.args != null) {
                session.pulsarSession.submit(request.url, request.args)
            } else {
                session.pulsarSession.submit(request.url)
            }
            true
        } else {
            if (!store!!.sessionExists(sessionId)) {
                return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
            }
            // Mock always returns success
            true
        }
        
        return ResponseEntity.ok(SubmitResponse(value = success))
    }

    /**
     * Normalizes a URL by removing fragments, normalizing scheme, etc.
     * Used in mock mode only.
     */
    private fun normalizeUrl(url: String): String {
        if (url.isBlank()) return ""
        
        var normalized = url.trim()
        
        // Add scheme if missing
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://$normalized"
        }
        
        // Remove fragment
        val fragmentIndex = normalized.indexOf('#')
        if (fragmentIndex > 0) {
            normalized = normalized.substring(0, fragmentIndex)
        }
        
        // Remove trailing slash for consistency (unless it's the root)
        if (normalized.endsWith("/") && normalized.count { it == '/' } > MIN_URL_SLASH_COUNT) {
            normalized = normalized.dropLast(1)
        }
        
        return normalized
    }
}
