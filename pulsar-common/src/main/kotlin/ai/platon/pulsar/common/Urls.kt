package ai.platon.pulsar.common

import org.apache.commons.lang3.StringUtils
import java.net.MalformedURLException
import java.net.URI
import java.net.URL

object Urls {

    @JvmStatic
    fun getURLOrNull(url: String): URL? {
        return try { URL(url) } catch (ignored: MalformedURLException) { null }
    }

    @JvmStatic
    fun isValidUrl(url: String): Boolean {
        return getURLOrNull(url) != null
    }

    /**
     * Simple normalization
     * TODO: move to general normalize module
     * */
    @JvmStatic
    fun normalizeUrl(url: String, ignoreQuery: Boolean = false): String {
        var u = getURLOrNull(url)?.toString()?:return ""
        u = u.substringBefore("#")
        if (ignoreQuery) {
            u = getUrlWithoutParameters(u)
        }
        return u
    }

    /**
     * Resolve relative URL-s and fix a java.net.URL error in handling of URLs
     * with pure query targets.
     *
     * @param base   base url
     * @param target target url (may be relative)
     * @return resolved absolute url.
     * @throws MalformedURLException
     */
    @Throws(MalformedURLException::class)
    @JvmStatic
    fun resolveURL(base: URL, targetUrl: String): URL {
        val target = targetUrl.trim()

        // handle the case that there is a target that is a pure query,
        // for example
        // http://careers3.accenture.com/Careers/ASPX/Search.aspx?co=0&sk=0
        // It has urls in the page of the form href="?co=0&sk=0&pg=1", and by
        // default
        // URL constructs the base+target combo as
        // http://careers3.accenture.com/Careers/ASPX/?co=0&sk=0&pg=1, incorrectly
        // dropping the Search.aspx target
        //
        // Browsers handle these just fine, they must have an exception similar to
        // this
        return if (target.startsWith("?")) {
            fixPureQueryTargets(base, target)
        } else URL(base, target)
    }

    /**
     * Handle the case in RFC3986 section 5.4.1 example 7, and similar.
     */
    private fun fixPureQueryTargets(base: URL, targetUrl: String): URL {
        var target = targetUrl.trim()
        if (!target.startsWith("?")) {
            return URL(base, target)
        }

        val basePath = base.path
        var baseRightMost = ""
        val baseRightMostIdx = basePath.lastIndexOf("/")
        if (baseRightMostIdx != -1) {
            baseRightMost = basePath.substring(baseRightMostIdx + 1)
        }

        if (target.startsWith("?")) {
            target = baseRightMost + target
        }

        return URL(base, target)
    }

    @JvmStatic
    fun splitUrlArgs(configuredUrl: String): Pair<String, String> {
        var url = configuredUrl.trim()
        val pos = url.indexOfFirst { it.isWhitespace() }

        var args = ""
        if (pos >= 0) {
            args = url.substring(pos)
            url = url.substring(0, pos)
        }

        return url to args
    }

    @JvmStatic
    fun getUrlWithoutParameters(url: String): String {
        try {
            var uri = URI(url)
            uri = URI(uri.scheme,
                    uri.authority,
                    uri.path,
                    null, // Ignore the query part of the input url
                    uri.fragment)
            return uri.toString()
        } catch (ignored: Throwable) {}

        return ""
    }

    @JvmStatic
    fun normalizedUrlAndKey(originalUrl: String, ignoreQuery: Boolean = false): Pair<String, String> {
        val url = if (ignoreQuery) normalizeUrl(originalUrl) else originalUrl
        val key = Urls.reverseUrlOrEmpty(url)
        return url to key
    }

    /**
     * Reverses a url's domain. This form is better for storing in hbase. Because
     * scans within the same domain are faster.
     *
     * E.g. "http://bar.foo.com:8983/to/index.html?a=b" becomes
     * "com.foo.bar:8983:http/to/index.html?a=b".
     *
     * @param url url to be reversed
     * @return Reversed url
     * @throws MalformedURLException
     */
    @JvmStatic
    fun reverseUrl(url: String): String {
        return reverseUrl(URL(url))
    }

    @JvmStatic
    fun reverseUrlOrEmpty(url: String): String {
        return try {
            reverseUrl(URL(url))
        } catch (e: MalformedURLException) {
            ""
        }
    }

