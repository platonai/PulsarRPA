package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.config.AppConstants.BROWSER_INTERACTIVE_ELEMENTS_SELECTOR
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.nodes.node.ext.*
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.rest.api.common.DEFAULT_INTRODUCE
import ai.platon.pulsar.rest.api.common.ScrapeAPIUtils
import ai.platon.pulsar.rest.api.entities.PromptRequest
import ai.platon.pulsar.rest.api.entities.PromptRequestL2
import ai.platon.pulsar.rest.api.entities.PromptResponseL2
import ai.platon.pulsar.rest.api.entities.ScrapeRequest
import ai.platon.pulsar.skeleton.common.options.LoadOptions
import ai.platon.pulsar.skeleton.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.skeleton.session.PulsarSession
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeFilter
import org.jsoup.select.NodeTraversor
import org.springframework.stereotype.Service
import java.time.Instant
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

    fun command(request: PromptRequestL2): PromptResponseL2 {
        request.args = LoadOptions.mergeArgs(request.args, "-refresh")
        val (page, document) = loadService.loadDocument(request)

        val response = PromptResponseL2()

        if (page.isNil) {
            response.pageStatusCode = ResourceStatus.SC_EXPECTATION_FAILED
            return response
        }

        if (!page.protocolStatus.isSuccess) {
            response.pageStatusCode = page.protocolStatus.minorCode
            return response
        }

        response.statusCode = doChat(page, document, request, response)

        val sql = request.xsql
        if (sql != null && ScrapeAPIUtils.isScrapeUDF(sql)) {
            executeQuery(sql, response)
        }

        response.pageStatusCode = page.protocolStatus.minorCode
        response.pageContentBytes = page.originalContentLength.toInt()
        response.finishTime = Instant.now()
        response.isDone = true

        return response
    }

    private fun doChat(page: WebPage, document: FeaturedDocument, request: PromptRequestL2, response: PromptResponseL2): Int {
        var statusCode = ResourceStatus.SC_OK

        try {
            doChat1(page, document, request, response)
        } catch (e: Exception) {
            statusCode = ResourceStatus.SC_EXPECTATION_FAILED
        }

        return statusCode
    }

    private fun doChat1(page: WebPage, document: FeaturedDocument, request: PromptRequestL2, response: PromptResponseL2) {
        // the 0-based screen number, 0.00 means at the top of the first screen, 1.50 means halfway through the second screen.
        val screenNumber = page.activeDOMMetadata?.screenNumber ?: 0f

        val userMessage1 = normalizeUserMessage(request.talkAboutPage)
        val userMessage2 = normalizeUserMessage(request.fieldDescriptions)
        if (userMessage1 != null || userMessage2 != null) {
            val richTextContent = if (request.richText == true) {
                selectNthScreenRichText(screenNumber, document)
            } else {
                selectNthScreenText(screenNumber, document)
            }

println(richTextContent)

            if (userMessage1 != null) {
                val message = "$userMessage1\n$richTextContent"
                response.talkAboutPageResponse = session.chat(message).content
            }
            if (userMessage2 != null) {
                val message = "$userMessage2\n$richTextContent"
                response.fields = session.chat(message).content
            }
        }
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

        NodeTraversor.filter(object: NodeFilter {
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

    private fun selectNthScreenRichText(screenNumber: Float, document: FeaturedDocument): String {
        val sb = StringBuilder()
        val lastText = AtomicReference<String>()

        NodeTraversor.filter(object: NodeFilter {
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

    private fun accumRichText(node: Node, sb: StringBuilder, lastText: AtomicReference<String>): NodeFilter.FilterResult {
        if (node.isImage) {
            if (node.width > 200 && node.height > 200) {
                val imageUrl = node.attr("abs:src")
                if (UrlUtils.isStandard(imageUrl)) {
                    sb.appendLine("<img src=\"$imageUrl\" />")
                }
            }
            return NodeFilter.FilterResult.SKIP_CHILDREN
        } else if (node.isAnchor) {
            val anchorText = node.cleanText
            val anchorUrl = node.attr("abs:href")
            if (anchorText.isNotEmpty() && UrlUtils.isStandard(anchorUrl)) {
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
