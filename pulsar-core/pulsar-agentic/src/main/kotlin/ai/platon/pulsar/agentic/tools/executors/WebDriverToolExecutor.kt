package ai.platon.pulsar.agentic.tools.executors

import ai.platon.pulsar.agentic.tools.ActionValidator
import ai.platon.pulsar.agentic.tools.BasicToolCallExecutor.Companion.esc
import ai.platon.pulsar.agentic.tools.BasicToolCallExecutor.Companion.norm
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.ToolCall
import ai.platon.pulsar.skeleton.crawl.fetch.driver.NavigateEntry
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import java.time.Duration
import kotlin.reflect.KClass

class WebDriverToolExecutor: AbstractToolExecutor() {
    private val logger = getLogger(this)

    override val domain = "driver"

    override val targetClass: KClass<*> = WebDriver::class

    @Deprecated("Not used anymore")
    @Throws(IllegalArgumentException::class)
    override fun toExpression(tc: ToolCall): String {
        return Companion.toExpression(tc)
    }

    /**
     * Execute a WebDriver function by name with named arguments.
     * args: (parameterName -> value). If provided names don't match a supported signature, throw IllegalArgumentException.
     */
    @Suppress("UNUSED_PARAMETER")
    @Throws(IllegalArgumentException::class)
    override suspend fun execute(
        objectName: String, functionName: String, args: Map<String, Any?>, target: Any
    ): Any? {
        val driver = requireNotNull(target as? WebDriver) { "The target must be a WebDriver" }

        fun value(name: String): Any? = args[name]
        fun str(name: String, required: Boolean = true): String? {
            val v = value(name) ?: return if (required) throw IllegalArgumentException("Missing parameter '$name' for $functionName") else null
            return v.toString()
        }
        fun int(name: String, required: Boolean = true, default: Int? = null): Int {
            val raw = value(name)
            if (raw == null) {
                if (required) throw IllegalArgumentException("Missing parameter '$name' for $functionName")
                return default ?: 0
            }
            return raw.toString().toIntOrNull() ?: throw IllegalArgumentException("Parameter '$name' must be an Int for $functionName | actual='${raw}'")
        }
        fun long(name: String, required: Boolean = true, default: Long? = null): Long {
            val raw = value(name)
            if (raw == null) {
                if (required) throw IllegalArgumentException("Missing parameter '$name' for $functionName")
                return default ?: 0L
            }
            return raw.toString().toLongOrNull() ?: throw IllegalArgumentException("Parameter '$name' must be a Long for $functionName | actual='${raw}'")
        }
        fun double(name: String, required: Boolean = true, default: Double? = null): Double {
            val raw = value(name)
            if (raw == null) {
                if (required) throw IllegalArgumentException("Missing parameter '$name' for $functionName")
                return default ?: 0.0
            }
            return raw.toString().toDoubleOrNull() ?: throw IllegalArgumentException("Parameter '$name' must be a Double for $functionName | actual='${raw}'")
        }
        fun bool(name: String, required: Boolean = true, default: Boolean? = null): Boolean {
            val raw = value(name)
            if (raw == null) {
                if (required) throw IllegalArgumentException("Missing parameter '$name' for $functionName")
                return default ?: false
            }
            return when (val s = raw.toString().lowercase()) {
                "true" -> true
                "false" -> false
                else -> throw IllegalArgumentException("Parameter '$name' must be a Boolean for $functionName | actual='${raw}'")
            }
        }

        fun validateExact(allowed: Set<String>, required: Set<String> = allowed) {
            // required subset check
            required.forEach { if (!args.containsKey(it)) throw IllegalArgumentException("Missing required parameter '$it' for $functionName") }
            // extraneous check
            args.keys.forEach { if (it !in allowed) throw IllegalArgumentException("Extraneous parameter '$it' for $functionName. Allowed=$allowed") }
        }

        return when (functionName) {
            // Navigation
            "open" -> {
                validateExact(setOf("url"))
                driver.open(str("url")!!)
            }
            "navigateTo" -> {
                when {
                    args.containsKey("url") -> {
                        validateExact(setOf("url"))
                        driver.navigateTo(str("url")!!)
                    }
                    // entry navigation (rare in tool usage) expects: rawUrl, pageUrl
                    args.containsKey("rawUrl") || args.containsKey("pageUrl") -> {
                        validateExact(setOf("rawUrl", "pageUrl"), required = setOf("rawUrl", "pageUrl"))
                        val rawUrl = str("rawUrl")!!
                        val pageUrl = str("pageUrl")!!
                        driver.navigateTo(NavigateEntry(rawUrl, pageUrl = pageUrl))
                    }
                    else -> throw IllegalArgumentException("navigateTo requires 'url' or ('rawUrl','pageUrl')")
                }
            }
            "reload" -> { validateExact(emptySet()); driver.reload() }
            "goBack" -> { validateExact(emptySet()); driver.goBack() }
            "goForward" -> { validateExact(emptySet()); driver.goForward() }

            // Wait
            "waitForSelector" -> {
                when {
                    args.isEmpty() -> throw IllegalArgumentException("waitForSelector requires 'selector' (optional 'timeoutMillis')")
                    args.containsKey("selector") && !args.containsKey("timeoutMillis") -> {
                        validateExact(setOf("selector"))
                        driver.waitForSelector(str("selector")!!)
                    }
                    args.containsKey("selector") && args.containsKey("timeoutMillis") -> {
                        validateExact(setOf("selector", "timeoutMillis"))
                        driver.waitForSelector(str("selector")!!, long("timeoutMillis"))
                    }
                    else -> throw IllegalArgumentException("waitForSelector requires 'selector' (optional 'timeoutMillis')")
                }
            }
            "waitForNavigation" -> {
                when {
                    args.isEmpty() -> driver.waitForNavigation()
                    args.containsKey("oldUrl") && !args.containsKey("timeoutMillis") -> {
                        validateExact(setOf("oldUrl"))
                        driver.waitForNavigation(str("oldUrl")!!)
                    }
                    args.containsKey("oldUrl") && args.containsKey("timeoutMillis") -> {
                        validateExact(setOf("oldUrl", "timeoutMillis"))
                        driver.waitForNavigation(str("oldUrl")!!, long("timeoutMillis"))
                    }
                    else -> throw IllegalArgumentException("waitForNavigation requires 'oldUrl' (optional 'timeoutMillis')")
                }
            }
            "waitForPage" -> {
                when {
                    args.containsKey("url") && args.containsKey("timeoutMillis") -> {
                        validateExact(setOf("url", "timeoutMillis"))
                        driver.waitForPage(str("url")!!, Duration.ofMillis(long("timeoutMillis")))
                    }
                    args.containsKey("url") -> {
                        validateExact(setOf("url"))
                        driver.waitForPage(str("url")!!, Duration.ofMillis(30000))
                    }
                    else -> throw IllegalArgumentException("waitForPage requires 'url' (optional 'timeoutMillis')")
                }
            }

            // Status checking
            "exists" -> { validateExact(setOf("selector")); driver.exists(str("selector")!!) }
            "isVisible" -> { validateExact(setOf("selector")); driver.isVisible(str("selector")!!) }
            "visible" -> { validateExact(setOf("selector")); driver.isVisible(str("selector")!!) }
            "isHidden" -> { validateExact(setOf("selector")); driver.isHidden(str("selector")!!) }
            "isChecked" -> { validateExact(setOf("selector")); driver.isChecked(str("selector")!!) }

            // Interactions
            "focus" -> { validateExact(setOf("selector")); driver.focus(str("selector")!!) }
            "type" -> { validateExact(setOf("selector", "text")); driver.type(str("selector")!!, str("text")!!) }
            "fill" -> { validateExact(setOf("selector", "text")); driver.fill(str("selector")!!, str("text")!!) }
            "press" -> { validateExact(setOf("selector", "key")); driver.press(str("selector")!!, str("key")!!) }
            "click" -> {
                // click(selector, count?) OR click(selector, modifier)
                when {
                    args.containsKey("selector") && args.containsKey("count") && !args.containsKey("modifier") -> {
                        validateExact(setOf("selector", "count"))
                        driver.click(selector = str("selector")!!, count = int("count"))
                    }
                    args.containsKey("selector") && args.containsKey("modifier") && !args.containsKey("count") -> {
                        validateExact(setOf("selector", "modifier"))
                        driver.click(selector = str("selector")!!, modifier = str("modifier")!!)
                    }
                    args.containsKey("selector") && !args.containsKey("count") && !args.containsKey("modifier") -> {
                        validateExact(setOf("selector"))
                        driver.click(selector = str("selector")!!)
                    }
                    else -> throw IllegalArgumentException("click requires 'selector' plus optionally one of 'count' or 'modifier'")
                }
            }
            "clickTextMatches" -> {
                when {
                    args.containsKey("selector") && args.containsKey("pattern") && args.containsKey("count") -> {
                        validateExact(setOf("selector", "pattern", "count"))
                        driver.clickTextMatches(selector = str("selector")!!, pattern = str("pattern")!!, count = int("count"))
                    }
                    args.containsKey("selector") && args.containsKey("pattern") -> {
                        validateExact(setOf("selector", "pattern"))
                        driver.clickTextMatches(selector = str("selector")!!, pattern = str("pattern")!!)
                    }
                    else -> throw IllegalArgumentException("clickTextMatches requires 'selector','pattern' (optional 'count')")
                }
            }
            "clickMatches" -> {
                when {
                    args.containsKey("selector") && args.containsKey("attrName") && args.containsKey("pattern") && args.containsKey("count") -> {
                        validateExact(setOf("selector", "attrName", "pattern", "count"))
                        driver.clickMatches(selector = str("selector")!!, attrName = str("attrName")!!, pattern = str("pattern")!!, count = int("count"))
                    }
                    args.containsKey("selector") && args.containsKey("attrName") && args.containsKey("pattern") -> {
                        validateExact(setOf("selector", "attrName", "pattern"))
                        driver.clickMatches(selector = str("selector")!!, attrName = str("attrName")!!, pattern = str("pattern")!!)
                    }
                    else -> throw IllegalArgumentException("clickMatches requires 'selector','attrName','pattern' (optional 'count')")
                }
            }
            "clickNthAnchor" -> {
                when {
                    args.containsKey("n") && args.containsKey("rootSelector") -> {
                        validateExact(setOf("n", "rootSelector"))
                        driver.clickNthAnchor(n = int("n"), rootSelector = str("rootSelector")!!)
                    }
                    args.containsKey("n") -> {
                        validateExact(setOf("n"))
                        driver.clickNthAnchor(n = int("n"))
                    }
                    else -> throw IllegalArgumentException("clickNthAnchor requires 'n' (optional 'rootSelector')")
                }
            }
            "check" -> { validateExact(setOf("selector")); driver.check(str("selector")!!) }
            "uncheck" -> { validateExact(setOf("selector")); driver.uncheck(str("selector")!!) }
            "scrollTo" -> { validateExact(setOf("selector")); driver.scrollTo(str("selector")!!) }
            "bringToFront" -> { validateExact(emptySet()); driver.bringToFront() }
            "chat" -> { validateExact(setOf("prompt", "selector")); driver.chat(prompt = str("prompt")!!, selector = str("selector")!!) }

            // Scrolling
            "scrollDown" -> { validateExact(if (args.isEmpty()) emptySet() else setOf("count")); driver.scrollDown(count = if (args.isEmpty()) 1 else int("count")) }
            "scrollUp" -> { validateExact(if (args.isEmpty()) emptySet() else setOf("count")); driver.scrollUp(count = if (args.isEmpty()) 1 else int("count")) }
            "scrollBy" -> {
                validateExact(setOf("pixels", "smooth"), required = emptySet())
                val pixels = (args["pixels"]?.toString()?.toDoubleOrNull()) ?: 200.0
                val smooth = args["smooth"]?.toString()?.lowercase()?.let { it == "true" } ?: true
                driver.scrollBy(pixels = pixels, smooth = smooth)
            }
            "scrollToTop" -> { validateExact(emptySet()); driver.scrollToTop() }
            "scrollToBottom" -> { validateExact(emptySet()); driver.scrollToBottom() }
            "scrollToMiddle" -> { validateExact(setOf("ratio")); driver.scrollToMiddle(double("ratio")) }
            "scrollToViewport" -> { validateExact(setOf("n")); driver.scrollToViewport(double("n")) }
            "scrollToScreen" -> { validateExact(setOf("screenNumber")); driver.scrollToViewport(double("screenNumber")) }

            // Mouse wheel / movement
            "mouseWheelDown" -> {
                // Optional named parameters: count, deltaX, deltaY, delayMillis
                validateExact(setOf("count", "deltaX", "deltaY", "delayMillis"), required = emptySet())
                driver.mouseWheelDown(
                    count = (args["count"]?.toString()?.toIntOrNull()) ?: 1,
                    deltaX = (args["deltaX"]?.toString()?.toDoubleOrNull()) ?: 0.0,
                    deltaY = (args["deltaY"]?.toString()?.toDoubleOrNull()) ?: 150.0,
                    delayMillis = (args["delayMillis"]?.toString()?.toLongOrNull()) ?: 0L
                )
            }
            "mouseWheelUp" -> {
                validateExact(setOf("count", "deltaX", "deltaY", "delayMillis"), required = emptySet())
                driver.mouseWheelUp(
                    count = (args["count"]?.toString()?.toIntOrNull()) ?: 1,
                    deltaX = (args["deltaX"]?.toString()?.toDoubleOrNull()) ?: 0.0,
                    deltaY = (args["deltaY"]?.toString()?.toDoubleOrNull()) ?: -150.0,
                    delayMillis = (args["delayMillis"]?.toString()?.toLongOrNull()) ?: 0L
                )
            }
            "moveMouseTo" -> {
                when {
                    args.containsKey("x") && args.containsKey("y") -> {
                        validateExact(setOf("x", "y"))
                        driver.moveMouseTo(double("x"), double("y"))
                    }
                    args.containsKey("selector") -> {
                        validateExact(setOf("selector", "deltaX", "deltaY"), required = setOf("selector"))
                        driver.moveMouseTo(str("selector")!!, int("deltaX", required = false, default = 0), int("deltaY", required = false, default = 0))
                    }
                    else -> throw IllegalArgumentException("moveMouseTo requires ('x','y') or 'selector' (+ optional deltaX, deltaY)")
                }
            }
            "dragAndDrop" -> {
                val allowed = mutableSetOf("selector", "deltaX", "deltaY")
                validateExact(allowed, required = setOf("selector", "deltaX"))
                driver.dragAndDrop(str("selector")!!, int("deltaX"), int("deltaY", required = false, default = 0))
            }
            "captureScreenshot" -> {
                when {
                    args.isEmpty() -> { validateExact(emptySet()); driver.captureScreenshot() }
                    args.containsKey("selector") -> { validateExact(setOf("selector")); driver.captureScreenshot(str("selector")!!) }
                    args.containsKey("fullPage") -> { validateExact(setOf("fullPage")); driver.captureScreenshot(bool("fullPage")) }
                    else -> throw IllegalArgumentException("captureScreenshot allows none or one of: 'selector' | 'fullPage'")
                }
            }

            // HTML / Text
            "outerHTML" -> {
                when {
                    args.isEmpty() -> { validateExact(emptySet()); driver.outerHTML() }
                    args.containsKey("selector") -> { validateExact(setOf("selector")); driver.outerHTML(str("selector")!!) }
                    else -> throw IllegalArgumentException("outerHTML takes zero args or 'selector'")
                }
            }
            "textContent" -> { validateExact(emptySet()); driver.textContent() }
            "nanoDOMTree" -> { validateExact(emptySet()); driver.nanoDOMTree() }
            "selectFirstTextOrNull" -> { validateExact(setOf("selector")); driver.selectFirstTextOrNull(str("selector")!!) }
            "selectTextAll" -> { validateExact(setOf("selector")); driver.selectTextAll(str("selector")!!) }
            "selectFirstAttributeOrNull" -> { validateExact(setOf("selector", "attrName")); driver.selectFirstAttributeOrNull(str("selector")!!, str("attrName")!!) }
            "selectAttributes" -> { validateExact(setOf("selector")); driver.selectAttributes(str("selector")!!) }
            "selectAttributeAll" -> {
                when {
                    args.containsKey("selector") && args.containsKey("attrName") && (args.containsKey("start") || args.containsKey("limit")) -> {
                        val allowed = mutableSetOf("selector", "attrName", "start", "limit")
                        validateExact(allowed)
                        driver.selectAttributeAll(
                            selector = str("selector")!!,
                            attrName = str("attrName")!!,
                            start = int("start", required = false, default = 0),
                            limit = int("limit", required = false, default = 10000)
                        )
                    }
                    args.containsKey("selector") && args.containsKey("attrName") -> {
                        validateExact(setOf("selector", "attrName"))
                        driver.selectAttributeAll(str("selector")!!, str("attrName")!!)
                    }
                    else -> throw IllegalArgumentException("selectAttributeAll requires 'selector','attrName' (optional 'start','limit')")
                }
            }
            "setAttribute" -> { validateExact(setOf("selector", "attrName", "attrValue")); driver.setAttribute(str("selector")!!, str("attrName")!!, str("attrValue")!!) }
            "setAttributeAll" -> { validateExact(setOf("selector", "attrName", "attrValue")); driver.setAttributeAll(str("selector")!!, str("attrName")!!, str("attrValue")!!) }

            // Property selection / set
            "selectFirstPropertyValueOrNull" -> { validateExact(setOf("selector", "propName")); driver.selectFirstPropertyValueOrNull(str("selector")!!, str("propName")!!) }
            "selectPropertyValueAll" -> {
                when {
                    args.containsKey("selector") && args.containsKey("propName") && (args.containsKey("start") || args.containsKey("limit")) -> {
                        val allowed = mutableSetOf("selector", "propName", "start", "limit")
                        validateExact(allowed)
                        driver.selectPropertyValueAll(
                            selector = str("selector")!!,
                            propName = str("propName")!!,
                            start = int("start", required = false, default = 0),
                            limit = int("limit", required = false, default = 10000)
                        )
                    }
                    args.containsKey("selector") && args.containsKey("propName") -> {
                        validateExact(setOf("selector", "propName"))
                        driver.selectPropertyValueAll(str("selector")!!, str("propName")!!)
                    }
                    else -> throw IllegalArgumentException("selectPropertyValueAll requires 'selector','propName' (optional 'start','limit')")
                }
            }
            "setProperty" -> { validateExact(setOf("selector", "propName", "propValue")); driver.setProperty(str("selector")!!, str("propName")!!, str("propValue")!!) }
            "setPropertyAll" -> { validateExact(setOf("selector", "propName", "propValue")); driver.setPropertyAll(str("selector")!!, str("propName")!!, str("propValue")!!) }

            // Hyperlinks / anchors / images
            "selectHyperlinks" -> {
                validateExact(setOf("selector", "offset", "limit"), required = setOf("selector"))
                driver.selectHyperlinks(
                    selector = str("selector")!!,
                    offset = int("offset", required = false, default = 1),
                    limit = int("limit", required = false, default = Int.MAX_VALUE)
                )
            }
            "selectAnchors" -> {
                validateExact(setOf("selector", "offset", "limit"), required = setOf("selector"))
                driver.selectAnchors(
                    selector = str("selector")!!,
                    offset = int("offset", required = false, default = 1),
                    limit = int("limit", required = false, default = Int.MAX_VALUE)
                )
            }
            "selectImages" -> {
                validateExact(setOf("selector", "offset", "limit"), required = setOf("selector"))
                driver.selectImages(
                    selector = str("selector")!!,
                    offset = int("offset", required = false, default = 1),
                    limit = int("limit", required = false, default = Int.MAX_VALUE)
                )
            }

            // JavaScript evaluation
            "evaluate" -> { validateExact(setOf("expression")); driver.evaluate(str("expression")!!) }
            "evaluateDetail" -> { validateExact(setOf("expression")); driver.evaluateDetail(str("expression")!!) }
            "evaluateValue" -> { validateExact(setOf("expression")); driver.evaluateValue(str("expression")!!) }
            "evaluateValueDetail" -> { validateExact(setOf("expression")); driver.evaluateValueDetail(str("expression")!!) }

            // Element geometry
            "clickablePoint" -> { validateExact(setOf("selector")); driver.clickablePoint(str("selector")!!) }
            "boundingBox" -> { validateExact(setOf("selector")); driver.boundingBox(str("selector")!!) }

            // Jsoup / resource
            "newJsoupSession" -> { validateExact(emptySet()); driver.newJsoupSession() }
            "loadJsoupResource" -> { validateExact(setOf("url")); driver.loadJsoupResource(str("url")!!) }
            "loadResource" -> { validateExact(setOf("url")); driver.loadResource(str("url")!!) }

            // Cookies
            "getCookies" -> { validateExact(emptySet()); driver.getCookies() }
            "deleteCookies" -> {
                // name mandatory; optionally url/domain/path
                validateExact(setOf("name", "url", "domain", "path"), required = setOf("name"))
                driver.deleteCookies(
                    name = str("name")!!,
                    url = str("url", required = false),
                    domain = str("domain", required = false),
                    path = str("path", required = false)
                )
            }
            "clearBrowserCookies" -> { validateExact(emptySet()); driver.clearBrowserCookies() }

            // Delay / pause / stop
            "delay" -> {
                validateExact(if (args.isEmpty()) emptySet() else setOf("millis"))
                driver.delay((args["millis"]?.toString()?.toLongOrNull()) ?: 1000L)
            }
            "pause" -> { validateExact(emptySet()); driver.pause() }
            "stop" -> { validateExact(emptySet()); driver.stop() }

            // URL & document info
            "currentUrl" -> { validateExact(emptySet()); driver.currentUrl() }
            "url" -> { validateExact(emptySet()); driver.url() }
            "documentURI" -> { validateExact(emptySet()); driver.documentURI() }
            "baseURI" -> { validateExact(emptySet()); driver.baseURI() }
            "referrer" -> { validateExact(emptySet()); driver.referrer() }
            "pageSource" -> { validateExact(emptySet()); driver.pageSource() }

            else -> throw IllegalArgumentException("Unsupported WebDriver tool function: $functionName")
        }
    }