    @JvmStatic
    fun reverseUrlOrNull(url: String): String? {
        return try {
            reverseUrl(URL(url))
        } catch (e: MalformedURLException) {
            null
        }
    }

    /**
     * Reverses a url's domain. This form is better for storing in hbase. Because
     * scans within the same domain are faster.
     *
     *
     * E.g. "http://bar.foo.com:8983/to/index.html?a=b" becomes
     * "com.foo.bar:http:8983/to/index.html?a=b".
     *
     * @param url url to be reversed
     * @return Reversed url
     */
    @JvmStatic
    fun reverseUrl(url: URL): String {
        val host = url.host
        val file = url.file
        val protocol = url.protocol
        val port = url.port

        val buf = StringBuilder()

        /* reverse host */
        reverseAppendSplits(host, buf)

        /* put protocol */
        buf.append(':')
        buf.append(protocol)

        /* put port if necessary */
        if (port != -1) {
            buf.append(':')
            buf.append(port)
        }

        /* put path */
        if (file.isNotEmpty() && '/' != file[0]) {
            buf.append('/')
        }
        buf.append(file)

        return buf.toString()
    }

    /**
     * Get the reversed and tenanted format of unreversedUrl, unreversedUrl can be both tenanted or not tenanted
     * This method might change the tenant id of the original url
     *
     *
     * Zero tenant id means no tenant
     *
     * @param unreversedUrl the unreversed url, can be both tenanted or not tenanted
     * @return the tenanted and reversed url of unreversedUrl
     */
    @JvmStatic
    fun reverseUrl(tenantId: Int, unreversedUrl: String): String {
        val tenantedUrl = TenantedUrl.split(unreversedUrl)
        return TenantedUrl.combine(tenantId, reverseUrl(tenantedUrl.url))
    }

    @JvmStatic
    fun unreverseUrl(reversedUrl: String): String {
        val buf = StringBuilder(reversedUrl.length + 2)

        var pathBegin = reversedUrl.indexOf('/')
        if (pathBegin == -1) {
            pathBegin = reversedUrl.length
        }
        val sub = reversedUrl.substring(0, pathBegin)

        val splits = StringUtils.splitPreserveAllTokens(sub, ':') // {<reversed host>, <port>, <protocol>}

        buf.append(splits[1]) // put protocol
        buf.append("://")
        reverseAppendSplits(splits[0], buf) // splits[0] is reversed
        // host
        if (splits.size == 3) { // has a port
            buf.append(':')
            buf.append(splits[2])
        }

        buf.append(reversedUrl.substring(pathBegin))

        return buf.toString()
    }

    /**
     * Get unreversed and tenanted url of reversedUrl, reversedUrl can be both tenanted or not tenanted,
     * This method might change the tenant id of the original url
     *
     * @param tenantId    the expected tenant id of the reversedUrl
     * @param reversedUrl the reversed url, can be both tenanted or not tenanted
     * @return the unreversed url of reversedTenantedUrl
     * @throws MalformedURLException
     */
    @JvmStatic
    fun unreverseUrl(tenantId: Int, reversedUrl: String): String {
        val tenantedUrl = TenantedUrl.split(reversedUrl)
        return TenantedUrl.combine(tenantId, unreverseUrl(tenantedUrl.url))
    }

    /**
     * Get start key for tenanted table
     *
     * @param unreversedUrl unreversed key, which is the original url
     * @return reverse and tenanted key
     */
    @JvmStatic
    fun getStartKey(tenantId: Int, unreversedUrl: String?): String? {
        if (unreversedUrl == null) {
            // restricted within tenant space
            return if (tenantId == 0) null else tenantId.toString()
        }

        //    if (StringUtils.countMatches(unreversedUrl, "0001") > 1) {
        //      return null;
        //    }

        val startKey = decodeKeyLowerBound(unreversedUrl)
        return reverseUrl(tenantId, startKey)
    }

    /**
     * Get start key for non-tenanted table
     *
     * @param unreversedUrl unreversed key, which is the original url
     * @return reverse key
     */
    @JvmStatic
    fun getStartKey(unreversedUrl: String?): String? {
        if (unreversedUrl == null) {
            return null
        }

        //    if (StringUtils.countMatches(unreversedUrl, "0001") > 1) {
        //      return null;
        //    }

        val startKey = decodeKeyLowerBound(unreversedUrl)
        return reverseUrl(startKey)
    }

