package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.nodes.node.ext.screenNumber
import ai.platon.pulsar.dom.select.ElementTraversor
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
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeTraversor
import org.springframework.stereotype.Service
import java.time.Instant

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
        // the 0-based screen number
        val screenNumber = page.activeDOMMetadata?.screenNumber ?: 0f

        var userMessage1 = normalizeUserMessage(request.talkAboutTextContentPrompt)
        var userMessage2 = normalizeUserMessage(request.textContentFieldDescriptions)
        if (userMessage1 != null || userMessage2 != null) {
            val textContent = selectNthScreenText(screenNumber, document)
            if (userMessage1 != null) {
                val message = userMessage1 + "\n" + textContent
                response.talkAboutTextContentResponse = session.chat(message).content
            }
            if (userMessage2 != null) {
                val message = userMessage2 + "\n" + textContent
                response.textContentFields = session.chat(message).content
            }
        }

        userMessage1 = normalizeUserMessage(request.talkAboutHTMLPrompt)
        userMessage2 = normalizeUserMessage(request.htmlFieldDescriptions)
        if (userMessage1 != null || userMessage2 != null) {
            val htmlContent = selectNthScreenHTML(screenNumber, document)
            if (userMessage1 != null) {
                val message = userMessage1 + "\n" + htmlContent
                response.talkAboutHTMLResponse = session.chat(message).content
            }
            if (userMessage2 != null) {
                val message = userMessage2 + "\n" + htmlContent
                response.htmlContentFields = session.chat(message).content
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
        NodeTraversor.traverse({ node, _ ->
            if (node is TextNode) {
                // Check if the node is within the specified screen number range
                if (node.screenNumber > screenNumber - 0.5 && node.screenNumber < screenNumber + 0.5) {
                    val text = node.text()
                    if (text.isNotBlank()) {
                        sb.append(text)
                    }
                }
            }
        }, document.body)

        return sb.toString()
    }

    private fun selectNthScreenHTML(screenNumber: Float, document: FeaturedDocument): String {
        val sb = StringBuilder()

        ElementTraversor.traverse(document.body) { ele ->
            // Check if the node is within the specified screen number range
            if (ele.screenNumber > screenNumber - 0.5 && ele.screenNumber < screenNumber + 0.5) {
                sb.append(ele.outerHtml())
            }
        }

        return sb.toString()
    }
}
