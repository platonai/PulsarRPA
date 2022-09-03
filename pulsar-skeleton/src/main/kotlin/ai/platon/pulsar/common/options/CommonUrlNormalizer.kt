package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.urls.NormUrl
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.crawl.AddRefererAfterFetchHandler
import ai.platon.pulsar.crawl.common.url.ListenableUrl
import ai.platon.pulsar.crawl.filter.CrawlUrlNormalizers

class CommonUrlNormalizer(private val urlNormalizers: CrawlUrlNormalizers? = null) {
    /**
     * Normalize an url.
     *
     * If both url arguments and LoadOptions are present, the url arguments overrides the LoadOptions.
     * */
    fun normalize(url: UrlAware, options: LoadOptions, toItemOption: Boolean): NormUrl {
        val (spec, args0) = UrlUtils.splitUrlArgs(url.url)
        val args1 = url.args ?: ""
        val args2 = options.toString()
        // the later args overwrites the earlier ones
        val args = "$args2 $args1 $args0".trim()

        val finalOptions = createLoadOptions(url, LoadOptions.parse(args, options), toItemOption)

//        require(options.eventHandler != null)
//        require(finalOptions.eventHandler != null)

        // TODO: the normalization order might not be the best
        var normalizedUrl: String
        val eventHandler = finalOptions.eventHandler
        if (eventHandler?.loadEventHandler?.onNormalize?.isNotEmpty == true) {
            normalizedUrl = eventHandler.loadEventHandler.onNormalize(spec) ?: return NormUrl.NIL
        } else {
            val ignoreQuery = options.ignoreUrlQuery
            normalizedUrl = UrlUtils.normalizeOrNull(spec, ignoreQuery) ?: return NormUrl.NIL
            val normalizers = urlNormalizers
            if (!options.noNorm && normalizers != null) {
                normalizedUrl = normalizers.normalize(normalizedUrl) ?: return NormUrl.NIL
            }
        }
        normalizedUrl = normalizedUrl.substringBefore("#")

        finalOptions.overrideConfiguration()

        val href = url.href?.takeIf { UrlUtils.isValidUrl(it) }
        return NormUrl(normalizedUrl, finalOptions, href, url)
    }

    private fun createLoadOptions(url: UrlAware, options: LoadOptions, toItemOption: Boolean = false): LoadOptions {
        val options2 = if (toItemOption) options.createItemOptions() else options
        val options3 = createLoadOptions0(url, options2)

        options3.overrideConfiguration()

        return options3
    }

    private fun createLoadOptions0(url: UrlAware, options: LoadOptions): LoadOptions {
        val clone = options.clone()
        require(options.eventHandler == clone.eventHandler)
        require(options.itemEventHandler == clone.itemEventHandler)

        clone.conf.name = clone.label
        clone.nMaxRetry = url.nMaxRetry

        if (url is ListenableUrl) {
            clone.ensureEventHandler().combine(url.eventHandler)
        }

        return clone
    }
}
