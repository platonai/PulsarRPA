package ai.platon.pulsar.skeleton.crawl.common

import ai.platon.pulsar.common.urls.URLUtils
import ai.platon.pulsar.common.urls.URLUtils.getURLOrNull
import ai.platon.pulsar.skeleton.common.domain.DomainSuffix
import ai.platon.pulsar.skeleton.common.domain.DomainSuffixes
import org.slf4j.LoggerFactory
import java.net.*

/**
 * Utility class for URL analysis.
 *
 * protocol://username:password@hostname:port/pathname?search#hash
 * -----------------------------href------------------------------
 *                              -----host----
 * -----------      origin      -------------
 *
 * protocol - protocol scheme of the URL, including the final ':'
 * hostname - domain name
 * port - port number
 * pathname - /pathname
 * search - ?parameters
 * hash - #fragment_identifier
 * username - username specified before the domain name
 * password - password specified before the domain name
 * href - the entire URL
 * origin - protocol://hostname:port
 * host - hostname:port
 *
 */
object InternalURLUtil {
    private val logger = LoggerFactory.getLogger(InternalURLUtil::class.java)
    private val IP_REGEX = Regex("(\\d{1,3}\\.){3}(\\d{1,3})")

    fun getHost(url: String): String? {
        val u = getURLOrNull(url) ?: return null
        return getHost(u, GroupMode.BY_HOST)
    }

    fun getHost(url: String, groupMode: GroupMode): String? {
        val u = getURLOrNull(url) ?: return null
        return getHost(u, groupMode)
    }

    fun getHost(url: URL, groupMode: GroupMode): String? {
        var host: String?
        if (groupMode == GroupMode.BY_IP) {
            host = try {
                val addr = InetAddress.getByName(url.host)
                addr.hostAddress
            } catch (e: UnknownHostException) { // unable to resolve it, so don't fall back to host name
                logger.warn("Unable to resolve: " + url.host + ", skipping.")
                return ""
            }
        } else if (groupMode == GroupMode.BY_DOMAIN) {
            host = URLUtils.getTopPrivateDomain(url)
        } else {
            host = url.host
            if (host == null) {
                logger.warn("Unknown host for url: $url, using URL string as key")
                host = url.toExternalForm()
            }
        }
        return host
    }

    @Deprecated("Moved to UrlUtils", ReplaceWith("UrlUtils.getTopPrivateDomain(url)"))
    @Throws(MalformedURLException::class)
    fun getDomainName(url: String): String = URLUtils.getTopPrivateDomain(url)

    @Deprecated("Use getTopPrivateDomain(url) instead", ReplaceWith("getTopPrivateDomain(url)"))
    fun getDomainName(url: URL): String {
        val tlds = DomainSuffixes.getInstance()
        var host = url.host
        // it seems that java returns hostnames ending with .
        if (host.endsWith(".")) host = host.substring(0, host.length - 1)
        if (IP_REGEX.matches(host)) return host
        var index = 0
        var candidate = host
        while (index >= 0) {
            index = candidate.indexOf('.')
            val subCandidate = candidate.substring(index + 1)
            if (tlds.isDomainSuffix(subCandidate)) {
                return candidate
            }
            candidate = subCandidate
        }
        return candidate
    }

    /**
     * Returns the [DomainSuffix] corresponding to the last public part of
     * the hostname
     *
     * @see com.google.common.net.InternetDomainName.publicSuffix
     */
    @Deprecated("Use getPublicSuffix(url) instead", ReplaceWith("UrlUtils.getPublicSuffix(url)"))
    fun getDomainSuffix(url: String): DomainSuffix? {
        val u = getURLOrNull(url) ?: return null
        return getDomainSuffix(u)
    }

    /**
     * Returns the [DomainSuffix] corresponding to the last public part of
     * the hostname
     *
     * @see com.google.common.net.InternetDomainName.publicSuffix
     */
    @Deprecated("Use UrlUtils.getPublicSuffix(url) instead", ReplaceWith("UrlUtils.getPublicSuffix(url)"))
    fun getDomainSuffix(url: URL): DomainSuffix? {
        return getDomainSuffix(DomainSuffixes.getInstance(), url)
    }

    @Deprecated("Use getPublicSuffix(url) instead", ReplaceWith("UrlUtils.getPublicSuffix(url)"))
    fun getDomainSuffix(tlds: DomainSuffixes, url: URL): DomainSuffix? {
        // DomainSuffixes tlds = DomainSuffixes.getInstance();
        val host = url.host
        if (IP_REGEX.matches(host)) {
            return null
        }
        var index = 0
        var candidate = host
        while (index >= 0) {
            index = candidate.indexOf('.')
            val subCandidate = candidate.substring(index + 1)
            val d = tlds[subCandidate]
            if (d != null) {
                return d
            }
            candidate = subCandidate
        }
        return null
    }

    /**
     * Returns the [DomainSuffix] corresponding to the last public part of
     * the hostname
     */
    @Throws(MalformedURLException::class)
    @Deprecated("Use getPublicSuffix(url) instead", ReplaceWith("UrlUtils.getPublicSuffix(url)"))
    fun getDomainSuffix(tlds: DomainSuffixes, url: String?): DomainSuffix? {
        if (url == null) {
            return null
        }
        return getDomainSuffix(tlds, URI.create(url).toURL())
    }

    /** Partitions of the hostname of the url by "."  */
    fun getHostBatches(url: URL): List<String> {
        val host = url.host
        // return whole hostname, if it is an ipv4
        // TODO : handle ipv6
        return if (IP_REGEX.matches(host)) listOf(host) else host.split(".")
    }

    @JvmStatic
    fun toASCII(url: String?): String? {
        if (url == null) {
            return null
        }

        return try {
            val u = URI.create(url).toURL()
            val host = u.host
            if (host == null || host.isEmpty()) {
                // no host name => no punycoded domain name
                // also do not add additional slashes for file: URLs (PULSAR-1880)
                return url
            }
            val p = URI(
                u.protocol, u.userInfo, IDN.toASCII(host),
                u.port, u.path, u.query, u.ref
            )
            p.toString()
        } catch (e: Exception) {
            null
        }
    }

    @JvmStatic
    fun toUNICODE(url: String?): String? {
        if (url == null) {
            return null
        }

        return try {
            val u = URI.create(url).toURL()
            val host = u.host
            if (host == null || host.isEmpty()) {
                // no host name => no punycoded domain name
                // also do not add additional slashes for file: URLs (PULSAR-1880)
                return url
            }
            val sb = StringBuilder()
            sb.append(u.protocol)
            sb.append("://")
            if (u.userInfo != null) {
                sb.append(u.userInfo)
                sb.append('@')
            }
            sb.append(IDN.toUnicode(host))
            if (u.port != -1) {
                sb.append(':')
                sb.append(u.port)
            }
            sb.append(u.file) // includes query
            if (u.ref != null) {
                sb.append('#')
                sb.append(u.ref)
            }
            sb.toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * @see InternalURLUtil
     */
    enum class GroupMode {
        BY_IP, BY_DOMAIN, BY_HOST
    }
}
