package ai.platon.pulsar.crawl.common

import ai.platon.pulsar.common.url.StatefulHyperlink
import ai.platon.pulsar.crawl.*
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

    open var loadEventHandler: LoadEventHandler? = null
    open var jsEventHandler: JsEventHandler? = null
    open var crawlEventHandler: CrawlEventHandler? = null
}
