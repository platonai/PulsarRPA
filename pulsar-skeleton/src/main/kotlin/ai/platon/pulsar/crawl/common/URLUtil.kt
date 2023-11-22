/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.crawl.common

import ai.platon.pulsar.common.domain.DomainSuffix
import ai.platon.pulsar.common.domain.DomainSuffixes
import ai.platon.pulsar.common.urls.UrlUtils.getURLOrNull
import com.google.common.net.InternetDomainName
import org.slf4j.LoggerFactory
import java.net.*
import java.util.*

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
 * TODO: merge with ai.platon.pulsar.common.url.Urls
 */
object URLUtil {
    private val logger = LoggerFactory.getLogger(URLUtil::class.java)
    private val IP_REGEX = Regex("(\\d{1,3}\\.){3}(\\d{1,3})")

    fun getHost(url: String): String? {
        val u = getURLOrNull(url) ?: return null
        return getHost(u, GroupMode.BY_HOST)
    }

    fun getHost(url: String, groupMode: GroupMode): String? {
        val u = getURLOrNull(url) ?: return null
        return getHost(u, groupMode)
    }

    fun getHost(url: String, defaultHost: String, groupMode: GroupMode): String {
        val host = getHost(url, groupMode) ?: return defaultHost
        return host.ifEmpty { defaultHost }
    }

