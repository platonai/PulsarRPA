package ai.platon.pulsar.crawl.common

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.url.StatefulHyperlink
import ai.platon.pulsar.crawl.*
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
): StatefulHyperlink(url, text, order, referer, args, href, label), CrawlEventHandler {

    override val isPersistable: Boolean = false

    val idleTime get() = Duration.between(modifiedAt, Instant.now())

    override var onFilter: (String) -> String? = { it }
    override var onNormalize: (String) -> String? = { it }
    override var onBeforeLoad: (String) -> Unit = {}
    override var onBeforeFetch: (WebPage) -> Unit = {}
    override var onAfterFetch: (WebPage) -> Unit = {}
    override var onBeforeParse: (WebPage) -> Unit = {}
    override var onBeforeHtmlParse: (WebPage) -> Unit = {}
    override var onBeforeExtract: (WebPage) -> Unit = {}
    override var onAfterExtract: (WebPage, FeaturedDocument) -> Unit = { _, _ -> }
    override var onAfterHtmlParse: (WebPage, FeaturedDocument) -> Unit = { _, _ -> }
    override var onAfterParse: (WebPage) -> Unit = { _ -> }
    override var onAfterLoad: (WebPage) -> Unit = {}
}
