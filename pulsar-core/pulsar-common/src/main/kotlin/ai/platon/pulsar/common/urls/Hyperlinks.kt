package ai.platon.pulsar.common.urls

/**
 * The hyperlink helper object.
 * */
object Hyperlinks {
    
    /**
     * Convert a [UrlAware] to a [Hyperlink], might loss information
     * */
    fun toHyperlink(url: UrlAware): Hyperlink {
        return if (url is Hyperlink) url
        else Hyperlink(url.url, url.text, url.order, url.referrer, url.args, url.href)
    }
}
