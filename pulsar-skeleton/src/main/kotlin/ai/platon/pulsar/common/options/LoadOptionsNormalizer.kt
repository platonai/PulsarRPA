package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.url.UrlAware
import ai.platon.pulsar.common.url.Urls
import ai.platon.pulsar.crawl.LoadEventHandler
import ai.platon.pulsar.crawl.filter.UrlNormalizers

class LoadOptionsNormalizer(
    val config: ImmutableConfig,
    val urlNormalizers: UrlNormalizers? = null
) {
    companion object {
        fun normalize(options: LoadOptions, url: UrlAware): LoadOptions {
            val args = url.args
            val actualOptions = options.clone()
            if (args != null) {
                actualOptions.mergeModified(args)
            }

            if (url.label.isNotBlank()) {
                actualOptions.label = url.label
            }

            return actualOptions
        }
    }

    /**
     * Normalize an url.
     *
     * If both url arguments and LoadOptions are present, the LoadOptions overrides the tailing arguments,
     * but default values in LoadOptions are ignored.
     * */
    fun normalize(url: UrlAware, options: LoadOptions, toItemOption: Boolean): NormUrl {
        val (spec, args) = Urls.splitUrlArgs(url.url)

        var finalOptions = options
        if (args.isNotBlank()) {
            // options parsed from args overrides options parsed from url
            val primeOptions = LoadOptions.parse(args, options.volatileConfig)
            finalOptions = LoadOptions.mergeModified(options, primeOptions, options.volatileConfig)
        }
        initOptions(finalOptions, toItemOption)

        var normalizedUrl: String
        val eventHandler = finalOptions.volatileConfig?.getBean(LoadEventHandler::class)
        if (eventHandler?.onNormalize != null) {
            normalizedUrl = eventHandler.onNormalize(spec) ?: return NormUrl.NIL
        } else {
            normalizedUrl = Urls.normalizeOrNull(spec, options.shortenKey) ?: return NormUrl.NIL
            val normalizers = urlNormalizers
            if (!options.noNorm && normalizers != null) {
                normalizedUrl = normalizers.normalize(normalizedUrl) ?: return NormUrl.NIL
            }
        }

        finalOptions.apply(finalOptions.volatileConfig)

        val href = url.href?.takeIf { Urls.isValidUrl(it) }
        return NormUrl(normalizedUrl, finalOptions, href)
    }

    private fun initOptions(options: LoadOptions, toItemOption: Boolean = false): LoadOptions {
        if (options.volatileConfig == null) {
            options.volatileConfig = config.toVolatileConfig()
        }

        options.apply(options.volatileConfig)

        return if (toItemOption) options.createItemOptions() else options
    }
}
