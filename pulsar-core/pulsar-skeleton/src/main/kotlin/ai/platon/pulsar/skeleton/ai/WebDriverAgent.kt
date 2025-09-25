package ai.platon.pulsar.skeleton.ai

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.external.ChatModel
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.ai.tta.TextToAction
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import java.nio.file.Files
import java.time.Instant
import java.util.UUID

class WebDriverAgent(
    val driver: WebDriver,
    val model: ChatModel,
    val maxSteps: Int = 100,
) {
    private val logger = getLogger(this)

    val uuid = UUID.randomUUID()
    val baseDir = AppPaths.get("agent")
    val conf get() = (driver as AbstractWebDriver).settings.config

    private val tta by lazy { TextToAction(conf) }

    private val history = mutableListOf<String>()

    suspend fun execute(instruction: String): ModelResponse {
        Files.createDirectories(baseDir)

        val systemPrompt = buildOperatorSystemPrompt(instruction)

        var step = 0
        while (step < maxSteps) {
            step++

            val screenshotB64 = safeScreenshot()
            val userMsg = buildUserMessage(instruction)

            val message = """
                $systemPrompt 
                $userMsg
            """.trimIndent()

            // Use TextToAction to generate EXACT ONE step
            val action = tta.generateWebDriverAction(message, driver, screenshotB64)
            val response = action.modelResponse

            val parsed = parseOperatorResponse(response.content)

            // termination checks (supported if model responded with such fields)
            if (parsed.taskComplete == true || parsed.method?.equals("close", true) == true) {
                runCatching { driver.close() }.onFailure { logger.warn("Close failed: {}", it.message) }
                history += "#$step complete: taskComplete=${parsed.taskComplete} method=${parsed.method}"
                break
            }

            val toolCall = parsed.toolCalls.firstOrNull()
            if (toolCall == null) {
                history += "#$step no-op (no tool_calls)"
                // if model keeps returning nothing, stop after a small backoff
                if (step >= maxSteps) break
                delay(250)
                continue
            }

//            val execSummary = runCatching { executeToolCall(toolCall) }
//                .onFailure { e -> history += "#$step ERR ${toolCall.name}: ${e.message}" }
//                .getOrElse { null }

            val execSummary = runCatching { driver.act(action) }
                .onFailure { e -> history += "#$step ERR ${toolCall.name}: ${e.message}" }
                .getOrElse { null }

            if (execSummary != null) {
                history += "#$step ${execSummary}"
            }
        }

        // Final summary
        val finalSummary = summarize(instruction)
        history += "FINAL ${finalSummary.content.take(200)}" // keep history compact
        persistTranscript(instruction, finalSummary)

        return finalSummary
    }

    private fun buildOperatorSystemPrompt(goal: String): String {
        return """
你是一个网页通用代理，目标是基于用户目标一步一步完成任务。
重要指南：
1) 将复杂动作拆成原子步骤；
2) act 一次仅做一个动作（单击一次、输入一次、选择一次）；
3) 不要在一步中合并多个动作；
4) 多个动作用多步表达；

输出严格使用 JSON，字段：
- tool_calls: [ { name: string, args: object } ] // 最多 1 个
- taskComplete: boolean // 可选
- method: string // 可选，'close' 时终止

工具规范：
${tta.TOOL_CALL_LIST}

用户总目标：$goal
        """.trimIndent()
    }

    private suspend fun buildUserMessage(instruction: String): String {
        val currentUrl = runCatching { driver.currentUrl() }.getOrNull().orEmpty()
        val h = if (history.isEmpty()) "(无)" else history.takeLast(8).joinToString("\n")
        return """
此前动作摘要：
$h

请基于当前页面截图与历史动作，规划下一步（严格单步原子动作），若无法推进请输出空 tool_calls。
目标：$instruction
当前URL：$currentUrl
        """.trimIndent()
    }

    private data class ToolCall(val name: String, val args: Map<String, Any?>)

    private data class ParsedResponse(
        val toolCalls: List<ToolCall>,
        val taskComplete: Boolean?,
        val method: String?
    )

    private fun parseOperatorResponse(content: String): ParsedResponse {
        // 1) Try structured JSON with tool_calls
        try {
            val root = JsonParser.parseString(content)
            if (root.isJsonObject) {
                val obj = root.asJsonObject
                val toolCalls = mutableListOf<ToolCall>()
                if (obj.has("tool_calls") && obj.get("tool_calls").isJsonArray) {
                    obj.getAsJsonArray("tool_calls").forEach { el ->
                        if (el.isJsonObject) {
                            val tc = el.asJsonObject
                            val name = tc["name"]?.takeIf { it.isJsonPrimitive }?.asString
                            val args = (tc.getAsJsonObject("args") ?: JsonObject()).entrySet()
                                .associate { (k, v) -> k to jsonElementToKotlin(v) }
                            if (!name.isNullOrBlank()) toolCalls += ToolCall(name, args)
                        }
                    }
                }
                val taskComplete = obj.get("taskComplete")?.takeIf { it.isJsonPrimitive }?.asBoolean
                val method = obj.get("method")?.takeIf { it.isJsonPrimitive }?.asString
                if (toolCalls.isNotEmpty() || taskComplete != null || method != null) {
                    return ParsedResponse(toolCalls.take(1), taskComplete, method)
                }
            }
        } catch (_: Exception) { /* fall back */
        }

        // 2) Fall back to TTA tool call JSON parser
        return runCatching {
            val calls = tta.parseToolCalls(content).map { ToolCall(it.name, it.args) }
            ParsedResponse(calls.take(1), null, null)
        }.getOrElse { ParsedResponse(emptyList(), null, null) }
    }

    private fun jsonElementToKotlin(e: com.google.gson.JsonElement): Any? = when {
        e.isJsonNull -> null
        e.isJsonPrimitive -> {
            val p = e.asJsonPrimitive
            when {
                p.isBoolean -> p.asBoolean
                p.isNumber -> {
                    val n = p.asNumber
                    val d = n.toDouble();
                    val i = n.toInt(); if (d == i.toDouble()) i else d
                }

                else -> p.asString
            }
        }

        e.isJsonArray -> e.asJsonArray.map { jsonElementToKotlin(it) }
        e.isJsonObject -> e.asJsonObject.entrySet().associate { it.key to jsonElementToKotlin(it.value) }
        else -> null
    }

    private suspend fun executeToolCall(tc: ToolCall): String {
        return when (tc.name) {
            // Navigation
            "navigateTo", "goto" -> {
                val url = tc.args["url"]?.toString()?.trim().orEmpty()
                if (url.isBlank()) return "skip navigateTo (blank url)"
                val old = runCatching { driver.currentUrl() }.getOrNull()
                driver.navigateTo(url)
                runCatching { driver.waitForNavigation(old ?: "") }
                "navigateTo -> $url"
            }

            // Wait
            "waitForSelector" -> {
                val selector = tc.args["selector"]?.toString()?.trim().orEmpty()
                val timeout = (tc.args["timeoutMillis"] as? Number)?.toLong() ?: 5000L
                if (selector.isBlank()) return "skip waitForSelector (blank selector)"
                driver.waitForSelector(selector, timeout)
                "waitForSelector -> $selector (${timeout}ms)"
            }

            // Basic interactions
            "click" -> {
                val selector = tc.args["selector"]?.toString()?.trim().orEmpty()
                if (selector.isBlank()) return "skip click (blank selector)"
                val count = (tc.args["count"] as? Number)?.toInt() ?: 1
                driver.click(selector, count)
                "click -> $selector x$count"
            }
            overrideName("fill") -> {
                val selector = tc.args["selector"]?.toString()?.trim().orEmpty()
                val text = tc.args["text"]?.toString() ?: ""
                if (selector.isBlank()) return "skip fill (blank selector)"
                driver.fill(selector, text)
                "fill -> $selector text(${text.take(20)})"
            }
            "press" -> {
                val selector = tc.args["selector"]?.toString()?.trim().orEmpty()
                val key = tc.args["key"]?.toString()?.trim().orEmpty()
                if (selector.isBlank() || key.isBlank()) return "skip press (blank args)"
                driver.press(selector, key)
                "press -> $selector key=$key"
            }
            "check" -> {
                val selector = tc.args["selector"]?.toString()?.trim().orEmpty()
                if (selector.isBlank()) return "skip check (blank selector)"
                driver.check(selector)
                "check -> $selector"
            }
            "uncheck" -> {
                val selector = tc.args["selector"]?.toString()?.trim().orEmpty()
                if (selector.isBlank()) return "skip uncheck (blank selector)"
                driver.uncheck(selector)
                "uncheck -> $selector"
            }

            // Scrolling
            "scrollDown" -> { val c = (tc.args["count"] as? Number)?.toInt() ?: 1; driver.scrollDown(c); "scrollDown -> $c" }
            "scrollUp" -> { val c = (tc.args["count"] as? Number)?.toInt() ?: 1; driver.scrollUp(c); "scrollUp -> $c" }
            "scrollToTop" -> { driver.scrollToTop(); "scrollToTop" }
            "scrollToBottom" -> { driver.scrollToBottom(); "scrollToBottom" }
            "scrollToMiddle" -> { val r = (tc.args["ratio"] as? Number)?.toDouble() ?: 0.5; driver.scrollToMiddle(r); "scrollToMiddle -> $r" }
            "scrollToScreen" -> { val n = (tc.args["screenNumber"] as? Number)?.toDouble() ?: 0.0; driver.scrollToScreen(n); "scrollToScreen -> $n" }

            // Advanced clicks
            "clickTextMatches" -> {
                val selector = tc.args["selector"]?.toString()?.trim().orEmpty()
                val pattern = tc.args["pattern"]?.toString()?.trim().orEmpty()
                val count = (tc.args["count"] as? Number)?.toInt() ?: 1
                if (selector.isBlank() || pattern.isBlank()) return "skip clickTextMatches (blank args)"
                driver.clickTextMatches(selector, pattern, count)
                "clickTextMatches -> $selector / $pattern ($count)"
            }
            "clickMatches" -> {
                val selector = tc.args["selector"]?.toString()?.trim().orEmpty()
                val attr = tc.args["attrName"]?.toString()?.trim().orEmpty()
                val pattern = tc.args["pattern"]?.toString()?.trim().orEmpty()
                val count = (tc.args["count"] as? Number)?.toInt() ?: 1
                if (selector.isBlank() || attr.isBlank() || pattern.isBlank()) return "skip clickMatches (blank args)"
                driver.clickMatches(selector, attr, pattern, count)
                "clickMatches -> $selector @$attr~$pattern ($count)"
            }
            "clickNthAnchor" -> {
                val n = (tc.args["n"] as? Number)?.toInt() ?: 0
                val root = tc.args["rootSelector"]?.toString() ?: "body"
                val href = driver.clickNthAnchor(n, root)
                "clickNthAnchor -> #$n in $root => $href"
            }

            // Screenshots
            "captureScreenshot" -> {
                val sel = tc.args["selector"]?.toString()?.trim().orEmpty()
                val b64 = if (sel.isBlank()) driver.captureScreenshot() else driver.captureScreenshot(sel)
                if (b64 != null) saveStepScreenshot(b64)
                val target = sel.ifBlank { "page" }
                "captureScreenshot -> $target"
            }

            // Timing
            "delay" -> {
                val ms = (tc.args["millis"] as? Number)?.toLong() ?: 1000L
                delay(ms)
                "delay -> $ms ms"
            }

            // Graceful unknowns
            else -> "skip unknown tool '${tc.name}'"
        }
    }

    private fun overrideName(name: String) = name // placeholder for future alias handling

    private suspend fun safeScreenshot(): String? = runCatching { driver.captureScreenshot() }.getOrNull()

    private fun saveStepScreenshot(b64: String) {
        runCatching {
            val ts = Instant.now().toEpochMilli()
            val p = baseDir.resolve("screenshot-${ts}.b64")
            Files.writeString(p, b64)
        }.onFailure { logger.warn("Save screenshot failed: {}", it.message) }
    }

    private fun persistTranscript(instruction: String, finalResp: ModelResponse) {
        runCatching {
            val ts = Instant.now().toEpochMilli()
            val log = baseDir.resolve("session-${ts}.log")
            val sb = StringBuilder()
            sb.appendLine("INSTRUCTION: $instruction")
            sb.appendLine("HISTORY:")
            history.forEach { sb.appendLine(it) }
            sb.appendLine()
            sb.appendLine("FINAL:")
            sb.appendLine(finalResp.content)
            Files.writeString(log, sb.toString())
        }.onFailure { logger.warn("Persist transcript failed: {}", it.message) }
    }

    private fun buildSummaryPrompt(goal: String): Pair<String, String> {
        val system = "你是总结助理，请基于执行轨迹对原始目标进行总结，输出 JSON。"
        val user = buildString {
            appendLine("原始目标：$goal")
            appendLine("执行轨迹(按序)：")
            history.forEach { appendLine(it) }
            appendLine()
            appendLine("""请严格输出 JSON：{"taskComplete":bool,"summary":string,"keyFindings":[string],"nextSuggestions":[string]} 无多余文字。""")
        }
        return system to user
    }

    private fun summarize(goal: String): ModelResponse {
        val (system, user) = buildSummaryPrompt(goal)
        return model.call(user, system)
    }
}
