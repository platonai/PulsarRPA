package ai.platon.pulsar.app.api.controller

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.ai.agent.detail.ActResultHelper
import ai.platon.pulsar.agentic.context.QLAgenticContext
import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.ResourceStatus.SC_INTERNAL_SERVER_ERROR
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.URLUtils
import ai.platon.pulsar.common.warnUnexpected
import ai.platon.pulsar.rest.api.entities.CommandStatus
import ai.platon.pulsar.rest.api.entities.NavigateRequest
import ai.platon.pulsar.rest.api.entities.ScreenshotRequest
import ai.platon.pulsar.agentic.ActResult
import ai.platon.pulsar.agentic.ActionOptions
import ai.platon.pulsar.agentic.ExtractOptions
import ai.platon.pulsar.agentic.ExtractResult
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * REST controller providing SPA (Single Page Application) automation endpoints backed by Browser4.
 *
 * - Base path: `api/spa`
 * - Produces: JSON; Consumes: any
 * - Thread-safety: [browser] and driver binding operations are guarded by [browserLock].
 * - Side effects: may launch and bind a real browser instance to the [session].
 */
@RestController
@CrossOrigin
@RequestMapping(
    "api/spa",
    consumes = [MediaType.ALL_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class SinglePageApplicationController(
    val session: AgenticSession,
) {
    private val logger = getLogger(SinglePageApplicationController::class)
    private var browser: Browser? = null
    private val agent get() = session.companionAgent
    private val activeDriver: WebDriver get() = session.getOrCreateBoundDriver()
    private val browserLock = ReentrantLock()
    private var isInitialized = false

    /**
     * Initialize Browser4 for SPA rendering and perform a simple health check.
     *
     * Steps
     * 1) Validate that the [session] is an active [ai.platon.pulsar.ql.h2.H2SQLSession] with [ai.platon.pulsar.ql.context.H2SQLContext].
     * 2) Ensure a real browser is configured via [ai.platon.pulsar.skeleton.PulsarSettings.withDefaultBrowser].
     * 3) If this is the first initialization, attach a launch handler to capture the browser instance,
     *    then load Baidu and check the rendered HTML for a known marker.
     *
     * @return JSON status map indicating initialized/healthy/error.
     */
    @GetMapping("init")
    fun init(): Map<String, String> {
        browserLock.withLock {
            try {
                require(session.context is QLAgenticContext)
                require(session.isActive)

                // 如果已经初始化过，直接返回健康状态
                if (isInitialized && browser != null) {
                    return mapOf(
                        "status" to "healthy",
                        "message" to "Browser4 already initialized"
                    )
                }

                if (!isInitialized) {
                    val options = session.options("-refresh")

                    val url = AppConstants.SEARCH_ENGINE_URL
                    logger.info("Verify $url to initialize ...")
                    val page = session.load(url, options)
                    val document = session.parse(page)
                    val html = document.html

                    if (html.length > 1_000) {
                        isInitialized = true
                        return mapOf(
                            "status" to "initialized",
                            "message" to "Browser4 initialized successfully"
                        )
                    }
                }

                return mapOf(
                    "status" to "healthy",
                    "message" to "Browser4 initialized"
                )
            } catch (e: Throwable) {
                warnUnexpected(this, e, "Failed to initialize Browser4")
                return mapOf(
                    "status" to "error",
                    "message" to "Failed to initialize Browser4: ${e.message}"
                )
            }
        }
    }

    /**
     * Navigate to a URL through the command execution pipeline.
     *
     * @param request The navigation request containing the target URL.
     * @return 200 OK with the command execution result as body.
     */
    @PostMapping("/navigate")
    suspend fun navigate(@RequestBody request: NavigateRequest): ResponseEntity<Any> {
        val url = request.url
        val driver = activeDriver

        return try {
            require(URLUtils.isStandard(url)) { "URL is not valid | $url" }

            val oldUrl = driver.currentUrl()

            driver.bringToFront()
            driver.navigateTo(url)
            driver.waitForNavigation(oldUrl)
            driver.waitForSelector("body")

            ResponseEntity.ok("success")
        } catch (e: Exception) {
            warnUnexpected(this, e, "Failed to navigate to ${request.url}")
            val message = e.message ?: "Failed to navigate | ${request.url}"
            val statusCode = SC_INTERNAL_SERVER_ERROR
            val result = ResourceStatus.getStatusText(statusCode) + " | " + message
            ResponseEntity.status(statusCode).body(result)
        }
    }

    /**
     * Execute an interaction instruction on the current page via the active driver.
     * Supports a per-endpoint timeout via property `browser4.spa.act.timeout.ms` (milliseconds).
     * Set to a positive value to enable, or <=0 to disable custom timeout.
     */
    @PostMapping("/act")
    suspend fun act(@RequestBody request: ActionOptions): ResponseEntity<ActResult> {
        return try {
            val result = agent.act(request)

            ResponseEntity.ok(result)
        } catch (e: Throwable) {
            warnUnexpected(this, e, "Failed to execute act on current page")
            val message = e.message ?: "Failed to act | ${request.action}"
            val statusCode = SC_INTERNAL_SERVER_ERROR
            val actResult = ActResultHelper.failed(ResourceStatus.getStatusText(statusCode) + " | " + message)
            ResponseEntity.status(statusCode).body(actResult)
        }
    }

    /**
     * Capture a screenshot of the current page.
     *
     * @param request Screenshot options (reserved for future use).
     * @return 200 OK with JPEG image data in binary format; 500 if no active driver is present.
     */
    @GetMapping("/screenshot", produces = [MediaType.IMAGE_JPEG_VALUE])
    suspend fun screenshot(@RequestBody request: ScreenshotRequest): ResponseEntity<Any> {
        val driver = activeDriver

        return try {
            driver.bringToFront()
            val screenshotBase64 = driver.captureScreenshot()
                ?: return ResponseEntity.status(SC_INTERNAL_SERVER_ERROR)
                    .body("Failed to capture screenshot.")

            // Decode base64 to bytes and return raw JPEG data
            val jpegBytes = Base64.getDecoder().decode(screenshotBase64)
            ResponseEntity.ok(jpegBytes)
        } catch (e: Throwable) {
            warnUnexpected(this, e, "Failed to capture screenshot from current page")
            val status = CommandStatus.failed(SC_INTERNAL_SERVER_ERROR)
            status.message = e.message ?: "Failed to capture screenshot"
            ResponseEntity.status(status.statusCode).body(status)
        }
    }

    /**
     * Extract data from the current page using the provided prompt/rules.
     *
     * Steps:
     * - Open the current URL with the active driver to obtain a [ai.platon.pulsar.persist.WebPage].
     * - Parse the page, then execute a step-by-step command with dataExtractionRules.
     *
     * @param options The extraction options including the prompt/rules.
     * @return 200 OK with the aggregated [ExtractResult]; 500 if no active driver is present.
     */
    @GetMapping("/extract")
    suspend fun extract(@RequestBody options: ExtractOptions): ResponseEntity<ExtractResult> {
        val result = agent.extract(options)
        return ResponseEntity.ok(result)
    }
}
