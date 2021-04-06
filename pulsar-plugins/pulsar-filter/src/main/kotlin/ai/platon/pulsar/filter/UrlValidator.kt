/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.filter

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.filter.CrawlUrlFilter
import java.util.regex.Pattern

/**
 *
 *
 * Validates URLs.
 *
 *
 *
 *
 *
 * Originally based in on php script by Debbie Dyer, validation.php v1.2b, Date:
 * 03/07/02, http://javascript.internet.com. However, this validation now bears
 * little resemblance to the php original.
 *
 *
 *
 * <pre>
 * Example of usage:
 * UrlValidator urlValidator = UrlValidator.get();
 * if (urlValidator.isValid("ftp://foo.bar.com/")) {
 * System.out.println("url is valid");
 * } else {
 * System.out.println("url is invalid");
 * }
 *
 * prints out "url is valid"
</pre> *
 *
 *
 *
 *
 * Based on UrlValidator code from Apache commons-validator.
 *
 *
 * @see [ Uniform Resource
 * Identifiers
](http://www.ietf.org/rfc/rfc2396.txt) */
class UrlValidator(conf: ImmutableConfig) : CrawlUrlFilter {
    private var maxTldLength: Int
    override fun filter(urlString: String): String? {
        return if (isValid(urlString)) urlString else null
    }

    /**
     *
     *
     * Checks if a field has a valid url address.
     *
     *
     * @param url The value validation is being performed on. A `null`
     * value is considered invalid.
     * @return true if the url is valid.
     */
    override fun isValid(url: String): Boolean {
        val matchUrlPat = URL_PATTERN.matcher(url)
        if (!LEGAL_ASCII_PATTERN.matcher(url).matches()) {
            return false
        }
        // Check the whole url address structure
        if (!matchUrlPat.matches()) {
            return false
        }
        if (!isValidScheme(matchUrlPat.group(PARSE_URL_SCHEME))) {
            return false
        }
        if (!isValidAuthority(matchUrlPat.group(PARSE_URL_AUTHORITY))) {
            return false
        }
        if (!isValidPath(matchUrlPat.group(PARSE_URL_PATH))) {
            return false
        }
        return isValidQuery(matchUrlPat.group(PARSE_URL_QUERY))
    }

    /**
     * Validate scheme. If schemes[] was initialized to a non null, then only
     * those scheme's are allowed. Note this is slightly different than for the
     * constructor.
     *
     * @param scheme The scheme to validate. A `null` value is considered
     * invalid.
     * @return true if valid.
     */
    private fun isValidScheme(scheme: String?): Boolean {
        return if (scheme == null) {
            false
        } else SCHEME_PATTERN.matcher(scheme).matches()
    }

    /**
     * Returns true if the authority is properly formatted. An authority is the
     * combination of hostname and port. A `null` authority value is
     * considered invalid.
     *
     * @param authority Authority value to validate.
     * @return true if authority (hostname and port) is valid.
     */
    private fun isValidAuthority(authority: String?): Boolean {
        if (authority == null) {
            return false
        }
        val authorityMatcher = AUTHORITY_PATTERN.matcher(authority)
        if (!authorityMatcher.matches()) {
            return false
        }
        var ipV4Address = false
        var hostname = false
        // check if authority is IP address or hostname
        var hostIP = authorityMatcher.group(PARSE_AUTHORITY_HOST_IP)
        val matchIPV4Pat = IP_V4_DOMAIN_PATTERN.matcher(hostIP)
        ipV4Address = matchIPV4Pat.matches()
        if (ipV4Address) { // this is an IP address so check components
            for (i in 1..4) {
                val ipSegment = matchIPV4Pat.group(i)
                if (ipSegment == null || ipSegment.length <= 0) {
                    return false
                }
                try {
                    if (ipSegment.toInt() > 255) {
                        return false
                    }
                } catch (e: NumberFormatException) {
                    return false
                }
            }
        } else { // Domain is hostname name
            hostname = DOMAIN_PATTERN.matcher(hostIP).matches()
        }

        // rightmost hostname will never start with a digit.
        if (hostname) {
            // LOW-TECH FIX FOR VALIDATOR-202
            // TODO: Rewrite to use ArrayList and .add semantics: see VALIDATOR-203
            val chars = hostIP.toCharArray()
            var size = 1
            for (i in chars.indices) {
                if (chars[i] == '.') {
                    size++
                }
            }

            val domainSegment = arrayOfNulls<String>(size)
            var segCount = 0
            var segLen = 0
            val atomMatcher = ATOM_PATTERN.matcher(hostIP)

            while (atomMatcher.find()) {
                val seg = atomMatcher.group()
                domainSegment[segCount] = seg
                segLen = seg.length + 1
                hostIP = if (segLen >= hostIP.length) "" else hostIP.substring(segLen)
                segCount++
            }

            val topLevel = domainSegment[segCount - 1]
            if (topLevel!!.length < 2 || topLevel.length > maxTldLength) {
                return false
            }

            // First letter of top level must be a alpha
            if (!ALPHA_PATTERN.matcher(topLevel.substring(0, 1)).matches()) {
                return false
            }

            // Make sure there's a host name preceding the authority.
            if (segCount < 2) {
                return false
            }
        }

        if (!hostname && !ipV4Address) {
            return false
        }

        val port = authorityMatcher.group(PARSE_AUTHORITY_PORT)
        if (port != null) {
            if (!PORT_PATTERN.matcher(port).matches()) {
                return false
            }
        }
        val extra = authorityMatcher.group(PARSE_AUTHORITY_EXTRA)
        return isBlankOrNull(extra)
    }

