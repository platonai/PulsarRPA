/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package ai.platon.pulsar.common;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

public class UrlUtil {

    @Nonnull
    public static Pair<String, String> splitUrlArgs(String configuredUrl) {
        Objects.requireNonNull(configuredUrl);

        configuredUrl = configuredUrl.trim();
        int pos = StringUtils.indexOf(configuredUrl, " ");

        String url;
        String args;
        if (pos >= 0) {
            url = configuredUrl.substring(0, pos);
            args = configuredUrl.substring(pos);
        } else {
            url = configuredUrl;
            args = "";
        }

        return Pair.of(url, args);
    }

    /**
     * Reverses a url's domain. This form is better for storing in hbase. Because
     * scans within the same domain are faster.
     * <p>
     * E.g. "http://bar.foo.com:8983/to/index.html?a=b" becomes
     * "com.foo.bar:8983:http/to/index.html?a=b".
     *
     * @param urlString url to be reversed
     * @return Reversed url
     * @throws MalformedURLException
     */
    public static String reverseUrl(String urlString) throws MalformedURLException {
        return reverseUrl(new URL(urlString));
    }

    public static String reverseUrlOrEmpty(String urlString) {
        try {
            return reverseUrl(new URL(urlString));
        } catch (MalformedURLException e) {
            return "";
        }
    }

