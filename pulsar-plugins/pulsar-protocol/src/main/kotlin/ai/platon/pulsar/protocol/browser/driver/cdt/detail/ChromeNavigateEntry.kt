package ai.platon.pulsar.protocol.browser.driver.cdt.detail

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.crawl.fetch.driver.NavigateEntry
import com.github.kklisura.cdt.protocol.v2023.events.network.RequestWillBeSent
import com.github.kklisura.cdt.protocol.v2023.events.network.ResponseReceived
import com.github.kklisura.cdt.protocol.v2023.types.network.ResourceType

class ChromeNavigateEntry(
    private val navigateEntry: NavigateEntry
) {
    private val logger = getLogger(this)

    private val tracer = logger.takeIf { it.isTraceEnabled }

    fun updateStateBeforeRequestSent(event: RequestWillBeSent) {
        // We may have better solution to do this
//        if (!navigateEntry.documentTransferred) {
//            navigateEntry.synchronized { updateStateBeforeRequestSent0(event) }
//        } else {
//            updateStateBeforeRequestSent0(event)
//        }
        updateStateBeforeRequestSent0(event)
    }

    fun updateStateAfterResponseReceived(event: ResponseReceived) {
        // We may have better solution to do this
//        if (!navigateEntry.documentTransferred) {
//            navigateEntry.synchronized { updateStateAfterResponseReceived0(event) }
//        } else {
//            updateStateAfterResponseReceived0(event)
//        }
        updateStateAfterResponseReceived0(event)
    }

    fun isMinorResource(event: RequestWillBeSent): Boolean {
        return navigateEntry.documentTransferred && isMinorResource(event.type)
    }

    private fun updateStateBeforeRequestSent0(event: RequestWillBeSent) {
        val count = navigateEntry.networkRequestCount.incrementAndGet()

        // TODO: handle redirection

        // The first request, it should be the main HTML document
        if (logger.isDebugEnabled && count == 1 && event.type != ResourceType.DOCUMENT) {
            // It might be a redirection, prefetch, or just an image
            var url = event.request.url
            if (url.startsWith("data:")) {
                url = "data:xxx(...ignored)"
            }
            logger.debug(
                "The resource type of the first request is {}, requests: {} | {}",
                event.type, navigateEntry.networkRequestCount, url
            )
        }

        if (isMajorRequestWillBeSent(event)) {
            navigateEntry.updateMainRequest(event.requestId, event.request.headers)
        }
    }

    private fun updateStateAfterResponseReceived0(event: ResponseReceived) {
        val count = navigateEntry.networkResponseCount.incrementAndGet()
        val response = event.response

        // TODO: handle redirection

        // The first response, it should be the main HTML document
        if (logger.isDebugEnabled && count == 1 && event.type != ResourceType.DOCUMENT) {
            var url = response.url
            if (url.startsWith("data:")) {
                url = "data:xxx(...ignored)"
            }
            // It might be a redirection, prefetch, or just an image
            logger.debug("The resource type of the first response is {}, responses: {} | {}",
                event.type, navigateEntry.networkResponseCount, url)
        }

        if (isMajorResponseReceived(event)) {
            tracer?.trace("onResponseReceived | driver, document | {}", event.requestId)
            navigateEntry.updateMainResponse(response.status, response.statusText, response.headers)
        }
    }

    private fun isMajorRequestWillBeSent(event: RequestWillBeSent): Boolean {
        return !navigateEntry.documentTransferred && event.type == ResourceType.DOCUMENT
    }

    private fun isMajorResponseReceived(event: ResponseReceived): Boolean {
        return !navigateEntry.documentTransferred && event.type == ResourceType.DOCUMENT
    }

    private fun isMinorResource(type: ResourceType): Boolean {
        return type in listOf(
            ResourceType.FONT,
            ResourceType.MEDIA,
            ResourceType.IMAGE,
        )
    }
}