    companion object {
        @Deprecated("Not used anymore")
        fun toExpression(tc: ToolCall): String {
            ActionValidator().validateToolCall(tc)

            val arguments = tc.arguments
            val expression = when (tc.method) {
                // Navigation
                "open" -> arguments["url"]?.let { "driver.open(${it.norm()})" }
                "navigateTo" -> arguments["url"]?.let { "driver.navigateTo(${it.norm()})" }
                "reload" -> "driver.reload()"
                "goBack" -> "driver.goBack()"
                "goForward" -> "driver.goForward()"
                // Wait
                "waitForSelector" -> arguments["selector"]?.let { sel ->
                    "driver.waitForSelector(${sel.norm()}, ${(arguments["timeoutMillis"] ?: 5000)})"
                }
                // Status checking (first batch of new tools)
                "exists" -> arguments["selector"]?.let { "driver.exists(${it.norm()})" }
                "isVisible" -> arguments["selector"]?.let { "driver.isVisible(${it.norm()})" }
                "focus" -> arguments["selector"]?.let { "driver.focus(${it.norm()})" }
                // Basic interactions
                "click" -> arguments["selector"]?.esc()?.let {
                    val modifier = arguments["modifier"]?.esc()
                    val count = arguments["count"]?.toIntOrNull() ?: 1
                    when {
                        modifier != null -> "driver.click(\"$it\", \"$modifier\")"
                        else -> "driver.click(\"$it\", $count)"
                    }
                }

                "fill" -> arguments["selector"]?.let { s ->
                    val text = arguments["text"]?.esc() ?: ""
                    "driver.fill(\"${s.esc()}\", \"$text\")"
                }

                "press" -> arguments["selector"]?.let { s ->
                    arguments["key"]?.let { k -> "driver.press(\"${s.esc()}\", \"${k.esc()}\")" }
                }

                "check" -> arguments["selector"]?.let { "driver.check(${it.norm()})" }
                "uncheck" -> arguments["selector"]?.let { "driver.uncheck(${it.norm()})" }
                // Scrolling
                "scrollDown" -> "driver.scrollDown(${arguments["count"] ?: 1})"
                "scrollUp" -> "driver.scrollUp(${arguments["count"] ?: 1})"
                "scrollBy" -> {
                    val pixels = (arguments["pixels"] ?: 200.0).toString()
                    val smooth = (arguments["smooth"] ?: true).toString()
                    "driver.scrollBy(${pixels}, ${smooth})"
                }

                "scrollTo" -> arguments["selector"]?.let { "driver.scrollTo(${it.norm()})" }
                "scrollToTop" -> "driver.scrollToTop()"
                "scrollToBottom" -> "driver.scrollToBottom()"
                "scrollToMiddle" -> "driver.scrollToMiddle(${arguments["ratio"] ?: 0.5})"
                "scrollToScreen" -> arguments["screenNumber"]?.let { n -> "driver.scrollToScreen(${n})" }
                // Advanced clicks
                "clickTextMatches" -> arguments["selector"]?.let { s ->
                    val pattern = arguments["pattern"]?.esc() ?: return@let null
                    val count = arguments["count"] ?: 1
                    "driver.clickTextMatches(\"${s.esc()}\", \"$pattern\", $count)"
                }

                "clickMatches" -> arguments["selector"]?.let { s ->
                    val attr = arguments["attrName"]?.esc() ?: return@let null
                    val pattern = arguments["pattern"]?.esc() ?: return@let null
                    val count = arguments["count"] ?: 1
                    "driver.clickMatches(\"${s.esc()}\", \"$attr\", \"$pattern\", $count)"
                }

                "clickNthAnchor" -> arguments["n"]?.let { n ->
                    val root = arguments["rootSelector"] ?: "body"
                    "driver.clickNthAnchor(${n}, \"${root.esc()}\")"
                }
                // Enhanced navigation
                "waitForNavigation" -> {
                    val oldUrl = arguments["oldUrl"] ?: ""
                    val timeout = arguments["timeoutMillis"] ?: 5000L
                    "driver.waitForNavigation(\"${oldUrl.esc()}\", ${timeout})"
                }
                // Screenshots
                "captureScreenshot" -> {
                    val sel = arguments["selector"]
                    val fp = arguments["fullPage"]
                    when {
                        arguments.isEmpty() -> "driver.captureScreenshot()"
                        !sel.isNullOrBlank() -> "driver.captureScreenshot(${sel.norm()})"
                        fp == "true" -> "driver.captureScreenshot(true)"
                        else -> null
                    }
                }
                // Timing
                "delay" -> "driver.delay(${arguments["millis"] ?: 1000})"
                // URL and document info
                "currentUrl" -> "driver.currentUrl()"
                "url" -> "driver.url()"
                "documentURI" -> "driver.documentURI()"
                "baseURI" -> "driver.baseURI()"
                "referrer" -> "driver.referrer()"
                "pageSource" -> "driver.pageSource()"
                "getCookies" -> "driver.getCookies()"
                // Additional status checking
                "isHidden" -> arguments["selector"]?.let { "driver.isHidden(${it.norm()})" }
                "visible" -> arguments["selector"]?.let { "driver.visible(${it.norm()})" }
                "isChecked" -> arguments["selector"]?.let { "driver.isChecked(${it.norm()})" }
                "bringToFront" -> "driver.bringToFront()"
                // Additional interactions
                "type" -> arguments["selector"]?.let { s ->
                    arguments["text"]?.let { t -> "driver.type(\"${s.esc()}\", \"${t.esc()}\")" }
                }

                "scrollToViewport" -> arguments["n"]?.let { "driver.scrollToViewport(${it})" }
                "mouseWheelDown" -> "driver.mouseWheelDown(${arguments["count"] ?: 1}, ${arguments["deltaX"] ?: 0.0}, ${arguments["deltaY"] ?: 150.0}, ${arguments["delayMillis"] ?: 0})"
                "mouseWheelUp" -> "driver.mouseWheelUp(${arguments["count"] ?: 1}, ${arguments["deltaX"] ?: 0.0}, ${arguments["deltaY"] ?: -150.0}, ${arguments["delayMillis"] ?: 0})"
                "moveMouseTo" -> arguments["x"]?.let { x ->
                    arguments["y"]?.let { y -> "driver.moveMouseTo(${x}, ${y})" }
                } ?: arguments["selector"]?.let { s ->
                    "driver.moveMouseTo(\"${s.esc()}\", ${arguments["deltaX"] ?: 0}, ${arguments["deltaY"] ?: 0})"
                }

                "dragAndDrop" -> arguments["selector"]?.let { s ->
                    "driver.dragAndDrop(\"${s.esc()}\", ${arguments["deltaX"] ?: 0}, ${arguments["deltaY"] ?: 0})"
                }
                // HTML and text extraction
                "outerHTML" -> arguments["selector"]?.let { "driver.outerHTML(${it.norm()})" } ?: "driver.outerHTML()"

                "textContent" -> "driver.textContent()"
                "selectFirstTextOrNull" -> arguments["selector"]?.let { "driver.selectFirstTextOrNull(${it.norm()})" }
                "selectTextAll" -> arguments["selector"]?.let { "driver.selectTextAll(${it.norm()})" }
                "selectFirstAttributeOrNull" -> arguments["selector"]?.let { s ->
                    arguments["attrName"]?.let { a -> "driver.selectFirstAttributeOrNull(\"${s.esc()}\", \"${a.esc()}\")" }
                }

                "selectAttributes" -> arguments["selector"]?.let { "driver.selectAttributes(${it.norm()})" }
                "selectAttributeAll" -> arguments["selector"]?.let { s ->
                    arguments["attrName"]?.let { a -> "driver.selectAttributeAll(\"${s.esc()}\", \"${a.esc()}\", ${arguments["start"] ?: 0}, ${arguments["limit"] ?: 10000})" }
                }

                "selectImages" -> arguments["selector"]?.let { "driver.selectImages(${it.norm()}, ${arguments["offset"] ?: 1}, ${arguments["limit"] ?: Int.MAX_VALUE})" }
                // JavaScript evaluation
                "evaluate" -> arguments["expression"]?.let { "driver.evaluate(${it.norm()})" }
                else -> null
            }

            return expression ?: throw IllegalArgumentException("⚠️ Illegal tool call | $tc")
        }
    }
}
