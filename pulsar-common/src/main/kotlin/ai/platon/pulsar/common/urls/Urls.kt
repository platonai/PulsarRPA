package ai.platon.pulsar.common.urls

import java.net.MalformedURLException
import java.net.URL
import java.time.Instant

/**
 * A degenerate url can be used to perform non-fetching tasks in the main loop.
 * */
interface DegenerateUrl {

}

/**
 * A URL represents a Uniform Resource Locator, a pointer to a "resource" on the World Wide Web.
 * A resource can be something as simple as a file or a directory, or it can be a reference to
 * a more complicated object, such as a query to a database or to a search engine.
 *
 * In java, a [URL] object represents a URL.
 * In Pulsar, a [UrlAware] object represents a URL with extra information telling the system
 * how to fetch it.
 * */
interface UrlAware {
    /**
     * The url, it can be configured or not
     * */
    var url: String

    /**
     * The url args
     * */
    var args: String?

    /**
     * The referer(or referrer)
     * */
    var referer: String?

    /**
     * The hypertext reference, It defines the address of the document, which this time is linked from
     * */
    var href: String?

    /**
     * The priority
     * */
    var priority: Int

    /**
     * The configured url, always be "$url $args"
     * */
    val configuredUrl: String

    /**
     * If true, the url is a standard URL and can be converted to a [java.net.URL]
     * */
    val isStandard: Boolean

    /**
     * Converted to a [java.net.URL]
     * */
    @get:Throws(MalformedURLException::class)
    val toURL: URL

    /**
     * Converted to a java.Util.URL, if the url is invalid, return null
     * */
    val toURLOrNull: URL?

    /**
     * An url is Nil if it equals to AppConstants.NIL_PAGE_URL
     * */
    val isNil: Boolean

    /**
     * If this link is persistable
     * */
    val isPersistable: Boolean

    /**
     * The url label, it should be a shortcut for -label option in args
     * */
    val label: String

    /**
     * The deadline, it should be a shortcut for -deadline option in args
     * */
    val deadline: Instant

    /**
     * The deadline, it should be a shortcut for -deadline option in args
     * */
    @Deprecated("Inappropriate name", ReplaceWith("deadline"))
    val deadTime: Instant get() = deadline

    /**
     * Required website language
     * */
    val lang: String

    /**
     * Required website country
     * */
    val country: String

    /**
     * Required website district
     * */
    val district: String

    /**
     * The maximum retry times
     * */
    val nMaxRetry: Int
}

interface ComparableUrlAware : UrlAware, Comparable<UrlAware>

/**
 * The StatefulUrl interface. A StatefulUrl is an UrlAware with status.
 * */
interface StatefulUrl : ComparableUrlAware {
    var authToken: String?
    var remoteAddr: String?
    var status: Int
    var modifiedAt: Instant
    val createdAt: Instant
}