    /**
     *
     *
     * Checks if the field isn't null and length of the field is greater than zero
     * not including whitespace.
     *
     *
     * @param value The value validation is being performed on.
     * @return true if blank or null.
     */
    private fun isBlankOrNull(value: String?): Boolean {
        return value == null || value.trim { it <= ' ' }.length == 0
    }

    /**
     * Returns true if the path is valid. A `null` value is considered
     * invalid.
     *
     * @param path Path value to validate.
     * @return true if path is valid.
     */
    private fun isValidPath(path: String?): Boolean {
        if (path == null) {
            return false
        }
        if (!PATH_PATTERN.matcher(path).matches()) {
            return false
        }
        val slash2Count = countToken("//", path)
        val slashCount = countToken("/", path)
        val dot2Count = countToken("..", path)
        return dot2Count <= 0 || slashCount - slash2Count - 1 > dot2Count
    }

    /**
     * Returns true if the query is null or it's a properly formatted query
     * string.
     *
     * @param query Query value to validate.
     * @return true if query is valid.
     */
    private fun isValidQuery(query: String?): Boolean {
        return if (query == null) {
            true
        } else QUERY_PATTERN.matcher(query).matches()
    }

    /**
     * Returns the number of times the token appears in the target.
     *
     * @param token  Token value to be counted.
     * @param target Target value to count tokens in.
     * @return the number of tokens.
     */
    private fun countToken(token: String, target: String): Int {
        var tokenIndex = 0
        var count = 0
        while (tokenIndex != -1) {
            tokenIndex = target.indexOf(token, tokenIndex)
            if (tokenIndex > -1) {
                tokenIndex++
                count++
            }
        }
        return count
    }

    companion object {
        const val ALPHA_CHARS = "a-zA-Z"
        const val ALPHA_NUMERIC_CHARS = "$ALPHA_CHARS\\d"
        const val SPECIAL_CHARS = ";/@&=,.?:+$"
        const val VALID_CHARS = "[^\\s$SPECIAL_CHARS]"
        const val SCHEME_CHARS = ALPHA_CHARS
        // Drop numeric, and "+-." for now
        const val AUTHORITY_CHARS = "$ALPHA_NUMERIC_CHARS\\-\\."
        const val ATOM = "$VALID_CHARS+"
        /**
         * This expression derived/taken from the BNF for URI (RFC2396).
         */
        val URL_PATTERN = Pattern
                .compile("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?")
        /**
         * Schema/Protocol (ie. http:, ftp:, file:, etc).
         */
        const val PARSE_URL_SCHEME = 2
        /**
         * Includes hostname/ip and port number.
         */
        const val PARSE_URL_AUTHORITY = 4
        const val PARSE_URL_PATH = 5
        const val PARSE_URL_QUERY = 7
        /**
         * Protocol (ie. http:, ftp:,https:).
         */
        val SCHEME_PATTERN = Pattern.compile("^[" + SCHEME_CHARS + "]+")
        val AUTHORITY_PATTERN = Pattern.compile("^([" + AUTHORITY_CHARS + "]*)(:\\d*)?(.*)?")
        const val PARSE_AUTHORITY_HOST_IP = 1
        const val PARSE_AUTHORITY_PORT = 2
        const val PARSE_AUTHORITY_EXTRA = 3
        // TODO: check if is it correct to add a \ before $
        val PATH_PATTERN = Pattern.compile("^(/[-\\w:@&?=+,.!/~*'%\$_;\\(\\)]*)?$")
        val QUERY_PATTERN = Pattern.compile("^(.*)$")
        val LEGAL_ASCII_PATTERN = Pattern.compile("^[\\x21-\\x7E]+$")
        val IP_V4_DOMAIN_PATTERN = Pattern.compile("^(\\d{1,3})[.](\\d{1,3})[.](\\d{1,3})[.](\\d{1,3})$")
        val DOMAIN_PATTERN = Pattern.compile("^$ATOM(\\.$ATOM)*$")
        val PORT_PATTERN = Pattern.compile("^:(\\d{1,5})$")
        val ATOM_PATTERN = Pattern.compile("($ATOM)")
        val ALPHA_PATTERN = Pattern.compile("^[$ALPHA_CHARS]")
        const val TOP_LEVEL_DOMAIN_LENGTH_VALUE = 8
        var TOP_LEVEL_DOMAIN_LENGTH = "urlfilter.tld.length"
    }

    init {
        maxTldLength = conf.getInt(TOP_LEVEL_DOMAIN_LENGTH, 8)
        if (maxTldLength <= 2) maxTldLength = TOP_LEVEL_DOMAIN_LENGTH_VALUE
    }
}