package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.common.browser.BrowserContextMode
import ai.platon.pulsar.common.catastrophicError
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

    private fun getActiveDriver(): WebDriver? {
        return browserLock.withLock {
            val driver = browser?.drivers?.values?.lastOrNull()
            if (driver != null) {
                session.bindDriver(driver)
            }
            driver
        }
    }

    private fun ensureBrowser(browser: Browser) {
        require(browser.settings.isSPA) { "The browser is not configured for SPA rendering" }

        browserLock.withLock {
            session.bindBrowser(browser)
            this.browser = browser
        }
    }

    @GetMapping("init")
    fun init(): Map<String, String> {
        browserLock.withLock {
            try {
                require(session is H2SQLSession)
                require(session.context is H2SQLContext)
                require(session.isActive)

                // Use a real browser for SPA rendering
                PulsarSettings().withDefaultBrowser()

                if (browser == null) {
                    val options = session.options("-refresh")
                    options.eventHandlers.browseEventHandlers.onBrowserLaunched.addLast { page, driver ->
                        ensureBrowser(driver.browser)
                    }
                    val page = session.load("https://www.baidu.com/", options)
                    val document = session.parse(page)
                    val html = document.html

                    if (html.length > 1_000 && html.contains("<input .+百度一下.+>".toRegex())) {
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
                catastrophicError(e, "Failed to initialize Browser4")
                return mapOf(
                    "status" to "error",
                    "message" to "Failed to initialize Browser4"
                )
            }
        }
    }

    @PostMapping("/navigate")
    fun navigate(@RequestBody request: NavigateRequest): ResponseEntity<Any> {
        val command = CommandRequest(
            url = request.url,
        )
        val response = commandService.executeSync(command)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/act")
    fun act(@RequestBody request: ActRequest): ResponseEntity<Any> {
        val driver = getActiveDriver() ?: return ResponseEntity.internalServerError().build<Any>()

        runBlocking { driver.instruct(request.act) }

        return ResponseEntity.ok("")
    }

    @GetMapping("/screenshot")
    fun screenshot(@RequestBody request: ScreenshotRequest): ResponseEntity<Any> {
        val driver = getActiveDriver() ?: return ResponseEntity.internalServerError().build<Any>()

        val screenshot = runBlocking { driver.captureScreenshot() }

        return ResponseEntity.ok(screenshot)
    }

    @GetMapping("/extract")
    fun extract(@RequestBody request: ExtractRequest): ResponseEntity<Any> {
        val driver = getActiveDriver() ?: return ResponseEntity.internalServerError().build<Any>()

        val status = CommandStatus()

        runBlocking {
            val page: WebPage = session.open(driver.currentUrl(), driver)
            val document = session.parse(page)
            val command = CommandRequest(url = page.url)
            commandService.executeCommandStepByStep(
                page,
                document,
                command,
                status
            )
        }

        return ResponseEntity.ok(status)
    }
}