    public static String reverseUrlOrNull(String urlString) {
        try {
            return reverseUrl(new URL(urlString));
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Reverses a url's domain. This form is better for storing in hbase. Because
     * scans within the same domain are faster.
     * <p>
     * E.g. "http://bar.foo.com:8983/to/index.html?a=b" becomes
     * "com.foo.bar:http:8983/to/index.html?a=b".
     *
     * @param url url to be reversed
     * @return Reversed url
     */
    public static String reverseUrl(URL url) {
        String host = url.getHost();
        String file = url.getFile();
        String protocol = url.getProtocol();
        int port = url.getPort();

        StringBuilder buf = new StringBuilder();

    /* reverse host */
        reverseAppendSplits(host, buf);

    /* put protocol */
        buf.append(':');
        buf.append(protocol);

    /* put port if necessary */
        if (port != -1) {
            buf.append(':');
            buf.append(port);
        }

    /* put path */
        if (file.length() > 0 && '/' != file.charAt(0)) {
            buf.append('/');
        }
        buf.append(file);

        return buf.toString();
    }

    /**
     * Get the reversed and tenanted format of unreversedUrl, unreversedUrl can be both tenanted or not tenanted
     * This method might change the tenant id of the original url
     * <p>
     * Zero tenant id means no tenant
     *
     * @param unreversedUrl the unreversed url, can be both tenanted or not tenanted
     * @return the tenanted and reversed url of unreversedUrl
     */
    public static String reverseUrl(int tenantId, String unreversedUrl) throws MalformedURLException {
        TenantedUrl tenantedUrl = TenantedUrl.split(unreversedUrl);
        return TenantedUrl.combine(tenantId, reverseUrl(tenantedUrl.getUrl()));
    }

    public static String unreverseUrl(String reversedUrl) {
        StringBuilder buf = new StringBuilder(reversedUrl.length() + 2);

        int pathBegin = reversedUrl.indexOf('/');
        if (pathBegin == -1) {
            pathBegin = reversedUrl.length();
        }
        String sub = reversedUrl.substring(0, pathBegin);

        String[] splits = StringUtils.splitPreserveAllTokens(sub, ':'); // {<reversed host>, <port>, <protocol>}

        buf.append(splits[1]); // put protocol
        buf.append("://");
        reverseAppendSplits(splits[0], buf); // splits[0] is reversed
        // host
        if (splits.length == 3) { // has a port
            buf.append(':');
            buf.append(splits[2]);
        }

        buf.append(reversedUrl.substring(pathBegin));
        return buf.toString();
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
    public static String unreverseUrl(int tenantId, String reversedUrl) throws MalformedURLException {
        TenantedUrl tenantedUrl = TenantedUrl.split(reversedUrl);
        return TenantedUrl.combine(tenantId, unreverseUrl(tenantedUrl.getUrl()));
    }

    /**
     * Get start key for tenanted table
     *
     * @param unreversedUrl unreversed key, which is the original url
     * @return reverse and tenanted key
     */
    public static String getStartKey(int tenantId, String unreversedUrl) throws MalformedURLException {
        if (unreversedUrl == null) {
            // restricted within tenant space
            return tenantId == 0 ? null : String.valueOf(tenantId);
        }

//    if (StringUtils.countMatches(unreversedUrl, "0001") > 1) {
//      return null;
//    }

        String startKey = decodeKeyLowerBound(unreversedUrl);
        return reverseUrl(tenantId, startKey);
    }

    /**
     * Get start key for non-tenanted table
     *
     * @param unreversedUrl unreversed key, which is the original url
     * @return reverse key
     */
    public static String getStartKey(String unreversedUrl) throws MalformedURLException {
        if (unreversedUrl == null) {
            return null;
        }

//    if (StringUtils.countMatches(unreversedUrl, "0001") > 1) {
//      return null;
//    }

        String startKey = decodeKeyLowerBound(unreversedUrl);
        return reverseUrl(startKey);
    }

    /**
     * Get end key for non-tenanted tables
     *
     * @param unreversedUrl unreversed key, which is the original url
     * @return reverse, key bound decoded key
     */
    public static String getEndKey(String unreversedUrl) throws MalformedURLException {
        if (unreversedUrl == null) {
            return null;
        }

//    if (StringUtils.countMatches(unreversedUrl, "FFFF") > 1) {
//      return null;
//    }

        String endKey = decodeKeyUpperBound(unreversedUrl);
        return reverseUrl(endKey);
    }

    /**
     * Get end key for tenanted tables
     *
     * @param unreversedUrl unreversed key, which is the original url
     * @return reverse, tenanted and key bound decoded key
     */
    public static String getEndKey(int tenantId, String unreversedUrl) throws MalformedURLException {
        if (unreversedUrl == null) {
            // restricted within tenant space
            return tenantId == 0 ? null : String.valueOf(tenantId + 1);
        }

//    if (StringUtils.countMatches(unreversedUrl, "FFFF") > 1) {
//      return null;
//    }

        String endKey = decodeKeyUpperBound(unreversedUrl);
        return reverseUrl(tenantId, endKey);
    }

    /**
     * We use unicode character \u0001 to be the lower key bound, but the client usally
     * encode the character to be a string "\\u0001" or "\\\\u0001", so we should decode
     * them to be the right one
     * <p>
     * Note, the character is displayed as <U+0001> in some output system
     * <p>
     * Now, we consider all the three character/string \u0001, "\\u0001", "\\\\u0001"
     * are the lower key bound
     */
    public static String decodeKeyLowerBound(String startKey) {
        startKey = startKey.replaceAll("\\\\u0001", "\u0001");
        startKey = startKey.replaceAll("\\u0001", "\u0001");

        return startKey;
    }

    /**
     * We use unicode character \uFFFF to be the upper key bound, but the client usally
     * encode the character to be a string "\\uFFFF" or "\\\\uFFFF", so we should decode
     * them to be the right one
     * <p>
     * Note, the character may display as <U+FFFF> in some output system
     * <p>
     * Now, we consider all the three character/string \uFFFF, "\\uFFFF", "\\\\uFFFF"
     * are the upper key bound
     */
    public static String decodeKeyUpperBound(String endKey) {
        // Character lastChar = Character.MAX_VALUE;
        endKey = endKey.replaceAll("\\\\uFFFF", "\uFFFF");
        endKey = endKey.replaceAll("\\uFFFF", "\uFFFF");

        return endKey;
    }

    /**
     * Given a reversed url, returns the reversed host E.g
     * "com.foo.bar:http:8983/to/index.html?a=b" -> "com.foo.bar"
     *
     * @param reversedUrl Reversed url
     * @return Reversed host
     */
    public static String getReversedHost(String reversedUrl) {
        return reversedUrl.substring(0, reversedUrl.indexOf(':'));
    }

    private static void reverseAppendSplits(String string, StringBuilder buf) {
        String[] splits = StringUtils.split(string, '.');
        if (splits.length > 0) {
            for (int i = splits.length - 1; i > 0; i--) {
                buf.append(splits[i]);
                buf.append('.');
            }
            buf.append(splits[0]);
        } else {
            buf.append(string);
        }
    }

    public static String reverseHost(String hostName) {
        StringBuilder buf = new StringBuilder();
        reverseAppendSplits(hostName, buf);
        return buf.toString();
    }

    public static String unreverseHost(String reversedHostName) {
        return reverseHost(reversedHostName); // Reversible
    }

    /**
     * Convert given Utf8 instance to String and and cleans out any offending "�"
     * from the String.
     *
     * @param utf8 Utf8 object
     * @return string-ifed Utf8 object or null if Utf8 instance is null
     */
    public static String toString(CharSequence utf8) {
        return (utf8 == null ? null : StringUtil.cleanField(utf8.toString()));
    }
}
