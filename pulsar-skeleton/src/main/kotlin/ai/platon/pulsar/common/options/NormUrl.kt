package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.common.config.AppConstants
import java.net.URL

open class NormUrl(val url: String, val options: LoadOptions): Comparable<NormUrl> {
    constructor(u: URL, options: LoadOptions): this(u.toString(), options)

    private val u by lazy { Urls.getURLOrNull(url) }

    val isEmpty get() = url.isEmpty()
    val isNotEmpty get() = !isEmpty
    val isNil get() = this == nil
    val isNotNil get() = !isNil
    val isValid get() = u != null
    val isInvalid get() = !isValid
    val configuredUrl get() = "$url $options"

    fun toURL(): URL? { return u }

    operator fun component1() = url
    operator fun component2() = options

    override fun hashCode(): Int {
        return url.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is NormUrl && url == other.url && options == other.options
    }

    override fun compareTo(other: NormUrl): Int {
        return configuredUrl.compareTo(other.configuredUrl)
    }

    override fun toString(): String {
        return configuredUrl
    }

    companion object {
        val nil = NormUrl(AppConstants.NIL_PAGE_URL, LoadOptions.default)
    }
}
