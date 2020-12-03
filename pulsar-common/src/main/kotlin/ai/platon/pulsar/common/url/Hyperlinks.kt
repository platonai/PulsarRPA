package ai.platon.pulsar.common.url

import ai.platon.pulsar.common.ResourceStatus
import org.slf4j.LoggerFactory
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

interface UrlAware: Comparable<UrlAware> {
    var url: String
    var args: String
}

interface StatefulUrl: UrlAware, Comparable<UrlAware> {
    override var url: String
    var status: Int
    var modifiedAt: Instant
    val createdAt: Instant
}

abstract class AbstractUrl(
        override var url: String,
        override var args: String = ""
): UrlAware {

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        return when (other) {
            is String -> url == other
            is URL -> url == other.toString()
            is UrlAware -> url == other.url
            else -> false
        }
    }

    override fun hashCode() = url.hashCode()

    override fun compareTo(other: UrlAware): Int {
        return url.compareTo(other.url)
    }

    override fun toString() = url
}

abstract class AbstractStatefulUrl(url: String): AbstractUrl(url), StatefulUrl {
    override var status: Int = ResourceStatus.SC_CREATED
    override var modifiedAt: Instant = Instant.now()
    override val createdAt: Instant = Instant.now()
}

open class PlainUrl(override var url: String): AbstractUrl(url)

data class HyperlinkDatum(
        val url: String,
        val text: String = "",
        val order: Int = 0
)

/**
 * A [hyperlink](https://en.wikipedia.org/wiki/Hyperlink), or simply a link, is a reference to data that the user can
 * follow by clicking or tapping.
 *
 * A hyperlink points to a whole document or to a specific element within a document.
 * Hypertext is text with hyperlinks. The text that is linked from is called anchor text.
 *
 * The [anchor text](https://en.wikipedia.org/wiki/Anchor_text), link label or link text is the visible,
 * clickable text in an HTML hyperlink
 * */
open class Hyperlink(
        url: String,
        val text: String = "",
        val order: Int = 0
): AbstractUrl(url) {
    fun data() = HyperlinkDatum(url, text, order)
}

data class LabeledHyperlinkDatum(
        var label: String,
        var url: String,
        val anchorText: String = "",
        val order: Int = 0,
        var depth: Int = 0
)

open class LabeledHyperlink(
        var label: String,
        url: String,
        val anchorText: String = "",
        val order: Int = 0,
        var depth: Int = 0
): AbstractUrl(url) {
    fun data() = LabeledHyperlinkDatum(label, url, anchorText, order, depth)
}

open class StatefulHyperlink(
        url: String,
        text: String = "",
        order: Int = 0,
        val referer: String? = null
): Hyperlink(url, text, order), StatefulUrl {
    override var status: Int = ResourceStatus.SC_CREATED
    override var modifiedAt: Instant = Instant.now()
    override val createdAt: Instant = Instant.now()
}

/**
 * A (fat link)[https://en.wikipedia.org/wiki/Hyperlink#Fat_link] (also known as a "one-to-many" link, an "extended link"
 * or a "multi-tailed link") is a hyperlink which leads to multiple endpoints; the link is a multivalued function.
 * */
open class FatLink(
        url: String,
        var tailLinks: List<StatefulHyperlink>,
        text: String = "",
        order: Int = 0
): Hyperlink(url, text, order) {
    val size get() = tailLinks.size
    val isEmpty get() = size == 0
    val isNotEmpty get() = !isEmpty

    override fun toString() = "$size $url"
}

open class StatefulFatLink(
        url: String,
        tailLinks: List<StatefulHyperlink>,
        text: String = "",
        order: Int = 0
): FatLink(url, tailLinks, text, order), StatefulUrl {

    override var status: Int = ResourceStatus.SC_CREATED
    override var modifiedAt: Instant = Instant.now()
    override val createdAt: Instant = Instant.now()

    override fun toString() = "$status $createdAt $modifiedAt ${super.toString()}"
}

open class LabeledStatefulFatLink(
        val label: String,
        url: String,
        tailLinks: List<StatefulHyperlink>,
        text: String = "",
        order: Int = 0
): StatefulFatLink(url, tailLinks, text, order), StatefulUrl {
    override fun toString() = "$label ${super.toString()}"
}
