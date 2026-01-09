package ai.platon.pulsar.rest.api.webdriver.controller

import ai.platon.pulsar.rest.api.webdriver.dto.*
import ai.platon.pulsar.rest.api.webdriver.service.SessionManager
import ai.platon.pulsar.rest.api.webdriver.store.InMemoryStore
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriverException
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.Base64

/**
 * Controller for selector-first element operations.
 */
@RestController
@CrossOrigin
@RequestMapping(
    "/session/{sessionId}/selectors",
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class SelectorController(
    @param:Autowired(required = false) private val sessionManager: SessionManager?,
    private val store: InMemoryStore
) {
    private val logger = LoggerFactory.getLogger(SelectorController::class.java)

    private val useRealSessions: Boolean = sessionManager != null

    /**
     * Checks if an element matching the selector exists.
     */
    @PostMapping("/exists", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun selectorExists(
        @PathVariable sessionId: String,
        @RequestBody request: SelectorRef,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} checking selector exists: {}", sessionId, request.selector)
        ControllerUtils.addRequestId(response)

        if (useRealSessions) {
            val managed = sessionManager!!.getSession(sessionId)
                ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

            return try {
                val exists = runBlocking {
                    val driver = managed.pulsarSession.getOrCreateBoundDriver()
                    driver.exists(request.selector)
                }
                ResponseEntity.ok(ExistsResponse(value = ExistsResponse.ExistsValue(exists = exists)))
            } catch (e: WebDriverException) {
                logger.error("Selector exists check failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
                ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
            } catch (e: Exception) {
                logger.error("Selector exists check failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
                ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
            }
        }

        if (!store.sessionExists(sessionId)) {
            return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
        }

        // Mock implementation always returns true for demonstration
        return ResponseEntity.ok(ExistsResponse(value = ExistsResponse.ExistsValue(exists = true)))
    }

    /**
     * Waits for an element matching the selector to appear.
     */
    @PostMapping("/waitFor", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun waitForSelector(
        @PathVariable sessionId: String,
        @RequestBody request: WaitForRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} waiting for selector: {} (timeout: {}ms)", sessionId, request.selector, request.timeout)
        ControllerUtils.addRequestId(response)

        if (useRealSessions) {
            val managed = sessionManager!!.getSession(sessionId)
                ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

            val timeoutMillis = request.timeout.toLong().coerceAtLeast(0)

            return try {
                val remainingMillis = runBlocking {
                    val driver = managed.pulsarSession.getOrCreateBoundDriver()
                    driver.waitForSelector(request.selector, timeoutMillis)
                }

                if (remainingMillis <= 0L) {
                    // OpenAPI defines 408 for waitFor timeout.
                    return ResponseEntity.status(408).body(
                        ErrorResponse(
                            value = ErrorResponse.ErrorValue(
                                error = "timeout",
                                message = "Timeout waiting for selector '${request.selector}'"
                            )
                        )
                    )
                }

                ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
            } catch (e: WebDriverException) {
                logger.error("Wait for selector failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
                ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
            } catch (e: Exception) {
                logger.error("Wait for selector failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
                ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
            }
        }

        if (!store.sessionExists(sessionId)) {
            return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
        }

        // Mock implementation - immediately returns success
        return ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
    }

    /**
     * Finds a single element by selector.
     */
    @PostMapping("/element", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun findElementBySelector(
        @PathVariable sessionId: String,
        @RequestBody request: SelectorRef,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} finding element by selector: {}", sessionId, request.selector)
        ControllerUtils.addRequestId(response)

        val element = store.getOrCreateElement(sessionId, request.selector, request.strategy)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return ResponseEntity.ok(ElementResponse(value = ElementRef(elementId = element.elementId)))
    }

    /**
     * Finds all elements matching the selector.
     */
    @PostMapping("/elements", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun findElementsBySelector(
        @PathVariable sessionId: String,
        @RequestBody request: SelectorRef,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} finding elements by selector: {}", sessionId, request.selector)
        ControllerUtils.addRequestId(response)

        val element = store.getOrCreateElement(sessionId, request.selector, request.strategy)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        // Mock implementation returns a single element
        return ResponseEntity.ok(ElementsResponse(value = listOf(ElementRef(elementId = element.elementId))))
    }

    /**
     * Clicks an element by selector.
     */
    @PostMapping("/click", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun clickBySelector(
        @PathVariable sessionId: String,
        @RequestBody request: SelectorRef,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} clicking selector: {}", sessionId, request.selector)
        ControllerUtils.addRequestId(response)

        if (useRealSessions) {
            val managed = sessionManager!!.getSession(sessionId)
                ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

            return try {
                runBlocking {
                    val driver = managed.pulsarSession.getOrCreateBoundDriver()
                    driver.click(request.selector)
                }
                ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
            } catch (e: WebDriverException) {
                logger.error("Click failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
                ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
            } catch (e: Exception) {
                logger.error("Click failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
                ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
            }
        }

        if (!store.sessionExists(sessionId)) {
            return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
        }

        store.getOrCreateElement(sessionId, request.selector, request.strategy)
        return ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
    }

    /**
     * Fills an input element by selector.
     */
    @PostMapping("/fill", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun fillBySelector(
        @PathVariable sessionId: String,
        @RequestBody request: FillRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} filling selector: {} with value: {}", sessionId, request.selector, request.value)
        ControllerUtils.addRequestId(response)

        if (useRealSessions) {
            val managed = sessionManager!!.getSession(sessionId)
                ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

            return try {
                runBlocking {
                    val driver = managed.pulsarSession.getOrCreateBoundDriver()
                    driver.fill(request.selector, request.value)
                }
                ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
            } catch (e: WebDriverException) {
                logger.error("Fill failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
                ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
            } catch (e: Exception) {
                logger.error("Fill failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
                ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
            }
        }

        if (!store.sessionExists(sessionId)) {
            return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
        }

        store.getOrCreateElement(sessionId, request.selector, request.strategy)
        return ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
    }

    /**
     * Presses a key on an element by selector.
     */
    @PostMapping("/press", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun pressBySelector(
        @PathVariable sessionId: String,
        @RequestBody request: PressRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} pressing key: {} on selector: {}", sessionId, request.key, request.selector)
        ControllerUtils.addRequestId(response)

        if (useRealSessions) {
            val managed = sessionManager!!.getSession(sessionId)
                ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

            return try {
                runBlocking {
                    val driver = managed.pulsarSession.getOrCreateBoundDriver()
                    driver.press(request.selector, request.key)
                }
                ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
            } catch (e: WebDriverException) {
                logger.error("Press failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
                ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
            } catch (e: Exception) {
                logger.error("Press failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
                ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
            }
        }

        if (!store.sessionExists(sessionId)) {
            return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
        }

        store.getOrCreateElement(sessionId, request.selector, request.strategy)
        return ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
    }

    /**
     * Gets the outer HTML of an element by selector.
     */
    @PostMapping("/outerHtml", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun getOuterHtmlBySelector(
        @PathVariable sessionId: String,
        @RequestBody request: SelectorRef,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} getting outerHtml for selector: {}", sessionId, request.selector)
        ControllerUtils.addRequestId(response)

        if (useRealSessions) {
            val managed = sessionManager!!.getSession(sessionId)
                ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

            return try {
                val html = runBlocking {
                    val driver = managed.pulsarSession.getOrCreateBoundDriver()
                    driver.outerHTML(request.selector)
                }
                ResponseEntity.ok(HtmlResponse(value = html))
            } catch (e: WebDriverException) {
                logger.error("Get outerHtml failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
                ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
            } catch (e: Exception) {
                logger.error("Get outerHtml failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
                ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
            }
        }

        val element = store.getOrCreateElement(sessionId, request.selector, request.strategy)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return ResponseEntity.ok(HtmlResponse(value = element.outerHtml))
    }

    /**
     * Takes a screenshot of an element by selector.
     */
    @PostMapping("/screenshot", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun screenshotBySelector(
        @PathVariable sessionId: String,
        @RequestBody request: SelectorRef,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} taking screenshot of selector: {}", sessionId, request.selector)
        ControllerUtils.addRequestId(response)

        if (useRealSessions) {
            val managed = sessionManager!!.getSession(sessionId)
                ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

            return try {
                val base64 = runBlocking {
                    val driver = managed.pulsarSession.getOrCreateBoundDriver()
                    driver.captureScreenshot(request.selector)
                }
                ResponseEntity.ok(ScreenshotResponse(value = base64))
            } catch (e: WebDriverException) {
                logger.error("Screenshot failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message)
                ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
            } catch (e: Exception) {
                logger.error("Screenshot failed | sessionId={} selector={} | {}", sessionId, request.selector, e.message, e)
                ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
            }
        }

        if (!store.sessionExists(sessionId)) {
            return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
        }

        val mockPng = Base64.getEncoder().encodeToString("mock-screenshot-data".toByteArray())
        return ResponseEntity.ok(ScreenshotResponse(value = mockPng))
    }
}