    fun getHost(url: URL, defaultHost: String, groupMode: GroupMode): String {
        val host = getHost(url, groupMode) ?: return defaultHost
        return host.ifEmpty { defaultHost }
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
            host = getDomainName(url)
            if (host == null) {
                logger.warn("Unknown domain for url: $url, using URL string as key")
                host = url.toExternalForm()
            }
        } else {
            host = url.host
            if (host == null) {
                logger.warn("Unknown host for url: $url, using URL string as key")
                host = url.toExternalForm()
            }
        }
        return host
    }

    /**
     * Returns the domain name of the url. The domain name of a url is the
     * substring of the url's hostname, w/o subdomain names. As an example <br></br>
     * `
     * getDomainName(conf, new URL(http://lucene.apache.org/))
    ` * <br></br>
     * will return <br></br>
     * ` apache.org`
     *
     * @see com.google.common.net.InternetDomainName.topPrivateDomain
     */
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
     * Returns the domain name of the url. The domain name of a url is the
     * substring of the url's hostname, w/o subdomain names. As an example <br></br>
     * `
     * getDomainName(conf, new http://lucene.apache.org/)
    ` * <br></br>
     * will return <br></br>
     * ` apache.org`
     *
     * @see com.google.common.net.InternetDomainName.topPrivateDomain
     *
     * @throws MalformedURLException
     */
    @Throws(MalformedURLException::class)
    fun getDomainName(url: String): String? {
        return getDomainName(URL(url))
    }

    fun getDomainName(url: String, defaultDomain: String): String {
        return kotlin.runCatching { getDomainName(url) }.getOrNull()?:defaultDomain
    }

    /**
     * Returns whether the given urls have the same domain name. As an example, <br></br>
     * ` isSameDomain(new URL("http://lucene.apache.org")
     * , new URL("http://people.apache.org/"))
     * <br></br> will return true. `
     *
     * @return true if the domain names are equal
     */
    fun isSameDomainName(url1: URL, url2: URL): Boolean {
        return getDomainName(url1).equals(getDomainName(url2), ignoreCase = true)
    }

    /**
     * Returns whether the given urls have the same domain name. As an example, <br></br>
     * ` isSameDomain("http://lucene.apache.org"
     * ,"http://people.apache.org/")
     * <br></br> will return true. `
     *
     * @return true if the domain names are equal
     * @throws MalformedURLException
     */
    @Throws(MalformedURLException::class)
    fun isSameDomainName(url1: String?, url2: String?): Boolean {
        return isSameDomainName(URL(url1), URL(url2))
    }

    /**
     * Returns the [DomainSuffix] corresponding to the last public part of
     * the hostname
     *
     * @see com.google.common.net.InternetDomainName.publicSuffix
     */
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
    fun getDomainSuffix(url: URL): DomainSuffix? {
        return getDomainSuffix(DomainSuffixes.getInstance(), url)
    }

    fun getDomainSuffix(tlds: DomainSuffixes, url: URL): DomainSuffix? { // DomainSuffixes tlds = DomainSuffixes.getInstance();
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
    fun getDomainSuffix(tlds: DomainSuffixes, url: String?): DomainSuffix? {
        return getDomainSuffix(tlds, URL(url))
    }

    /** Partitions of the hostname of the url by "."  */
    fun getHostBatches(url: URL): List<String> {
        val host = url.host
        // return whole hostname, if it is an ipv4
        // TODO : handle ipv6
        return if (IP_REGEX.matches(host)) listOf(host) else host.split(".")
    }

    /**
     * Partitions of the hostname of the url by "."
     *
     * @throws MalformedURLException
     */
    @Throws(MalformedURLException::class)
    fun getHostBatches(url: String): List<String> {
        return getHostBatches(URL(url))
    }

    /**
     * Given two urls, a src and a destination of a redirect, it returns the
     * representative url.
     *
     * This method implements an extended version of the algorithm used by the
     * Yahoo! Slurp crawler described here:<br></br>
     * [ How
     * does the Yahoo! webcrawler handle redirects?](http://help.yahoo.com/l/nz/yahooxtra/search/webcrawler/slurp-11.html) <br></br>
     * <br></br>
     *
     *  1. Choose target url if either url is malformed.
     *  1. If different domains the keep the destination whether or not the
     * redirect is temp or perm
     *
     *  * a.com -> b.com*
     *
     *  1. If the redirect is permanent and the source is root, keep the source.
     *
     *  * *a.com -> a.com?y=1 || *a.com -> a.com/xyz/index.html
     *
     *  1. If the redirect is permanent and the source is not root and the
     * destination is root, keep the destination
     *
     *  * a.com/xyz/index.html -> a.com*
     *
     *  1. If the redirect is permanent and neither the source nor the destination
     * is root, then keep the destination
     *
     *  * a.com/xyz/index.html -> a.com/abc/page.html*
     *
     *  1. If the redirect is temporary and source is root and destination is not
     * root, then keep the source
     *
     *  * *a.com -> a.com/xyz/index.html
     *
     *  1. If the redirect is temporary and source is not root and destination is
     * root, then keep the destination
     *
     *  * a.com/xyz/index.html -> a.com*
     *
     *  1. If the redirect is temporary and neither the source or the destination
     * is root, then keep the shortest url. First check for the shortest host, and
     * if both are equal then check by path. Path is first by length then by the
     * number of / path separators.
     *
     *  * a.com/xyz/index.html -> a.com/abc/page.html*
     *  * *www.a.com/xyz/index.html -> www.news.a.com/xyz/index.html
     *
     *  1. If the redirect is temporary and both the source and the destination
     * are root, then keep the shortest sub-domain
     *
     *  * *www.a.com -> www.news.a.com
     *
     * <br></br>
     * While not in this logic there is a further piece of representative url
     * logic that occurs during indexing and after scoring. During creation of the
     * basic fields before indexing, if a url has a representative url stored we
     * check both the url and its representative url (which should never be the
     * same) against their linkrank scores and the highest scoring one is kept as
     * the url and the lower scoring one is held as the orig url inside of the
     * index.
     *
     * @param src
     * The source url.
     * @param dst
     * The destination url.
     * @param temp
     * Is the redirect a temporary redirect.
     *
     * @return String The representative url.
     */
    @JvmStatic
    fun chooseRepr(src: String, dst: String, temp: Boolean): String { // validate both are well formed urls
        val srcUrl: URL
        val dstUrl: URL
        try {
            srcUrl = URL(src)
            dstUrl = URL(dst)
        } catch (e: MalformedURLException) {
            return dst
        }
        // get the source and destination domain, host, and page
        val srcDomain = getDomainName(srcUrl)
        val dstDomain = getDomainName(dstUrl)
        val srcHost = srcUrl.host
        val dstHost = dstUrl.host
        val srcFile = srcUrl.file
        val dstFile = dstUrl.file
        // are the source and destination the root path url.com/ or url.com
        val srcRoot = srcFile == "/" || srcFile.length == 0
        val destRoot = dstFile == "/" || dstFile.length == 0
        // 1) different domain them keep dest, temp or perm
        // a.com -> b.com*
        //
        // 2) permanent and root, keep src
        // *a.com -> a.com?y=1 || *a.com -> a.com/xyz/index.html
        //
        // 3) permanent and not root and dest root, keep dest
        // a.com/xyz/index.html -> a.com*
        //
        // 4) permanent and neither root keep dest
        // a.com/xyz/index.html -> a.com/abc/page.html*
        //
        // 5) temp and root and dest not root keep src
        // *a.com -> a.com/xyz/index.html
        //
        // 7) temp and not root and dest root keep dest
        // a.com/xyz/index.html -> a.com*
        //
        // 8) temp and neither root, keep shortest, if hosts equal by path else by
        // hosts. paths are first by length then by number of / separators
        // a.com/xyz/index.html -> a.com/abc/page.html*
        // *www.a.com/xyz/index.html -> www.news.a.com/xyz/index.html
        //
        // 9) temp and both root keep shortest sub domain
        // *www.a.com -> www.news.a.com
        // if we are dealing with a redirect from one domain to another keep the
        // destination
        if (srcDomain != dstDomain) {
            return dst
        }
        // if it is a permanent redirect
        return if (!temp) { // if source is root return source, otherwise destination
            if (srcRoot) {
                src
            } else {
                dst
            }
        } else {
            // temporary redirect
            // source root and destination not root
            if (srcRoot && !destRoot) {
                src
            } else if (!srcRoot && destRoot) { // destination root and source not
                dst
            } else if (!srcRoot && !destRoot && srcHost == dstHost) { // source and destination hosts are the same, check paths, host length
                val numSrcPaths = srcFile.split("/").toTypedArray().size
                val numDstPaths = dstFile.split("/").toTypedArray().size
                if (numSrcPaths != numDstPaths) {
                    if (numDstPaths < numSrcPaths) dst else src
                } else {
                    val srcPathLength = srcFile.length
                    val dstPathLength = dstFile.length
                    if (dstPathLength < srcPathLength) dst else src
                }
            } else { // different host names and both root take the shortest
                val numSrcSubs = srcHost.split("\\.").toTypedArray().size
                val numDstSubs = dstHost.split("\\.").toTypedArray().size
                if (numDstSubs < numSrcSubs) dst else src
            }
        }
    }
    
    /**
     * Returns the lowercase origin for the url.
     *
     * @param url The url to check.
     * @return String The hostname for the url.
     */
    @Throws(MalformedURLException::class)
    fun getOrigin(url: String): String {
        val u = URL(url)
        return u.protocol + "://" + u.host
    }
    
    /**
     * Returns the lowercase origin for the url or null if the url is not well-formed.
     *
     * @param url The url to check.
     * @return String The hostname for the url.
     */
    fun getOriginOrNull(url: String?): String? {
        if (url == null) {
            return null
        }
        
        return try {
            val u = URL(url)
            u.protocol + "://" + u.host
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * Returns the lowercase hostname for the url.
     *
     * @param url The url to check.
     * @return String The hostname for the url.
     */
    @Throws(MalformedURLException::class)
    fun getHostName(url: String) = URL(url).host.lowercase(Locale.getDefault())

    /**
     * Returns the lowercase hostname for the url or null if the url is not well-formed.
     *
     * @param url The url to check.
     * @return String The hostname for the url.
     */
    fun getHostNameOrNull(url: String?): String? {
        if (url == null) {
            return null
        }
        
        return try {
            URL(url).host.lowercase(Locale.getDefault())
        } catch (e: MalformedURLException) {
            null
        }
    }

    fun getHostName(url: String?, defaultValue: String): String {
        return try {
            URL(url).host.lowercase(Locale.getDefault())
        } catch (e: MalformedURLException) {
            defaultValue
        }
    }

    /**
     * Returns the path for the url. The path consists of the protocol, host, and
     * path, but does not include the query string. The host is lowercased but the
     * path is not.
     *
     * @param url
     * The url to check.
     * @return String The path for the url.
     */
    fun getQuery(url: String): String? {
        var url = url
        return try { // get the full url, and replace the query string with and empty string
            url = url.lowercase(Locale.getDefault())
            val queryStr = URL(url).query
            if (queryStr != null) url.replace("?$queryStr", "") else url
        } catch (e: MalformedURLException) {
            null
        }
    }

    @JvmStatic
    fun toASCII(url: String?): String? {
        return try {
            val u = URL(url)
            val host = u.host
            if (host == null || host.isEmpty()) {
                // no host name => no punycoded domain name
                // also do not add additional slashes for file: URLs (PULSAR-1880)
                return url
            }
            val p = URI(u.protocol, u.userInfo, IDN.toASCII(host),
                    u.port, u.path, u.query, u.ref)
            p.toString()
        } catch (e: Exception) {
            null
        }
    }

    @JvmStatic
    fun toUNICODE(url: String?): String? {
        return try {
            val u = URL(url)
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
     * @see URLUtil
     */
    enum class GroupMode {
        BY_IP, BY_DOMAIN, BY_HOST
    }
}
