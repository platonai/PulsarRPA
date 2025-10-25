package ai.platon.pulsar.protocol.browser.driver.cdt.detail

import ai.platon.cdt.kt.protocol.events.network.RequestWillBeSent
import ai.platon.cdt.kt.protocol.events.network.ResponseReceived
import ai.platon.cdt.kt.protocol.events.page.FrameNavigated
import ai.platon.cdt.kt.protocol.types.network.ResourceType
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.crawl.fetch.driver.NavigateEntry

class ChromeNavigateEntry(
    private val navigateEntry: NavigateEntry
) {
    private val logger = getLogger(this)

    private val tracer = logger.takeIf { it.isTraceEnabled }

    fun updateStateBeforeRequestSent(event: RequestWillBeSent) {
        updateStateBeforeRequestSent0(event)
    }

    fun updateStateAfterResponseReceived(event: ResponseReceived) {
        updateStateAfterResponseReceived0(event)
    }

    fun updateStateAfterFrameNavigated(event: FrameNavigated) {
        if (event.frame.parentId == null) {
            navigateEntry.mainFrameId = event.frame.id
        }
    }

    fun isMinorResource(event: RequestWillBeSent): Boolean {
        val type = event.type ?: return true
        return navigateEntry.mainFrameReceived && isMinorResource(type)
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
            val headers = mutableMapOf<String, Any>()
            event.request.headers.forEach { (key, value) -> if (value != null) headers[key] = value }
            navigateEntry.updateMainRequest(event.requestId, headers)
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
            val headers = mutableMapOf<String, Any>()
            response.headers.forEach { (key, value) -> if (value != null) headers[key] = value }
            navigateEntry.updateMainResponse(response.status, response.statusText, headers)
        }
    }

    private fun isMajorRequestWillBeSent(event: RequestWillBeSent): Boolean {
        return !navigateEntry.mainFrameReceived && event.type == ResourceType.DOCUMENT
    }

    private fun isMajorResponseReceived(event: ResponseReceived): Boolean {
        return !navigateEntry.mainFrameReceived && event.type == ResourceType.DOCUMENT
    }

    private fun isMinorResource(type: ResourceType): Boolean {
        return type in listOf(
            ResourceType.FONT,
            ResourceType.MEDIA,
            ResourceType.IMAGE,
        )
    }
}
