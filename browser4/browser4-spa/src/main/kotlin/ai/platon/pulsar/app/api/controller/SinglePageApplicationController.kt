package ai.platon.pulsar.app.api.controller

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.ai.agent.detail.ActResultHelper
import ai.platon.pulsar.agentic.context.QLAgenticContext
import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.ResourceStatus.SC_INTERNAL_SERVER_ERROR
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.URLUtils
import ai.platon.pulsar.common.warnUnexpected
import ai.platon.pulsar.rest.api.entities.NavigateRequest
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.File

/**
 * Contract-aligned REST controller for SPA automation endpoints.
 *
 * This controller is designed to match the API contract defined in `browserApi.ts`:
 * - GET  /api                    (health)
 * - POST /api/init               (init)
 * - POST /api/navigate           (navigate)
 * - POST /api/screenshot         (screenshot)
 * - POST /api/act                (act)
 * - POST /api/extract            (extract)
 * - POST /api/convert-svg        (convert svg -> png)
 */
@RestController
@CrossOrigin
@RequestMapping(
    "api",
    consumes = [MediaType.ALL_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class SinglePageApplicationController(
    val session: AgenticSession,
) {
    private val logger = getLogger(SinglePageApplicationController::class)

    private val agent get() = session.companionAgent
    private val activeDriver: WebDriver get() = session.getOrCreateBoundDriver()

    private var isInitialized = false

    data class BrowserActionResult(
        val success: Boolean,
        val message: String,
        val error: String? = null,
        val url: String = "",
        val title: String = "",
        val screenshot_base64: String? = null,
        val action: String? = null,
    )

    data class InitRequest(
        val api_key: String? = null,
    )

    data class HealthResponse(
        val status: String,
        val service: String = "browserApi",
    )

    data class ErrorResponse(
        val status: String = "error",
        val message: String,
    )

    data class ScreenshotRequestCompat(
        val id: String? = null,
    )

    data class ExtractRequestCompat(
        val instruction: String,
        val iframes: Boolean? = null,
    )

    data class ConvertSvgRequest(
        val svg_file_path: String,
    )

    private data class PageInfo(
        val url: String,
        val title: String,
        val screenshotBase64: String,
    )

    private fun isHealthy(): Boolean {
        if (!isInitialized) return false
        return try {
            session.isActive
        } catch (_: Throwable) {
            false
        }
    }

    private suspend fun capturePageInfoOrEmpty(): PageInfo {
        return try {
            val driver = activeDriver
            driver.bringToFront()

            val url = runCatching { driver.currentUrl() }.getOrDefault("")
            val title = runCatching { driver.title() }.getOrDefault("")
            val screenshotBase64 = runCatching { driver.captureScreenshot() }.getOrDefault("") ?: ""

            PageInfo(url, title, screenshotBase64)
        } catch (_: Throwable) {
            PageInfo("", "", "")
        }
    }

    private fun notInitializedResponse(message: String): ResponseEntity<BrowserActionResult> {
        return ResponseEntity.status(SC_INTERNAL_SERVER_ERROR).body(
            BrowserActionResult(
                success = false,
                message = "Browser not initialized",
                error = message,
                url = "",
                title = "",
            )
        )
    }

    private suspend fun failWithPageInfo(message: String, error: Throwable? = null): ResponseEntity<BrowserActionResult> {
        // When not initialized, browserApi.ts returns empty url/title and does not attempt further capture.
        if (!isInitialized) {
            return notInitializedResponse(error?.message ?: "Browser must be initialized before performing actions")
        }

        val pageInfo = capturePageInfoOrEmpty()
        val errText = error?.message ?: error?.toString()
        return ResponseEntity.status(SC_INTERNAL_SERVER_ERROR).body(
            BrowserActionResult(
                success = false,
                message = message,
                error = errText,
                url = pageInfo.url,
                title = pageInfo.title,
                screenshot_base64 = pageInfo.screenshotBase64,
            )
        )
    }

    @GetMapping
    fun health(): ResponseEntity<Any> {
        return if (isHealthy()) {
            ResponseEntity.ok(HealthResponse(status = "healthy"))
        } else {
            ResponseEntity.status(SC_INTERNAL_SERVER_ERROR).body(HealthResponse(status = "unhealthy"))
        }
    }

    @PostMapping("/init")
    fun init(@RequestBody(required = false) request: InitRequest?): ResponseEntity<Any> {
        return try {
            // Align with browserApi.ts: accept api_key but do not hard-fail if not provided.
            // If future implementation needs it, wire it into session/model config.
            @Suppress("UNUSED_VARIABLE")
            val apiKey = request?.api_key

            require(session.context is QLAgenticContext) { "Invalid session context" }
            require(session.isActive) { "Session is not active" }

            if (!isInitialized) {
                // Trigger driver creation as the "init" side effect.
                session.getOrCreateBoundDriver()
                isInitialized = true
            }

            ResponseEntity.ok(HealthResponse(status = "healthy"))
        } catch (e: Throwable) {
            warnUnexpected(this, e, "Failed to initialize Browser4")
            ResponseEntity.status(SC_INTERNAL_SERVER_ERROR).body(ErrorResponse(message = e.message ?: "Failed to initialize browser"))
        }
    }

    @PostMapping("/navigate")
    suspend fun navigate(@RequestBody request: NavigateRequest): ResponseEntity<BrowserActionResult> {
        val url = request.url

        return try {
            if (!isInitialized) {
                return notInitializedResponse("Browser must be initialized before navigation")
            }
            require(URLUtils.isStandard(url)) { "URL is not valid | $url" }

            val driver = activeDriver
            val oldUrl = driver.currentUrl()

            driver.bringToFront()
            driver.navigateTo(url)
            driver.waitForNavigation(oldUrl)
            driver.waitForSelector("body")

            val pageInfo = capturePageInfoOrEmpty()
            ResponseEntity.ok(
                BrowserActionResult(
                    success = true,
                    message = "Navigated to $url",
                    error = "",
                    url = pageInfo.url,
                    title = pageInfo.title,
                    screenshot_base64 = pageInfo.screenshotBase64,
                )
            )
        } catch (e: Throwable) {
            warnUnexpected(this, e, "Failed to navigate to $url")
            failWithPageInfo("Failed to navigate to $url", e)
        }
    }

    @PostMapping("/screenshot")
    suspend fun screenshot(@RequestBody(required = false) request: ScreenshotRequestCompat?): ResponseEntity<BrowserActionResult> {
        return try {
            if (!isInitialized) {
                return notInitializedResponse("Browser must be initialized before taking screenshot")
            }
            val pageInfo = capturePageInfoOrEmpty()
            ResponseEntity.ok(
                BrowserActionResult(
                    success = true,
                    message = "Screenshot taken",
                    url = pageInfo.url,
                    title = pageInfo.title,
                    screenshot_base64 = pageInfo.screenshotBase64,
                )
            )
        } catch (e: Throwable) {
            warnUnexpected(this, e, "Failed to take screenshot")
            failWithPageInfo("Failed to take screenshot", e)
        }
    }

    @PostMapping("/act")
    suspend fun act(@RequestBody request: ai.platon.pulsar.agentic.ActionOptions): ResponseEntity<BrowserActionResult> {
        return try {
            if (!isInitialized) {
                return notInitializedResponse("Browser must be initialized before performing actions")
            }

            val result = agent.act(request)
            val pageInfo = capturePageInfoOrEmpty()

            ResponseEntity.ok(
                BrowserActionResult(
                    success = result.success,
                    message = result.message,
                    action = result.action,
                    url = pageInfo.url,
                    title = pageInfo.title,
                    screenshot_base64 = pageInfo.screenshotBase64,
                )
            )
        } catch (e: Throwable) {
            warnUnexpected(this, e, "Failed to execute act on current page")
            val message = e.message ?: "Failed to act | ${request.action}"
            val statusCode = SC_INTERNAL_SERVER_ERROR
            val failure = ActResultHelper.failed(ResourceStatus.getStatusText(statusCode) + " | " + message)
            failWithPageInfo("Failed to act", e)
        }
    }

    @PostMapping("/extract")
    suspend fun extract(@RequestBody request: ExtractRequestCompat): ResponseEntity<BrowserActionResult> {
        return try {
            if (!isInitialized) {
                return notInitializedResponse("Browser must be initialized before extracting data")
            }

            // Map browserApi.ts request to internal ExtractOptions.
            // The internal API requires a schema; use a permissive schema by default.
            val schema = ai.platon.pulsar.agentic.ExtractionSchema.fromMap(mapOf("type" to "object"))
            val options = ai.platon.pulsar.agentic.ExtractOptions(
                instruction = request.instruction,
                schema = schema,
                iframes = request.iframes,
            )

            val result = agent.extract(options)
            val pageInfo = capturePageInfoOrEmpty()

            ResponseEntity.ok(
                BrowserActionResult(
                    success = result.success,
                    message = "Extracted result for: ${request.instruction}",
                    action = result.data.toString(),
                    url = pageInfo.url,
                    title = pageInfo.title,
                    screenshot_base64 = pageInfo.screenshotBase64,
                )
            )
        } catch (e: Throwable) {
            warnUnexpected(this, e, "Failed to extract")
            failWithPageInfo("Failed to extract", e)
        }
    }

    @PostMapping("/convert-svg")
    suspend fun convertSvg(@RequestBody request: ConvertSvgRequest): ResponseEntity<BrowserActionResult> {
        val svgFilePath = request.svg_file_path

        return try {
            if (!isInitialized) {
                return notInitializedResponse("Browser must be initialized before converting SVG")
            }

            if (svgFilePath.isBlank()) {
                return ResponseEntity.badRequest().body(
                    BrowserActionResult(
                        success = false,
                        message = "SVG file path is required",
                        error = "svg_file_path parameter is missing",
                        url = "",
                        title = "",
                    )
                )
            }

            val file = File(svgFilePath)
            require(file.exists()) { "SVG file does not exist | $svgFilePath" }

            val unixPath = file.absolutePath.replace('\\', '/')
            val fileUrl = "file:///$unixPath"

            val driver = activeDriver
            val oldUrl = driver.currentUrl()

            driver.bringToFront()
            driver.navigateTo(fileUrl)
            driver.waitForNavigation(oldUrl)
            driver.waitForSelector("svg")

            // We can't reliably capture element-only screenshot with current WebDriver API;
            // use page screenshot as the PNG base64 output.
            val screenshotBase64 = driver.captureScreenshot() ?: ""
            val pageInfo = capturePageInfoOrEmpty()

            ResponseEntity.ok(
                BrowserActionResult(
                    success = true,
                    message = "Successfully converted SVG to PNG: $svgFilePath",
                    url = pageInfo.url,
                    title = pageInfo.title,
                    screenshot_base64 = if (screenshotBase64.isNotBlank()) screenshotBase64 else pageInfo.screenshotBase64,
                )
            )
        } catch (e: Throwable) {
            warnUnexpected(this, e, "Failed to convert SVG")
            failWithPageInfo("Failed to convert SVG", e)
        }
    }
}
