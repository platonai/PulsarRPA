package ai.platon.pulsar.common.urls

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.options.OptionUtils
import java.net.MalformedURLException
import java.net.URL
import java.time.Instant

abstract class AbstractUrl(
    override var url: String,
    override var args: String? = null,
    override var referrer: String? = null,
    override var href: String? = null,
    override var priority: Int = 0,
) : ComparableUrlAware {

    override val configuredUrl get() = UrlUtils.mergeUrlArgs(url, args)

    override val isStandard get() = UrlUtils.isStandard(url)

    @get:Throws(MalformedURLException::class)
    override val toURL get() = URL(url)

    override val toURLOrNull get() = UrlUtils.getURLOrNull(url)

    override val isNil get() = url == AppConstants.NIL_PAGE_URL

    /**
     * If this url is persistable
     * */
    override val isPersistable: Boolean = true

    override val label: String get() = OptionUtils.findOption(args, listOf("-l", "-label", "--label")) ?: ""

    override val deadline: Instant
        get() {
            val deadTime = OptionUtils.findOption(args, listOf("-deadline", "-deadTime", "--dead-time")) ?: ""
            return DateTimes.parseBestInstantOrNull(deadTime) ?: DateTimes.doomsday
        }

    /**
     * Required website language
     * */
    override var lang: String = "*"

    /**
     * Required website country
     * */
    override var country: String = "*"

    /**
     * Required website district
     * */
    override var district: String = "*"

    /**
     * The maximum retry times
     * */
    override var nMaxRetry: Int = 3

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

    open fun serialize(): String {
        return serializeTo(StringBuilder()).toString()
    }

    protected open fun serializeTo(sb: StringBuilder): StringBuilder {
        sb.append(url)

        args?.takeIf { it.isNotBlank() }?.replace("\"", "\\\"")
            ?.let { sb.append(" -args ").append(it) }
        href?.let { sb.append(" -href ").append(it) }
        referrer?.let { sb.append(" -referrer ").append(it) }
        priority.takeIf { it != 0 }?.let { sb.append(" -priority ").append(it) }
        lang.takeIf { it != "*" }?.let { sb.append(" -lang ").append(it) }
        country.takeIf { it != "*" }?.let { sb.append(" -country ").append(it) }
        district.takeIf { it != "*" }?.let { sb.append(" -district ").append(it) }
        nMaxRetry.takeIf { it != 3 }?.let { sb.append(" -nMaxRetry ").append(it) }

        return sb
    }
}

abstract class AbstractStatefulUrl(
    url: String,
    args: String? = null,
    referrer: String? = null,
) : AbstractUrl(url, args, referrer), StatefulUrl {
    override var status: Int = ResourceStatus.SC_CREATED
    override var modifiedAt: Instant = Instant.now()
    override val createdAt: Instant = Instant.now()

    override fun serializeTo(sb: StringBuilder): StringBuilder {
        super.serializeTo(sb)

        status.takeUnless { it == ResourceStatus.SC_CREATED }
            ?.let { sb.append(" -status ").append(it) }
        authToken?.let { sb.append(" -authToken ").append(it) }
        remoteAddr?.let { sb.append(" -remoteAddr ").append(it) }
        createdAt.let { sb.append(" -createdAt ").append(it) }
        modifiedAt.let { sb.append(" -modifiedAt ").append(it) }

        return sb
    }
}
