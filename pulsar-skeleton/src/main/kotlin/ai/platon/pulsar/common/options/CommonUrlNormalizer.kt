package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.urls.NormUrl
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.crawl.AddRefererAfterFetchHandler
import ai.platon.pulsar.crawl.common.url.ListenableHyperlink
import ai.platon.pulsar.crawl.filter.CrawlUrlNormalizers

class CommonUrlNormalizer(private val urlNormalizers: CrawlUrlNormalizers? = null) {
    companion object {
        fun registerEventHandlers(url: ListenableHyperlink, options: LoadOptions) {
            url.eventHandler.loadEventHandler.onAfterFetch.addFirst(AddRefererAfterFetchHandler(url))
            options.eventHandler = url.eventHandler
            options.conf.name = options.label
        }
    }

    /**
     * Normalize an url.
     *
     * If both url arguments and LoadOptions are present, the LoadOptions overrides the tailing arguments,
     * but default values in LoadOptions are ignored.
     * */
    fun normalize(url: UrlAware, options: LoadOptions, toItemOption: Boolean): NormUrl {
        val (spec, args0) = UrlUtils.splitUrlArgs(url.url)
        val args1 = url.args ?: ""
        val args2 = options.toString()
        // the later args overwrites the earlier ones
        val args = "$args2 $args1 $args0".trim()

        val finalOptions = initOptions(LoadOptions.parse(args, options), toItemOption)
        if (url is ListenableHyperlink) {
            registerEventHandlers(url, finalOptions)
        }

//        require(options.eventHandler != null)
//        require(finalOptions.eventHandler != null)

        // TODO: the normalization order might not be the best
        var normalizedUrl: String
        val eventHandler = finalOptions.eventHandler
        if (eventHandler?.loadEventHandler?.onNormalize != null) {
            normalizedUrl = eventHandler.loadEventHandler.onNormalize(spec) ?: return NormUrl.NIL
        } else {
            val ignoreQuery = options.shortenKey || options.ignoreQuery
            normalizedUrl = UrlUtils.normalizeOrNull(spec, ignoreQuery) ?: return NormUrl.NIL
            val normalizers = urlNormalizers
            if (!options.noNorm && normalizers != null) {
                normalizedUrl = normalizers.normalize(normalizedUrl) ?: return NormUrl.NIL
            }
        }

        finalOptions.toConf()

        val href = url.href?.takeIf { UrlUtils.isValidUrl(it) }
        return NormUrl(normalizedUrl, finalOptions, href, url)
    }

    private fun initOptions(options: LoadOptions, toItemOption: Boolean = false): LoadOptions {
        options.toConf()
        return if (toItemOption) options.createItemOptions() else options
    }
}
