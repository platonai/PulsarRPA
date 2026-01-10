package ai.platon.pulsar.rest.openapi.controller

import ai.platon.pulsar.rest.openapi.dto.*
import ai.platon.pulsar.rest.openapi.service.SessionManager
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
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
@ConditionalOnBean(SessionManager::class)
class PulsarSessionController(
    private val sessionManager: SessionManager
) {
    private val logger = LoggerFactory.getLogger(PulsarSessionController::class.java)

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

        val session = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        // Use real PulsarSession.normalize
        val normUrl = session.pulsarSession.normalize(request.url, request.args ?: "")
        val result = NormUrlResult(
            spec = normUrl.spec,
            url = normUrl.url.toString(),
            args = normUrl.args,
            isNil = normUrl.isNil
        )

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

        val session = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        // Use real PulsarSession.open (which fetches fresh from internet)
        val page = runBlocking {
            session.pulsarSession.open(request.url)
        }

        sessionManager.setSessionUrl(sessionId, request.url)

        val result = WebPageResult(
            url = page.url,
            location = page.location ?: page.url,
            contentType = page.contentType ?: "text/html",
            contentLength = page.contentLength.toInt(),
            protocolStatus = page.protocolStatus?.toString() ?: "200 OK",
            isNil = page.isNil
        )

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

        val session = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        // Use real PulsarSession.load (checks cache first)
        val page = if (request.args != null) {
            session.pulsarSession.load(request.url, request.args)
        } else {
            session.pulsarSession.load(request.url)
        }

        sessionManager.setSessionUrl(sessionId, request.url)

        val result = WebPageResult(
            url = page.url,
            location = page.location ?: page.url,
            contentType = page.contentType ?: "text/html",
            contentLength = page.contentLength.toInt(),
            protocolStatus = page.protocolStatus?.toString() ?: "200 OK",
            isNil = page.isNil
        )

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

        val session = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        // Use real PulsarSession.submit
        if (request.args != null) {
            session.pulsarSession.submit(request.url, request.args)
        } else {
            session.pulsarSession.submit(request.url)
        }

        return ResponseEntity.ok(SubmitResponse(value = true))
    }
}
