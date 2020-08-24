package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.common.config.AppConstants
import java.net.MalformedURLException
import java.net.URL

open class NormUrl(val url: URL, val options: LoadOptions): Comparable<NormUrl> {

    @Throws(MalformedURLException::class)
    constructor(spec: String, options: LoadOptions): this(URL(spec), options)

    val spec = url.toString()
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

    override fun compareTo(other: NormUrl): Int {
        return configuredUrl.compareTo(other.configuredUrl)
    }

    override fun toString(): String {
        return configuredUrl
    }

    companion object {
        val NIL = NormUrl(AppConstants.NIL_PAGE_URL, LoadOptions.default)

        fun parse(configuredUrl: String): NormUrl {
            val (url, args) = Urls.splitUrlArgs(configuredUrl)
            val options = LoadOptions.parse(args)
            return NormUrl(url, options)
        }
    }
}
