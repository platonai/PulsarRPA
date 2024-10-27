package ai.platon.pulsar.common.urls

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.options.OptionUtils
import java.net.MalformedURLException
import java.net.URL
import java.time.Instant

/**
 * An abstract url is a url with some common properties and methods.
 * */
abstract class AbstractUrl(
    /**
     * The url specification of the hyperlink, it is usually normalized, and can contain load arguments.
     * */
    override var url: String,
    /**
     * The anchor text
     * */
    override var text: String = "",
    /**
     * The order of this hyperlink in it referrer page
     * */
    override var order: Int = 0,
    /**
     * The url of the referrer page
     * */
    override var referrer: String? = null,
    /**
     * The additional url arguments
     * */
    override var args: String? = null,
    /**
     * The hypertext reference, It defines the address of the document, which this time is linked from
     * */
    override var href: String? = null,
    /**
     * The priority of this hyperlink
     * */
    override var priority: Int = 0,
    /**
     * The language of this hyperlink
     * */
    override var lang: String = "*",
    /**
     * The country of this hyperlink
     * */
    override var country: String = "*",
    /**
     * The district of this hyperlink
     * */
    override var district: String = "*",
    /**
     * The maximum number of retries
     * */
    override var nMaxRetry: Int = 3,
    /**
     * The depth of this hyperlink
     * */
    override var depth: Int = 0
) : ComparableUrlAware {

    override val configuredUrl get() = UrlUtils.mergeUrlArgs(url, args)

    override val isStandard get() = UrlUtils.isStandard(url)

    @get:Throws(MalformedURLException::class)
    override val toURL get() = URL(url)

    override val toURLOrNull get() = UrlUtils.getURLOrNull(url)

    override val isNil get() = url == AppConstants.NIL_PAGE_URL

    override val isPersistable: Boolean = true

    override val label: String get() = OptionUtils.findOption(args, listOf("-l", "-label", "--label")) ?: ""

    override val deadline: Instant
        get() {
            val deadTime = OptionUtils.findOption(args, listOf("-deadline", "-deadTime", "--dead-time")) ?: ""
            return DateTimes.parseBestInstantOrNull(deadTime) ?: DateTimes.doomsday
        }
    
    /**
     * An abstract url can compare to one of the following types:
     * 1. a [String]
     * 2. a [URL]
     * 3. a [UrlAware]
     * */
    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        return when (other) {
            is String -> url == other
            is URL -> url == other.toString()
            is UrlAware -> url == other.url
            else -> false
        }
    }

    override fun compareTo(other: UrlAware): Int {
        return url.compareTo(other.url)
    }

    override fun hashCode() = url.hashCode()

    override fun toString() = url
    
    /**
     * Serialize the url to a string
     * */
    open fun serialize(): String {
        return serializeTo(StringBuilder()).toString()
    }

    /**
     * Serialize the url to a string builder
     * */
    open fun serializeTo(sb: StringBuilder): StringBuilder {
        sb.append(url)
        
        args?.takeIf { it.isNotBlank() }?.replace("\"", "\\\"")
            ?.let { sb.append(" -args ").append(it) }
        text.takeUnless { it.isEmpty() }?.let { sb.append(" -text ").append(it) }
        order.takeUnless { it == 0 }?.let { sb.append(" -order ").append(it) }
        href?.let { sb.append(" -href ").append(it) }
        referrer?.let { sb.append(" -referrer ").append(it) }
        priority.takeIf { it != 0 }?.let { sb.append(" -priority ").append(it) }
        lang.takeIf { it != "*" }?.let { sb.append(" -lang ").append(it) }
        country.takeIf { it != "*" }?.let { sb.append(" -country ").append(it) }
        district.takeIf { it != "*" }?.let { sb.append(" -district ").append(it) }
        nMaxRetry.takeIf { it != 3 }?.let { sb.append(" -nMaxRetry ").append(it) }
        depth.takeUnless { it == 0 }?.let { sb.append(" -depth ").append(it) }
        
        return sb
    }
}
