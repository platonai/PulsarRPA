package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.config.AppConstants.BROWSER_INTERACTIVE_ELEMENTS_SELECTOR
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.serialize.json.JsonExtractor
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.common.urls.URLUtils
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.nodes.node.ext.*
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.rest.api.common.*
import ai.platon.pulsar.rest.api.entities.PromptRequest
import ai.platon.pulsar.rest.api.entities.PromptRequestL2
import ai.platon.pulsar.rest.api.entities.PromptResponseL2
import ai.platon.pulsar.rest.api.entities.ScrapeRequest
import ai.platon.pulsar.skeleton.common.options.LoadOptions
import ai.platon.pulsar.skeleton.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.skeleton.session.PulsarSession
import com.fasterxml.jackson.module.kotlin.readValue
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeFilter
import org.jsoup.select.NodeTraversor
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicReference

@Service
class PromptService(
    val session: PulsarSession,
    val globalCacheFactory: GlobalCacheFactory,
    val loadService: LoadService,
    val scrapeService: ScrapeService,
) {
    companion object {
        const val MIN_USER_MESSAGE_LENGTH = 2
    }

    val commandStatusCache = ConcurrentSkipListMap<String, PromptResponseL2>()
    private val logger = getLogger(PromptService::class)

    fun chat(prompt: String): String {
        return session.chat(prompt).content
    }

    fun chat(request: PromptRequest): String {
        request.args = LoadOptions.mergeArgs(request.args, "-refresh")
        val (page, document) = loadService.loadDocument(request)

        val prompt = request.prompt
        if (prompt.isNullOrBlank()) {
            return DEFAULT_INTRODUCE
        }

        return if (page.protocolStatus.isSuccess) {
            session.chat(prompt, document.text).content
        } else {
            // Throw?
            page.protocolStatus.toString()
        }
    }

    fun extract(request: PromptRequest): String {
        val prompt = request.prompt
        if (prompt.isNullOrBlank()) {
            return DEFAULT_INTRODUCE
        }

        request.args = LoadOptions.mergeArgs(request.args, "-refresh")
        val (page, document) = loadService.loadDocument(request)

        val prompt2 = """
            Extract the following information from the web page:
            $prompt
            
            ${document.text}


            """.trimIndent()

        return if (page.protocolStatus.isSuccess) {
            session.chat(prompt2).content
        } else {
            // Throw?
            page.protocolStatus.toString()
        }
    }

    fun convertAPIRequestCommandToJSON(request: String, url: String): String? {
        require(URLUtils.isStandard(url)) { "URL must not be blank" }

        // Replace the URL in the request with a placeholder, so the result from the LLM can be cached.
        val processedRequest = request.replace(url, PLACEHOLDER_URL)
        val prompt = API_REQUEST_COMMAND_CONVERSION_TEMPLATE
            .replace(PLACEHOLDER_REQUEST, processedRequest)

        var content = session.chat(prompt).content
        if (content.isBlank()) {
            return null
        }
        content = content.replace(PLACEHOLDER_URL, url)

        return JsonExtractor.extractJsonBlocks(content).firstOrNull()
    }

    /**
     * Converts a request string into a PromptRequestL2 object.
     *
     * This method extracts a URL from the request string and uses it to create a PromptRequestL2 object.
     * The URL is replaced with a placeholder in the request string to allow for caching of the result from the LLM.
     *
     * @param request The request string containing a URL.
     * @return A PromptRequestL2 object if a URL is found in the request string, null otherwise.
     * */
    fun convertPromptToRequest(request: String): PromptRequestL2? {
        if (request.isBlank()) {
            return null
        }

        val urls = LinkExtractors.fromText(request)
        if (urls.isEmpty()) {
            return null
        }
        val url = urls.first()

        val json = convertAPIRequestCommandToJSON(request, url)
        if (json.isNullOrBlank()) {
            return null
        }

        val request2: PromptRequestL2 = pulsarObjectMapper().readValue(json)
        request2.url = url
        return request2
    }

    fun convertResponseToMarkdown(response: PromptResponseL2): String {
        val jsonResponse = pulsarObjectMapper().writeValueAsString(response)
        return convertResponseToMarkdown(jsonResponse)
    }

    fun convertResponseToMarkdown(jsonResponse: String): String {
        val userMessage = CONVERT_RESPONSE_TO_MARKDOWN_PROMPT_TEMPLATE.replace(JSON_STRING_PLACEHOLDER, jsonResponse)
        return session.chat(userMessage).content
    }

    fun executeCommand(uuid: String, request: String): PromptResponseL2 {
        val request2 = convertPromptToRequest(request)
        val response2 = if (request2 != null) {
            executeCommand(uuid, request2)
        } else {
            PromptResponseL2.failed(ResourceStatus.SC_BAD_REQUEST)
        }

        commandStatusCache[uuid] = response2

        return response2
    }

    /**
     * Executes a command based on the provided request string.
     *
     * This method first attempts to convert the request string into a PromptRequestL2 object.
     * If successful, it calls the command method with the PromptRequestL2 object.
     * If not, it returns a failed response with a status code indicating a bad request.
     *
     * @param request The request string containing a URL and other parameters.
     * @return A PromptResponseL2 object containing the result of the command execution.
     * */
    fun executeCommand(request: String): PromptResponseL2 {
        val uuid = UUID.randomUUID().toString()
        val request2 = convertPromptToRequest(request)
        val response2 = if (request2 != null) {
            executeCommandNotCached(uuid, request2)
        } else {
            PromptResponseL2.failed(uuid, ResourceStatus.SC_BAD_REQUEST)
        }

        require(uuid == response2.uuid) { "UUID mismatch: $uuid != ${response2.uuid}" }
        commandStatusCache[uuid] = response2

        return response2
    }

    fun executeCommand(request: PromptRequestL2): PromptResponseL2 {
        return executeCommand("", request)
    }

    /**
     * Executes a command based on the provided PromptRequestL2 object.
     *
     * This method loads the document associated with the request, processes the chat and data extraction rules,
     * and returns a PromptResponseL2 object containing the results.
     *
     * @param request The PromptRequestL2 object containing the URL and other parameters.
     * @return A PromptResponseL2 object containing the result of the command execution.
     * */
    fun executeCommand(uuid: String, request: PromptRequestL2): PromptResponseL2 {
        val response = executeCommandNotCached(uuid, request)
        commandStatusCache[uuid] = response
        return response
    }

    private fun executeCommandNotCached(uuid: String, request: PromptRequestL2): PromptResponseL2 {
        request.args = LoadOptions.mergeArgs(request.args, "-refresh")
        val (page, document) = loadService.loadDocument(request)

        if (page.isNil) {
            return PromptResponseL2.failed(ResourceStatus.SC_EXPECTATION_FAILED)
        }

        val response = PromptResponseL2(
            uuid,
            pageStatusCode = page.protocolStatus.minorCode,
            pageContentBytes = page.originalContentLength.toInt()
        )
        if (!page.protocolStatus.isSuccess) {
            return response
        }

        response.statusCode = doChat(page, document, request, response)
        logger.info("Conversion with document ${document.baseURI} finished with status code ${response.statusCode}")

        val sql = request.xsql
        if (sql != null && ScrapeAPIUtils.isScrapeUDF(sql)) {
            kotlin.runCatching { executeQuery(sql, response) }.onFailure { logger.warn("Failed to execute query", it) }
        }

        if (uuid.isBlank()) {
            response.uuid = UUID.randomUUID().toString()
        }
        response.finishTime = Instant.now()
        response.isDone = true

        return response
    }

    private fun doChat(
        page: WebPage,
        document: FeaturedDocument,
        request: PromptRequestL2,
        response: PromptResponseL2
    ): Int {
        var statusCode = ResourceStatus.SC_OK

        try {
            conversionStepByStep(page, document, request, response)
        } catch (e: Exception) {
            statusCode = ResourceStatus.SC_EXPECTATION_FAILED
        }

        return statusCode
    }

    private fun conversionStepByStep(
        page: WebPage,
        document: FeaturedDocument,
        request: PromptRequestL2,
        response: PromptResponseL2
    ) {
        // the 0-based screen number, 0.00 means at the top of the first screen, 1.50 means halfway through the second screen.
        val screenNumber = page.activeDOMMetadata?.screenNumber ?: 0f

        val pageSummaryPrompt = normalizePageSummaryPrompt(request.pageSummaryPrompt)
        val dataExtractionRules = normalizeDataExtractionRules(request.dataExtractionRules)
        var richText: String? = null
        var textContent: String? = null
        if (pageSummaryPrompt != null || dataExtractionRules != null) {
            textContent = if (request.richText == true) {
                selectNthScreenRichText(screenNumber, document).also { richText = it }
            } else {
                selectNthScreenText(screenNumber, document)
            }

            if (pageSummaryPrompt != null) {
                val message = pageSummaryPrompt.replace(PLACEHOLDER_PAGE_CONTENT, textContent)
                response.pageSummary = session.chat(message).content
                logger.info("Page summary: ${response.pageSummary}")
            }

            if (dataExtractionRules != null) {
                val message = dataExtractionRules.replace(PLACEHOLDER_PAGE_CONTENT, textContent)
                response.fields = session.chat(message).content
                logger.info("Data extraction: ${response.fields}")
            }
        }

        val linkExtractionRules = normalizeLinkExtractionRules(request.linkExtractionRules)
        if (linkExtractionRules != null) {
            val finalRichText = richText ?: selectNthScreenRichText(screenNumber, document)
            val message = linkExtractionRules.replace(PLACEHOLDER_PAGE_CONTENT, finalRichText)
            response.links = session.chat(message).content
            logger.info("Link extraction: ${response.links}")
        }
    }

    private fun normalizePageSummaryPrompt(message: String?): String? {
        val message2 = normalizeUserMessage(message) ?: return null

        val suffix = """

### Page Content:

{PLACEHOLDER_PAGE_CONTENT}

        """.trimIndent()

        return "$message2\n$suffix"
    }

    private fun normalizeDataExtractionRules(message: String?): String? {
        val message2 = normalizeUserMessage(message) ?: return null

        val suffix = """

### Rules:
- According the request, extract fields from the page content
- Your result should be a JSON object, where the key is the field name and the value is the field value.

### Page Content:

{PLACEHOLDER_PAGE_CONTENT}

        """.trimIndent()

        return "$message2\n$suffix"
    }

    private fun normalizeLinkExtractionRules(message: String?): String? {
        val message2 = normalizeUserMessage(message) ?: return null

        val suffix = """

### Rules:
- According the request, extract links from the page content
- The link should be a valid URL
- No duplicate links
- Your result should be a JSON object, where the key is the link and the value is the link text.

### Page Content:

{PLACEHOLDER_PAGE_CONTENT}

        """.trimIndent()

        return "$message2\n$suffix"
    }

    /**
     * Normalizes the user message by trimming whitespace and ensuring it has a minimum length.
     * */
    private fun normalizeUserMessage(message: String?): String? {
        return message?.trim()?.takeIf { it.length > MIN_USER_MESSAGE_LENGTH }
    }

    private fun executeQuery(sql: String, response: PromptResponseL2) {
        val scrapeRequest = ScrapeRequest(sql)
        try {
            val scrapeResponse = scrapeService.executeQuery(scrapeRequest)
            response.statusCode = scrapeResponse.statusCode
            response.xsqlResultSet = scrapeResponse.resultSet
        } catch (e: Exception) {
            response.statusCode = ResourceStatus.SC_EXPECTATION_FAILED
        }
    }

    private fun selectNthScreenText(screenNumber: Float, document: FeaturedDocument): String {
        val sb = StringBuilder()
        var lastText = ""

        NodeTraversor.filter(object : NodeFilter {
            override fun head(node: Node, depth: Int): NodeFilter.FilterResult {
                // Check if the node is within the specified screen number range
                if (node.screenNumber < screenNumber - 0.5 || node.screenNumber > screenNumber + 0.5) {
                    return NodeFilter.FilterResult.CONTINUE
                }

                if (node is TextNode) {
                    if (node.numChars > 0) {
                        val text = node.cleanText
                        if (text.isNotBlank() && text != lastText) {
                            sb.append(text)
                            lastText = text
                        }
                    }
                } else {
                    val nodeName = node.nodeName().lowercase()
                    if (nodeName == "a") {
                        if (!sb.endsWith(" ") && !sb.endsWith("\n")) {
                            sb.append(" ")
                        }
                    } else if (nodeName in BROWSER_INTERACTIVE_ELEMENTS_SELECTOR) {
                        if (!sb.endsWith("\n")) {
                            sb.append("\n")
                        }
                    }
                }

                return NodeFilter.FilterResult.CONTINUE
            }
        }, document.body)

        return sb.toString()
    }

    private fun selectNthScreenRichText(screenNumber: Float, document: FeaturedDocument): String {
        val sb = StringBuilder()
        val lastText = AtomicReference<String>()

        NodeTraversor.filter(object : NodeFilter {
            override fun head(node: Node, depth: Int): NodeFilter.FilterResult {
                // Check if the node is within the specified screen number range
                if (node.screenNumber < screenNumber - 0.5 || node.screenNumber > screenNumber + 0.5) {
                    return NodeFilter.FilterResult.CONTINUE
                }

                if (node is TextNode || node.isImage || node.isAnchor) {
                    return accumRichText(node, sb, lastText)
                } else if (node.nodeName().lowercase() in BROWSER_INTERACTIVE_ELEMENTS_SELECTOR) {
                    if (!sb.endsWith("\n")) {
                        sb.appendLine()
                    }
                }

                return NodeFilter.FilterResult.CONTINUE
            }
        }, document.body)

        return sb.toString()
    }

    private fun accumRichText(
        node: Node,
        sb: StringBuilder,
        lastText: AtomicReference<String>
    ): NodeFilter.FilterResult {
        if (node.isImage) {
            if (node.width > 200 && node.height > 200) {
                val imageUrl = node.attr("abs:src")
                if (URLUtils.isStandard(imageUrl)) {
                    sb.appendLine("<img src=\"$imageUrl\" />")
                }
            }
            return NodeFilter.FilterResult.SKIP_CHILDREN
        } else if (node.isAnchor) {
            val anchorText = node.cleanText
            val anchorUrl = node.attr("abs:href")
            if (anchorText.isNotEmpty() && URLUtils.isStandard(anchorUrl)) {
                sb.appendLine("<a href=\"$anchorUrl\">$anchorText</a>")
            }
            return NodeFilter.FilterResult.SKIP_CHILDREN
        } else if (node is TextNode && node.numChars > 0) {
            val text = node.cleanText
            if (text.isNotEmpty() && lastText.get() != text) {
                sb.append(text)
                lastText.set(text)
            }
            return NodeFilter.FilterResult.CONTINUE
        }
        return NodeFilter.FilterResult.CONTINUE
    }
}
