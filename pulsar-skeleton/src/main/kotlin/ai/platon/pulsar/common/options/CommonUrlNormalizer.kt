package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.urls.NormUrl
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.common.urls.Urls
import ai.platon.pulsar.crawl.AddRefererAfterFetchHandler
import ai.platon.pulsar.crawl.LoadEventHandler
import ai.platon.pulsar.crawl.LoadEventPipelineHandler
import ai.platon.pulsar.crawl.common.url.ListenableHyperlink
import ai.platon.pulsar.crawl.filter.CrawlUrlNormalizers

class CommonUrlNormalizer(private val urlNormalizers: CrawlUrlNormalizers? = null) {
    companion object {
        fun registerEventHandlers(url: ListenableHyperlink, options: LoadOptions) {
            val handler = url.loadEventHandler
            if (handler is LoadEventPipelineHandler) {
                handler.onAfterFetchPipeline.addFirst(AddRefererAfterFetchHandler(url))
            }

            options.conf.name = options.label

            // register the handler
            options.conf.putBean(handler)
        }
    }

    /**
     * Normalize an url.
     *
     * If both url arguments and LoadOptions are present, the LoadOptions overrides the tailing arguments,
     * but default values in LoadOptions are ignored.
     * */
    fun normalize(url: UrlAware, options: LoadOptions, toItemOption: Boolean): NormUrl {
        val (spec, args0) = Urls.splitUrlArgs(url.url)
        val args1 = url.args ?: ""
        val args2 = options.toString()
        // the later on overwriting the ones before
        val args = "$args2 $args1 $args0".trim()

        val finalOptions = initOptions(LoadOptions.parse(args, options.conf), toItemOption)
        if (url is ListenableHyperlink) {
            registerEventHandlers(url, finalOptions)
        }

        // TODO: the normalization order might not be the best
        var normalizedUrl: String
        val eventHandler = finalOptions.conf.getBean(LoadEventHandler::class)
        if (eventHandler?.onNormalize != null) {
            normalizedUrl = eventHandler.onNormalize(spec) ?: return NormUrl.NIL
        } else {
            normalizedUrl = Urls.normalizeOrNull(spec, options.shortenKey) ?: return NormUrl.NIL
            val normalizers = urlNormalizers
            if (!options.noNorm && normalizers != null) {
                normalizedUrl = normalizers.normalize(normalizedUrl) ?: return NormUrl.NIL
            }
        }

        finalOptions.apply(finalOptions.conf)

        val href = url.href?.takeIf { Urls.isValidUrl(it) }
        return NormUrl(normalizedUrl, finalOptions, href, url)
    }

    private fun initOptions(options: LoadOptions, toItemOption: Boolean = false): LoadOptions {
        options.apply(options.conf)

        return if (toItemOption) options.createItemOptions() else options
    }
}
