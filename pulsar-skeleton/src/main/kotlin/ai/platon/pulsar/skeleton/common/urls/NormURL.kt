package ai.platon.pulsar.skeleton.common.urls

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.skeleton.common.options.LoadOptions
import java.net.MalformedURLException
import java.net.URL

/**
 * [NormURL] stands for `normal url`, which means the url is final and will be used to locate the resource.
 *
 * Every normal url contains two urls: a `url` and a `href`, `url` stands for Uniform Resource Locator,
 * both for external webpage and internal database record, and `href` stands for Hyperlink Reference,
 * which contains a url extracted from an HTML document.
 *
 * `Href` is the first choice to locate resources, because it's extracted from the HTML document
 * without modification, while `url` is typically normalized.
 * */
open class NormURL constructor(
    /**
     * The url is final and will be used to locate the resource.
     * */
    val url: URL,
    /**
     * The load options to be used to load the resource.
     * */
    val options: LoadOptions,
    /**
     * The href is the raw url in the html without normalization, for example, an url with a timestamp
     * query parameter added.
     *
     * The href is the first choice to locate resources, because it's extracted from the HTML document
     * without modification, while url is typically normalized.
     * */
    var href: URL? = null,
    /**
     * A url aware object that contains the url and its related information.
     * */
    var detail: UrlAware? = null
): Comparable<NormURL> {

    /**
     * Construct a NormURL from a string url and a LoadOptions object.
     * */
    @Throws(MalformedURLException::class)
    constructor(spec: String, options: LoadOptions, hrefSpec: String? = null, detail: UrlAware? = null):
            this(URL(spec), options, hrefSpec?.let { URL(hrefSpec) }, detail)

    /**
     * The url specification in string format.
     */
    val spec get() = url.toString()
    /**
     * The href specification in string format.
     */
    val hrefSpec get() = href?.toString()
    /**
     * The load options specification in string format.
     */
    val args get() = options.toString()
    /**
     * The configured url specification in string format.
     */
    val configuredUrl get() = "$spec $args".trim()
    /**
     * The referrer url specification in string format.
     */
    val referrer get() = options.referrer ?: (detail?.referrer)
    /**
     * Whether the url is nil.
     */
    val isNil get() = this.spec == AppConstants.NIL_PAGE_URL
    /**
     * Whether the url is not nil.
     */
    val isNotNil get() = !isNil
    /**
     * The 1st-component, which is the url specification.
     */
    operator fun component1() = spec
    /**
     * The 2nd-component, which is the load options.
     */
    operator fun component2() = options
    /**
     * The hash code of the object.
     *
     * @return the hash code of the object
     */
    override fun hashCode(): Int {
        return configuredUrl.hashCode()
    }
    /**
     * Whether the object is equal to this object.
     *
     * @param other the object to be compared
     * @return whether the object is equal to this object
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        return other is NormURL && configuredUrl == other.configuredUrl
    }
    /**
     * Compare this object to another object.
     *
     * @param other the object to be compared
     * @return the comparison result
     */
    override fun compareTo(other: NormURL) = configuredUrl.compareTo(other.configuredUrl)
    /**
     * The string representation of the object.
     *
     * @return the string representation of the object
     */
    override fun toString() = configuredUrl

    companion object {
        /**
         * Create a nil NormURL object.
         */
        fun createNil(detail: UrlAware? = null) = NormURL(AppConstants.NIL_PAGE_URL, LoadOptions.DEFAULT, detail = detail)
        /**
         * Parse a configured url to a NormURL object.
         *
         * @param configuredUrl the configured url to be parsed
         * @param volatileConfig the volatile configuration
         * @return the parsed NormURL object
         */
        @JvmStatic
        fun parse(configuredUrl: String, volatileConfig: VolatileConfig): NormURL {
            val (url, args) = UrlUtils.splitUrlArgs(configuredUrl)
            val options = LoadOptions.parse(args, volatileConfig)
            return NormURL(url, options)
        }
    }
}

@Deprecated("Use NormURL instead", ReplaceWith("NormURL"))
typealias NormUrl = NormURL
