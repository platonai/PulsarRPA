package ai.platon.pulsar.common.urls

/**
 * A (fat link)[https://en.wikipedia.org/wiki/Hyperlink#Fat_link] (also known as a "one-to-many" link, an "extended link"
 * or a "multi-tailed link") is a hyperlink which leads to multiple endpoints; the link is a multivalued function.
 * */
open class FatLink(
    /**
     * The url specification of the hyperlink, it is usually normalized, and can contain load arguments.
     * */
    url: String,
    /**
     * The anchor text
     * */
    text: String = "",
    /**
     * The order of this hyperlink in it referrer page
     * */
    order: Int = 0,
    /**
     * The url of the referrer page
     * */
    referrer: String? = null,
    /**
     * The additional url arguments
     * */
    args: String? = null,
    /**
     * The hypertext reference, It defines the address of the document, which this time is linked from
     * */
    href: String? = null,
    /**
     * The priority of this hyperlink
     * */
    priority: Int = 0,
    /**
     * The language of this hyperlink
     * */
    lang: String = "*",
    /**
     * The country of this hyperlink
     * */
    country: String = "*",
    /**
     * The district of this hyperlink
     * */
    district: String = "*",
    /**
     * The maximum number of retries
     * */
    nMaxRetry: Int = 3,
    /**
     * The depth of this hyperlink
     * */
    depth: Int = 0,
    /**
     * The tail links
     * */
    var tailLinks: List<StatefulHyperlink>,
) : Hyperlink(url, text, order, referrer, args, href, priority, lang, country, district, nMaxRetry, depth) {
    val size get() = tailLinks.size
    val isEmpty get() = size == 0
    val isNotEmpty get() = !isEmpty
    
    override fun toString() = "$size | $url"
}
