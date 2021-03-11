package ai.platon.pulsar.crawl.common.url

import ai.platon.pulsar.common.config.AppConstants.PSEUDO_URL_BASE
import ai.platon.pulsar.common.url.StatefulHyperlink
import ai.platon.pulsar.crawl.*
import org.apache.commons.lang3.RandomStringUtils
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
    /**
     * A pseudo url can not be fetched directly, but the crawl events are handled
     * */
    val isPseudo: Boolean get() = url.startsWith(PSEUDO_URL_BASE)

    val idleTime get() = Duration.between(modifiedAt, Instant.now())

    open var loadEventHandler: LoadEventHandler? = DefaultLoadEventHandler()
    open var jsEventHandler: JsEventHandler? = DefaultJsEventHandler()
    open var crawlEventHandler: CrawlEventHandler? = DefaultCrawlEventHandler()

    companion object {
        val randomPseudoUrl get() = PSEUDO_URL_BASE + "/" + RandomStringUtils.randomAlphanumeric(8)
    }
}
