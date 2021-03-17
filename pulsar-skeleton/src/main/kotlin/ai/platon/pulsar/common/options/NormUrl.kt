package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.url.Urls
import java.net.MalformedURLException
import java.net.URL

open class NormUrl constructor(
        val url: URL,
        val options: LoadOptions,
        var href: URL? = null
): Comparable<NormUrl> {

    @Throws(MalformedURLException::class)
    constructor(spec: String, options: LoadOptions, href: String? = null):
            this(URL(spec), options, href?.let { URL(href) })

    val spec = url.toString()
    val hrefSpec = href?.toString()
    val args = options.toString()
    val configuredUrl = "$spec $args"

    val isEmpty get() = spec.isEmpty()
    val isNotEmpty get() = !isEmpty
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
        val NIL = NormUrl(AppConstants.NIL_PAGE_URL, LoadOptions.default)
        fun parse(configuredUrl: String, volatileConfig: VolatileConfig): NormUrl {
            val (url, args) = Urls.splitUrlArgs(configuredUrl)
            val options = LoadOptions.parse(args, volatileConfig)
            return NormUrl(url, options)
        }
    }
}
