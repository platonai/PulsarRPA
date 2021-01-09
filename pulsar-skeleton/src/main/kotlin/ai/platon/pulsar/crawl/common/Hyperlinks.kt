package ai.platon.pulsar.crawl.common

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.url.StatefulHyperlink
import ai.platon.pulsar.crawl.AddRefererAfterFetchHandler
import ai.platon.pulsar.crawl.HtmlDocumentHandler
import ai.platon.pulsar.crawl.WebPageHandler
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import java.time.Duration
import java.time.Instant

open class ListenableHyperlink(
        /**
         * The url of this hyperlink
         * */
        url: String,
        /**
         * The anchor text of this hyperlink
         * */
        text: String = "",
        /**
         * The order of this hyperlink in it's referer page
         * */
        order: Int = 0,
        /**
         * The url of the referer page
         * */
        referer: String? = null,
        /**
         * The url arguments
         * */
        args: String? = null,
        /**
         * A click url is a url variant, it's the raw url in the html without normalization,
         * for example, an url with a timestamp query parameter added
         * */
        href: String? = null,
        /**
         * The label
         * */
        label: String = ""
): StatefulHyperlink(url, text, order, referer, args, href, label) {

    override val isPersistable: Boolean = false

    val idleTime get() = Duration.between(modifiedAt, Instant.now())

    var onBeforeLoad: () -> Unit = {}
    var onBeforeFetch: (WebPage) -> Unit = { _: WebPage -> }
    var onAfterFetch: (WebPage) -> Unit = { _: WebPage -> }
    var onBeforeParse: (WebPage) -> Unit = { _: WebPage -> }
    var onBeforeExtract: (WebPage) -> Unit = { _: WebPage -> }
    var onAfterExtract: (WebPage, FeaturedDocument) -> Unit = { _: WebPage, _: FeaturedDocument -> }
    var onAfterParse: (WebPage, FeaturedDocument) -> Unit = { _: WebPage, _: FeaturedDocument -> }
    var onAfterLoad: (WebPage) -> Unit = { _: WebPage -> }
}

object Hyperlinks {

    fun registerHandlers(url: ListenableHyperlink, volatileConfig: VolatileConfig) {
        listOf(
                object: WebPageHandler() {
                    override val name = CapabilityTypes.FETCH_BEFORE_LOAD_HANDLER
                    override fun invoke(page: WebPage) = url.onBeforeLoad()
                },

                object: WebPageHandler() {
                    override val name = CapabilityTypes.FETCH_BEFORE_FETCH_HANDLER
                    override fun invoke(page: WebPage) = url.onBeforeFetch(page)
                },

                object: WebPageHandler() {
                    override val name = CapabilityTypes.FETCH_AFTER_FETCH_HANDLER
                    override fun invoke(page: WebPage) {
                        AddRefererAfterFetchHandler(url).invoke(page)
                        url.onAfterFetch(page)
                    }
                },

                object: WebPageHandler() {
                    override val name = CapabilityTypes.FETCH_BEFORE_HTML_PARSE_HANDLER
                    override fun invoke(page: WebPage) = url.onBeforeParse(page)
                },

                object: WebPageHandler() {
                    override val name = CapabilityTypes.FETCH_BEFORE_EXTRACT_HANDLER
                    override fun invoke(page: WebPage) = url.onBeforeExtract(page)
                },

                object: HtmlDocumentHandler() {
                    override val name = CapabilityTypes.FETCH_AFTER_EXTRACT_HANDLER
                    override fun invoke(page: WebPage, document: FeaturedDocument) = url.onAfterExtract(page, document)
                },

                object: HtmlDocumentHandler() {
                    override val name = CapabilityTypes.FETCH_AFTER_HTML_PARSE_HANDLER
                    override fun invoke(page: WebPage, document: FeaturedDocument) = url.onAfterParse(page, document)
                },

                object: WebPageHandler() {
                    override val name = CapabilityTypes.FETCH_AFTER_LOAD_HANDLER
                    override fun invoke(page: WebPage) = url.onAfterLoad(page)
                }
        ).forEach { volatileConfig.putBean(it.name, it) }
    }
}