    /**
     * Get end key for non-tenanted tables
     *
     * @param unreversedUrl unreversed key, which is the original url
     * @return reverse, key bound decoded key
     */
    @JvmStatic
    fun getEndKey(unreversedUrl: String?): String? {
        if (unreversedUrl == null) {
            return null
        }

        //    if (StringUtils.countMatches(unreversedUrl, "FFFF") > 1) {
        //      return null;
        //    }

        val endKey = decodeKeyUpperBound(unreversedUrl)
        return reverseUrl(endKey)
    }

    /**
     * Get end key for tenanted tables
     *
     * @param unreversedUrl unreversed key, which is the original url
     * @return reverse, tenanted and key bound decoded key
     */
    @JvmStatic
    fun getEndKey(tenantId: Int, unreversedUrl: String?): String? {
        if (unreversedUrl == null) {
            // restricted within tenant space
            return if (tenantId == 0) null else (tenantId + 1).toString()
        }

        //    if (StringUtils.countMatches(unreversedUrl, "FFFF") > 1) {
        //      return null;
        //    }

        val endKey = decodeKeyUpperBound(unreversedUrl)
        return reverseUrl(tenantId, endKey)
    }

    /**
     * We use unicode character \u0001 to be the lower key bound, but the client usally
     * encode the character to be a string "\\u0001" or "\\\\u0001", so we should decode
     * them to be the right one
     *
     *
     * Note, the character is displayed as <U></U>+0001> in some output system
     *
     *
     * Now, we consider all the three character/string \u0001, "\\u0001", "\\\\u0001"
     * are the lower key bound
     */
    @JvmStatic
    fun decodeKeyLowerBound(startKey: String): String {
        var startKey = startKey
        startKey = startKey.replace("\\\\u0001".toRegex(), "\u0001")
        startKey = startKey.replace("\\u0001".toRegex(), "\u0001")

        return startKey
    }

    /**
     * We use unicode character \uFFFF to be the upper key bound, but the client usally
     * encode the character to be a string "\\uFFFF" or "\\\\uFFFF", so we should decode
     * them to be the right one
     *
     *
     * Note, the character may display as <U></U>+FFFF> in some output system
     *
     *
     * Now, we consider all the three character/string \uFFFF, "\\uFFFF", "\\\\uFFFF"
     * are the upper key bound
     */
    @JvmStatic
    fun decodeKeyUpperBound(endKey: String): String {
        var endKey = endKey
        // Character lastChar = Character.MAX_VALUE;
        endKey = endKey.replace("\\\\uFFFF".toRegex(), "\uFFFF")
        endKey = endKey.replace("\\uFFFF".toRegex(), "\uFFFF")

        return endKey
    }

    /**
     * Given a reversed url, returns the reversed host E.g
     * "com.foo.bar:http:8983/to/index.html?a=b" -> "com.foo.bar"
     *
     * @param reversedUrl Reversed url
     * @return Reversed host
     */
    @JvmStatic
    fun getReversedHost(reversedUrl: String): String {
        return reversedUrl.substring(0, reversedUrl.indexOf(':'))
    }

    private fun reverseAppendSplits(string: String, buf: StringBuilder) {
        val splits = StringUtils.split(string, '.')
        if (splits.isNotEmpty()) {
            for (i in splits.size - 1 downTo 1) {
                buf.append(splits[i])
                buf.append('.')
            }
            buf.append(splits[0])
        } else {
            buf.append(string)
        }
    }

    @JvmStatic
    fun reverseHost(hostName: String): String {
        val buf = StringBuilder()
        reverseAppendSplits(hostName, buf)
        return buf.toString()
    }

    @JvmStatic
    fun unreverseHost(reversedHostName: String): String {
        return reverseHost(reversedHostName) // Reversible
    }

    /**
     * Convert given Utf8 instance to String and and cleans out any offending "�"
     * from the String.
     *
     * @param utf8 Utf8 object
     * @return string-ifed Utf8 object or null if Utf8 instance is null
     */
    @JvmStatic
    fun toString(utf8: CharSequence?): String? {
        return if (utf8 == null) null else StringUtil.cleanField(utf8.toString())
    }
}
