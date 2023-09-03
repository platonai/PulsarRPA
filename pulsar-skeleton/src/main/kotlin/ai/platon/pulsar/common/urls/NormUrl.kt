package ai.platon.pulsar.common.urls

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.options.LoadOptions
import java.net.MalformedURLException
import java.net.URL

/**
 * [NormURL] stands for `normal url`, which means the url is the final form to locate a resource.
 *
 * Every normal url contains two urls: a `url` and a `href`, `url` stands for Uniform Resource Locator,
 * both for external webpage and internal database record, and `href` stands for Hyperlink Reference,
 * which contains a url extracted from an HTML document.
 *
 * `Href` is the first choice to locate resources, because it's extracted from the HTML document
 * without modification, while `url` is typically normalized.
 * */
open class NormURL constructor(
    val url: URL,
    val options: LoadOptions,
    var href: URL? = null,
    var detail: UrlAware? = null
): Comparable<NormURL> {

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

    fun asItemURL() = NormURL(url, options.createItemOptions(), href, detail)

    override fun hashCode(): Int {
        return configuredUrl.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        return other is NormURL && configuredUrl == other.configuredUrl
    }

    override fun compareTo(other: NormURL) = configuredUrl.compareTo(other.configuredUrl)

    override fun toString() = configuredUrl

    companion object {
        val NIL = NormURL(AppConstants.NIL_PAGE_URL, LoadOptions.DEFAULT)

        @JvmStatic
        fun parse(configuredUrl: String, conf: VolatileConfig): NormURL {
            val (url, args) = UrlUtils.splitUrlArgs(configuredUrl)
            val options = LoadOptions.parse(args, conf)
            return NormURL(url, options)
        }
    }
}
