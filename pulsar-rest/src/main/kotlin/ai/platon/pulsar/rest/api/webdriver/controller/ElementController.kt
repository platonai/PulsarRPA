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
import java.util.UUID

/**
 * Controller for element operations by element ID.
 */
@RestController
@CrossOrigin
@RequestMapping(
    "/session/{sessionId}",
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class ElementController(
    @param:Autowired(required = false) private val sessionManager: SessionManager?,
    private val store: InMemoryStore
) {
    private val logger = LoggerFactory.getLogger(ElementController::class.java)
    
    private val useRealSessions: Boolean = sessionManager != null

    /**
     * Finds a single element using WebDriver locator strategy.
     */
    @PostMapping("/element", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun findElement(
        @PathVariable sessionId: String,
        @RequestBody request: FindElementRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} finding element using {}: {}", sessionId, request.using, request.value)
        ControllerUtils.addRequestId(response)

        // Convert WebDriver strategy to selector
        val selector = convertToSelector(request.using, request.value)
        val element = store.getOrCreateElement(sessionId, selector, "css")
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        return ResponseEntity.ok(ElementResponse(value = ElementRef(elementId = element.elementId)))
    }

    /**
     * Finds all elements using WebDriver locator strategy.
     */
    @PostMapping("/elements", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun findElements(
        @PathVariable sessionId: String,
        @RequestBody request: FindElementRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} finding elements using {}: {}", sessionId, request.using, request.value)
        ControllerUtils.addRequestId(response)

        val selector = convertToSelector(request.using, request.value)
        val element = store.getOrCreateElement(sessionId, selector, "css")
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        // Mock implementation returns a single element
        return ResponseEntity.ok(ElementsResponse(value = listOf(ElementRef(elementId = element.elementId))))
    }

    /**
     * Clicks an element by ID.
     */
    @PostMapping("/element/{elementId}/click")
    fun clickElement(
        @PathVariable sessionId: String,
        @PathVariable elementId: String,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} clicking element: {}", sessionId, elementId)
        ControllerUtils.addRequestId(response)

        if (useRealSessions) {
            val managed = sessionManager!!.getSession(sessionId)
                ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

            val element = store.getElement(sessionId, elementId)
                ?: return ControllerUtils.notFound("no such element", "Element with id $elementId not found")

            return try {
                runBlocking {
                    val driver = managed.pulsarSession.getOrCreateBoundDriver()
                    driver.click(element.selector)
                }
                ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
            } catch (e: WebDriverException) {
                logger.error("Element click failed | sessionId={} elementId={} | {}", sessionId, elementId, e.message)
                ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
            } catch (e: Exception) {
                logger.error("Element click failed | sessionId={} elementId={} | {}", sessionId, elementId, e.message, e)
                ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
            }
        }

        if (!store.sessionExists(sessionId)) {
            return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
        }

        val element = store.getElement(sessionId, elementId)
            ?: return ControllerUtils.notFound("no such element", "Element with id $elementId not found")

        // Mock implementation - just log the click
        logger.debug("Clicked element: {}", element.selector)
        return ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
    }

    /**
     * Sends keys to an element by ID.
     */
    @PostMapping("/element/{elementId}/value", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun sendKeysToElement(
        @PathVariable sessionId: String,
        @PathVariable elementId: String,
        @RequestBody request: SendKeysRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} sending keys to element: {}", sessionId, elementId)
        ControllerUtils.addRequestId(response)

        if (useRealSessions) {
            val managed = sessionManager!!.getSession(sessionId)
                ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

            val element = store.getElement(sessionId, elementId)
                ?: return ControllerUtils.notFound("no such element", "Element with id $elementId not found")

            return try {
                runBlocking {
                    val driver = managed.pulsarSession.getOrCreateBoundDriver()
                    driver.fill(element.selector, request.text)
                }
                ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
            } catch (e: WebDriverException) {
                logger.error("Send keys failed | sessionId={} elementId={} | {}", sessionId, elementId, e.message)
                ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
            } catch (e: Exception) {
                logger.error("Send keys failed | sessionId={} elementId={} | {}", sessionId, elementId, e.message, e)
                ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
            }
        }

        if (!store.sessionExists(sessionId)) {
            return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
        }

        val element = store.getElement(sessionId, elementId)
            ?: return ControllerUtils.notFound("no such element", "Element with id $elementId not found")

        // Mock implementation - update element text
        element.text = request.text
        return ResponseEntity.ok(WebDriverResponse<Any?>(value = null))
    }

    /**
     * Gets an element attribute by name.
     */
    @GetMapping("/element/{elementId}/attribute/{name}")
    fun getElementAttribute(
        @PathVariable sessionId: String,
        @PathVariable elementId: String,
        @PathVariable name: String,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} getting attribute {} from element: {}", sessionId, name, elementId)
        ControllerUtils.addRequestId(response)

        if (useRealSessions) {
            val managed = sessionManager!!.getSession(sessionId)
                ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

            val element = store.getElement(sessionId, elementId)
                ?: return ControllerUtils.notFound("no such element", "Element with id $elementId not found")

            return try {
                val value = runBlocking {
                    val driver = managed.pulsarSession.getOrCreateBoundDriver()
                    driver.selectFirstAttributeOrNull(element.selector, name)
                }
                ResponseEntity.ok(AttributeResponse(value = value ?: ""))
            } catch (e: WebDriverException) {
                logger.error("Get attribute failed | sessionId={} elementId={} name={} | {}", sessionId, elementId, name, e.message)
                ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
            } catch (e: Exception) {
                logger.error("Get attribute failed | sessionId={} elementId={} name={} | {}", sessionId, elementId, name, e.message, e)
                ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
            }
        }

        if (!store.sessionExists(sessionId)) {
            return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
        }

        val element = store.getElement(sessionId, elementId)
            ?: return ControllerUtils.notFound("no such element", "Element with id $elementId not found")

        // Mock implementation - return from attributes map or generate mock value
        val value = element.attributes[name] ?: "mock-$name-value"
        return ResponseEntity.ok(AttributeResponse(value = value))
    }

    /**
     * Gets an element's text content.
     */
    @GetMapping("/element/{elementId}/text")
    fun getElementText(
        @PathVariable sessionId: String,
        @PathVariable elementId: String,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} getting text from element: {}", sessionId, elementId)
        ControllerUtils.addRequestId(response)

        if (useRealSessions) {
            val managed = sessionManager!!.getSession(sessionId)
                ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

            val element = store.getElement(sessionId, elementId)
                ?: return ControllerUtils.notFound("no such element", "Element with id $elementId not found")

            return try {
                val text = runBlocking {
                    val driver = managed.pulsarSession.getOrCreateBoundDriver()
                    driver.selectFirstTextOrNull(element.selector) ?: ""
                }
                ResponseEntity.ok(TextResponse(value = text))
            } catch (e: WebDriverException) {
                logger.error("Get text failed | sessionId={} elementId={} | {}", sessionId, elementId, e.message)
                ControllerUtils.errorResponse("webdriver error", e.message ?: "WebDriver error")
            } catch (e: Exception) {
                logger.error("Get text failed | sessionId={} elementId={} | {}", sessionId, elementId, e.message, e)
                ControllerUtils.errorResponse("internal error", e.message ?: "Internal error")
            }
        }

        if (!store.sessionExists(sessionId)) {
            return ControllerUtils.notFound("session not found", "No active session with id $sessionId")
        }

        val element = store.getElement(sessionId, elementId)
            ?: return ControllerUtils.notFound("no such element", "Element with id $elementId not found")

        return ResponseEntity.ok(TextResponse(value = element.text))
    }

    /**
     * Converts WebDriver locator strategy to CSS selector.
     */
    private fun convertToSelector(using: String, value: String): String {
        return when (using) {
            "css selector" -> value
            "xpath" -> value
            "id" -> "#$value"
            "name" -> "[name=\"$value\"]"
            "class name" -> ".$value"
            "tag name" -> value
            else -> value
        }
    }
}
