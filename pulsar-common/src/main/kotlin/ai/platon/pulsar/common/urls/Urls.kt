package ai.platon.pulsar.common.urls

import java.net.MalformedURLException
import java.net.URL
import java.time.Instant

/**
 * A degenerate url represent a task that executes in the main loop.
 * A degenerate url can be submitted to the url pool normally, the main loop will take it from the url pool,
 * and execute it as a task, but it will never be loaded as a webpage.
 * */
interface DegenerateUrl

interface CallableDegenerateUrl: DegenerateUrl {
    operator fun invoke()
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
     * The url specification, can be followed by load arguments.
     * */
    var url: String

    /**
     * The explicitly specified load arguments
     * */
    var args: String?

    /**
     * The hypertext reference, it defines the address of the document, which this time is linked from.
     * The href is usually extracted from the webpage and serves as the browser's primary choice for navigation.
     * */
    var href: String?

    /**
     * The referrer url, it is the url of the webpage that contains the hyperlink.
     * */
    var referrer: String?

    /**
     * The priority of the url, the higher the priority, the earlier the url will be loaded.
     * Priority is a numerical value, where smaller numbers indicate higher priority.
     * */
    var priority: Int

    /**
     * The configured url, always be "$url $args"
     * */
    val configuredUrl: String

    /**
     * If true, the url is standard and can be converted to a [java.net.URL]
     * */
    val isStandard: Boolean

    /**
     * Converted to a [java.net.URL]
     * */
    @get:Throws(MalformedURLException::class)
    val toURL: URL

    /**
     * Converted to a [java.net.URL], if the url is invalid, return null
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
     * The url label, it should be a shortcut for `-label` option in load options
     * */
    val label: String

    /**
     * The deadline, it should be a shortcut for `-deadline` option in load options
     * */
    val deadline: Instant

    /**
     * Required website language, reserved for future use
     * */
    val lang: String

    /**
     * Required website country, reserved for future use
     * */
    val country: String

    /**
     * Required website district, reserved for future use
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
