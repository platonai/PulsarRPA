package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.model.GoraWebPage
import ai.platon.pulsar.rest.api.entities.PromptRequest
import ai.platon.pulsar.skeleton.session.PulsarSession
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class LoadService {

    private val logger = LoggerFactory.getLogger(LoadService::class.java)

    @Autowired
    lateinit var session: PulsarSession

    fun load(url: String): WebPage {
        return session.load(url)
    }

    fun loadDocument(url: String, args: String? = null): Pair<WebPage, FeaturedDocument> {
        if (url.contains(":8182/")) {
            logger.warn("Unexpected url, internal url is not allowed | {}", url)
            return GoraWebPage.NIL to FeaturedDocument.NIL
        }

        val page = session.load(url, args ?: "")
        val document = session.parse(page, noCache = true)

        return page to document
    }

    fun loadDocument(request: PromptRequest): Pair<WebPage, FeaturedDocument> {
        val args = request.args ?: ""
        val options = session.options(args)

        // Actions is an alias for actionsOnDocumentReady
        request.actionsOnDocumentReady = request.actionsOnDocumentReady ?: request.actions
        val actionsOnDocumentReady = request.actionsOnDocumentReady
        if (actionsOnDocumentReady != null) {
            options.eventHandlers.browseEventHandlers.onDocumentActuallyReady.addLast { page, driver ->
                driver.instruct(actionsOnDocumentReady)
            }
        }

        val page = session.load(request.url, options)
        val document = session.parse(page)

        return page to document
    }
}
