package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.warnUnexpected
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.ql.context.H2SQLContext
import ai.platon.pulsar.ql.h2.H2SQLSession
import ai.platon.pulsar.rest.api.entities.*
import ai.platon.pulsar.rest.api.service.CommandService
import ai.platon.pulsar.rest.api.service.ConversationService
import ai.platon.pulsar.skeleton.PulsarSettings
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.skeleton.session.PulsarSession
import kotlinx.coroutines.runBlocking
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
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
    val session: PulsarSession,
    val conversationService: ConversationService,
    val commandService: CommandService,
) {
    private var browser: Browser? = null
    private val browserLock = ReentrantLock()
    private var isInitialized = false // 添加初始化标志

    /**
     * Initialize Browser4 for SPA rendering and perform a simple health check.
     *
     * Steps
     * 1) Validate that the [session] is an active [H2SQLSession] with [H2SQLContext].
     * 2) Ensure a real browser is configured via [PulsarSettings.withDefaultBrowser].
     * 3) If this is the first initialization, attach a launch handler to capture the browser instance,
     *    then load Baidu and check the rendered HTML for a known marker.
     *
     * @return JSON status map indicating initialized/healthy/error.
     */
    @GetMapping("init")
    fun init(): Map<String, String> {
        browserLock.withLock {
            try {
                require(session.context is H2SQLContext)
                require(session.isActive)

                // Use a real browser for SPA rendering
                PulsarSettings().withDefaultBrowser()

                // 如果已经初始化过，直接返回健康状态
                if (isInitialized && browser != null) {
                    return mapOf(
                        "status" to "healthy",
                        "message" to "Browser4 already initialized"
                    )
                }

                // 只有在未初始化时才进行初始化
                if (!isInitialized) {
                    val options = session.options("-refresh")
                    options.eventHandlers.browseEventHandlers.onBrowserLaunched.addLast { _, driver ->
                        ensureBrowser(driver.browser)
                    }
                    val page = session.load("https://www.baidu.com/", options)
                    val document = session.parse(page)
                    val html = document.html

                    if (html.length > 1_000 && html.contains("<input .+百度一下.+>".toRegex())) {
                        isInitialized = true // 标记为已初始化
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
    fun navigate(@RequestBody request: NavigateRequest): ResponseEntity<Any> {
        val command = CommandRequest(
            url = request.url,
        )
        val response = commandService.executeSync(command)
        return ResponseEntity.ok(response)
    }

    /**
     * Execute an interaction instruction on the current page via the active driver.
     *
     * @param request The action request including the instruction text.
     * @return 200 OK with empty body on success; 500 if no active driver is present.
     */
    @PostMapping("/act")
    fun act(@RequestBody request: ActRequest): ResponseEntity<Any> {
        val driver = getActiveDriver()
        if (driver == null) {
            val status = CommandStatus.failed(ai.platon.pulsar.common.ResourceStatus.SC_SERVICE_UNAVAILABLE)
            status.message = "No active browser driver. Initialize via /api/spa/init and navigate first."
            return ResponseEntity.status(status.statusCode).body(status)
        }

        return try {
            runBlocking { driver.instruct(request.act) }
            ResponseEntity.ok("")
        } catch (e: Throwable) {
            warnUnexpected(this, e, "Failed to execute act on current page")
            val status = CommandStatus.failed(ResourceStatus.SC_INTERNAL_SERVER_ERROR)
            status.message = e.message ?: "Failed to act"
            ResponseEntity.status(status.statusCode).body(status)
        }
    }

    /**
     * Capture a screenshot of the current page.
     *
     * @param request Screenshot options (reserved for future use).
     * @return 200 OK with screenshot payload; 500 if no active driver is present.
     */
    @GetMapping("/screenshot")
    fun screenshot(@RequestBody request: ScreenshotRequest): ResponseEntity<Any> {
        val driver = getActiveDriver()
        if (driver == null) {
            val status = CommandStatus.failed(ResourceStatus.SC_SERVICE_UNAVAILABLE)
            status.message = "No active browser driver. Initialize via /api/spa/init and navigate first."
            return ResponseEntity.status(status.statusCode).body(status)
        }

        return try {
            val screenshot = runBlocking { driver.captureScreenshot() }
            ResponseEntity.ok(screenshot)
        } catch (e: Throwable) {
            warnUnexpected(this, e, "Failed to capture screenshot from current page")
            val status = CommandStatus.failed(ResourceStatus.SC_INTERNAL_SERVER_ERROR)
            status.message = e.message ?: "Failed to capture screenshot"
            ResponseEntity.status(status.statusCode).body(status)
        }
    }

    /**
     * Extract data from the current page using the provided prompt/rules.
     *
     * Steps:
     * - Open the current URL with the active driver to obtain a [WebPage].
     * - Parse the page, then execute a step-by-step command with dataExtractionRules.
     *
     * @param request The extraction request including the prompt/rules.
     * @return 200 OK with the aggregated [CommandStatus]; 500 if no active driver is present.
     */
    @GetMapping("/extract")
    fun extract(@RequestBody request: ExtractRequest): ResponseEntity<Any> {
        val driver = getActiveDriver() ?: return ResponseEntity.internalServerError().build<Any>()

        val status = CommandStatus()

        runBlocking {
            val page: WebPage = session.open(driver.currentUrl(), driver)
            val document = session.parse(page)
            val command = CommandRequest(url = page.url, dataExtractionRules = request.prompt)
            commandService.executeCommandStepByStep(
                page,
                document,
                command,
                status
            )
        }

        return ResponseEntity.ok(status)
    }

    /**
     * Get the latest active [WebDriver] from the currently tracked [browser] and bind it to [session].
     *
     * Contract
     * - Thread-safe via [browserLock].
     * - If a driver is found, it binds the driver to the current session.
     *
     * @return The most recent driver if available; otherwise `null`.
     */
    private fun getActiveDriver(): WebDriver? {
        return browserLock.withLock {
            val driver = browser?.drivers?.values?.lastOrNull()
            if (driver != null) {
                session.bindDriver(driver)
            }
            driver
        }
    }

    /**
     * Guard that the given [browser] is configured for SPA and bind it to the [session].
     *
     * Preconditions
     * - Requires: `browser.settings.isSPA == true`. Otherwise, throws [IllegalArgumentException].
     *
     * Side effects
     * - Binds the browser into the session and stores it to the controller state.
     */
    private fun ensureBrowser(browser: Browser) {
        require(browser.settings.isSPA) { "The browser is not configured for SPA rendering" }

        browserLock.withLock {
            session.bindBrowser(browser)
            this.browser = browser
        }
    }
}
