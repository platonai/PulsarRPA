package ai.platon.pulsar.common.urls

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.options.LoadOptions
import java.net.MalformedURLException
import java.net.URL

/**
 * A normalized url.
 *
 * Every NormUrl contains two urls: url and href, url stands for Uniform Resource Locator,
 * both for external webpage and internal database record, and href stands for Hyperlink Reference,
 * which is a url extracted from a HTML document.
 *
 * The href is the first choice to locate a resource, because it's
 * extracted from a HTML document without modification while the url might be normalized.
 * */
open class NormUrl constructor(
    val url: URL,
    val options: LoadOptions,
    var href: URL? = null,
    var detail: UrlAware? = null
): Comparable<NormUrl> {

    @Throws(MalformedURLException::class)
    constructor(spec: String, options: LoadOptions, hrefSpec: String? = null, detail: UrlAware? = null):
            this(URL(spec), options, hrefSpec?.let { URL(hrefSpec) }, detail)

    val spec get() = url.toString()
    val hrefSpec get() = href?.toString()
    val args get() = options.toString()
    val configuredUrl get() = "$spec $args".trim()

    val isNil get() = this == NIL
    val isNotNil get() = !isNil

    operator fun component1() = spec
    operator fun component2() = options

    override fun hashCode(): Int {
        return configuredUrl.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is NormUrl && configuredUrl == other.configuredUrl
    }

    override fun compareTo(other: NormUrl) = configuredUrl.compareTo(other.configuredUrl)

    override fun toString() = configuredUrl

    companion object {
        val NIL = NormUrl(AppConstants.NIL_PAGE_URL, LoadOptions.DEFAULT)

        @JvmStatic
        fun parse(configuredUrl: String, volatileConfig: VolatileConfig): NormUrl {
            val (url, args) = UrlUtils.splitUrlArgs(configuredUrl)
            val options = LoadOptions.parse(args, volatileConfig)
            return NormUrl(url, options)
        }
    }
}
