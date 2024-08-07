
package ai.platon.pulsar.protocol.browser

import ai.platon.pulsar.skeleton.context.PulsarContexts
import ai.platon.pulsar.skeleton.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.skeleton.crawl.protocol.Response
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.emulator.BrowserEmulatedFetcher
import ai.platon.pulsar.protocol.browser.emulator.Defaults
import ai.platon.pulsar.protocol.crowd.ForwardingProtocol

class BrowserEmulatorProtocol : ForwardingProtocol() {
    private val context get() = PulsarContexts.create()

    private val browserEmulator by lazy {
        // require(conf === context.unmodifiedConfig)
        context.getBeanOrNull(BrowserEmulatedFetcher::class)
            ?: Defaults(conf).browserEmulatedFetcher.also { PulsarContexts.registerClosable(it) }
    }

    private val browserEmulatorOrNull get() = if (context.isActive) browserEmulator else null

    @Throws(Exception::class)
    override fun getResponse(page: WebPage, followRedirects: Boolean): Response? {
        require(page.isNotInternal) { "Unexpected internal page ${page.url}" }
        return super.getResponse(page, followRedirects)
            ?: browserEmulatorOrNull?.fetchContent(page)
            ?: ForwardingResponse.canceled(page)
    }

    @Throws(Exception::class)
    override suspend fun getResponseDeferred(page: WebPage, followRedirects: Boolean): Response? {
        require(page.isNotInternal) { "Unexpected internal page ${page.url}" }
        return super.getResponse(page, followRedirects)
            ?: browserEmulatorOrNull?.fetchContentDeferred(page)
            ?: ForwardingResponse.canceled(page)
    }

    override fun reset() {
        browserEmulatorOrNull?.reset()
    }

    override fun cancel(page: WebPage) {
        browserEmulatorOrNull?.cancel(page)
    }

    override fun cancelAll() {
        browserEmulatorOrNull?.cancelAll()
    }
}
