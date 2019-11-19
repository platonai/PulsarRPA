/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.platon.pulsar.crawl.filter;

import ai.platon.pulsar.common.config.ImmutableConfig;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class uses a "chained filter" pattern to run defined normalizers.
 * Different lists of normalizers may be defined for different "scopes", or
 * contexts where they are used (note however that they need to be activated
 * first through <tt>plugin.include</tt> property).
 * <p>
 * <p>
 * There is one global scope defined by default, which consists of all active
 * normalizers. The order in which these normalizers are executed may be defined
 * in "urlnormalizer.order" property, which lists space-separated implementation
 * classes (if this property is missing normalizers will be run in random
 * order). If there are more normalizers activated than explicitly named on this
 * list, the remaining ones will be run in random order after the ones specified
 * on the list are executed.
 * </p>
 * <p>
 * You can define a set of contexts (or scopes) in which normalizers may be
 * called. Each scope can have its own list of normalizers (defined in
 * "urlnormalizer.scope.<scope_name>" property) and its own order (defined in
 * "urlnormalizer.order.<scope_name>" property). If any of these properties are
 * missing, default settings are used for the global scope.
 * </p>
 * <p>
 * In case no normalizers are required for any given scope, a
 * <code>ai.platon.pulsar.crawl.net.urlnormalizer.pass.PassURLNormalizer</code> should
 * be used.
 * </p>
 * <p>
 * Each normalizer may further select among many configurations, depending on
 * the scope in which it is called, because the scope name is passed as a
 * parameter to each normalizer. You can also use the same normalizer for many
 * scopes.
 * </p>
 * <p>
 * Several scopes have been defined, and various PulsarConstants cli will attempt using
 * scope-specific normalizers first (and fall back to default config if
 * scope-specific configuration is missing).
 * </p>
 * <p>
 * Normalizers may be run several times, to ensure that modifications introduced
 * by normalizers at the end of the list can be further reduced by normalizers
 * executed at the beginning. By default this loop is executed just once - if
 * you want to ensure that all possible combinations have been applied you may
 * want to run this loop up to the number of activated normalizers. This loop
 * count can be configured through <tt>urlnormalizer.loop.count</tt> property.
 * As soon as the url is unchanged the loop will stop and return the result.
 * </p>
 *
 * @author Andrzej Bialecki
 */
public final class UrlNormalizers {

    /**
     * Default scope. If no scope properties are defined then the configuration
     * for this scope will be used.
     */
    public static final String SCOPE_DEFAULT = "default";
    public static final String SCOPE_PARTITION = "partition";
    public static final String SCOPE_GENERATE_HOST_COUNT = "generate_host_count";
    public static final String SCOPE_INJECT = "inject";
    public static final String SCOPE_FETCHER = "fetcher";
    public static final String SCOPE_CRAWLDB = "crawldb";
    public static final String SCOPE_LINKDB = "linkdb";
    public static final String SCOPE_INDEXER = "index";
    public static final String SCOPE_OUTLINK = "outlink";

    public static final Logger LOG = LoggerFactory.getLogger(UrlNormalizers.class);

    private ArrayList<UrlNormalizer> urlNormalizers = new ArrayList<>();
    // Reserved
    private Map<String, UrlNormalizer> scopedUrlNormalizers = new HashMap<>();
    private int loopCount = -1;
    private String scope = SCOPE_DEFAULT;

    public UrlNormalizers() {
    }

    public UrlNormalizers(ImmutableConfig conf) {
        this(Collections.emptyList(), SCOPE_DEFAULT, conf);
    }

    public UrlNormalizers(String scope, ImmutableConfig conf) {
        this(Collections.emptyList(), scope, conf);
    }

    public UrlNormalizers(List<UrlNormalizer> urlNormalizers, String scope, ImmutableConfig conf) {
        this.urlNormalizers.addAll(urlNormalizers);

        loopCount = conf.getInt("urlnormalizer.loop.count", 1);
        this.scope = scope;
    }

    /**
     * TODO : not implemented
     */
    public ArrayList<UrlNormalizer> getURLNormalizers(String scope) {
        return urlNormalizers;
    }

    public ArrayList<UrlNormalizer> getUrlNormalizers() {
        return urlNormalizers;
    }

    public void setUrlNormalizers(ArrayList<UrlNormalizer> urlNormalizers) {
        this.urlNormalizers = urlNormalizers;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public UrlNormalizer findByClassName(String name) {
        return CollectionUtils.find(urlNormalizers, p -> p.getClass().getSimpleName().equals(name));
    }

    /**
     * Normalize
     *
     * @param urlString The URL string to normalize.
     * @return A normalized String, using the given <code>scope</code>
     */
    public String normalize(String urlString) {
        return normalize(urlString, SCOPE_DEFAULT);
    }

    /**
     * Normalize
     *
     * @param urlString The URL string to normalize.
     * @param scope     The given scope.
     * @return A normalized String, using the given <code>scope</code>
     */
    public String normalize(String urlString, String scope) {
        // optionally loop several times, and break if no further changes
        String s = urlString;
        for (int k = 0; k < loopCount; k++) {
            for (UrlNormalizer normalizer : this.urlNormalizers) {
                if (urlString == null) {
                    return null;
                }
                urlString = normalizer.normalize(urlString, scope);
            }
            if (s.equals(urlString)) {
                break;
            }
            s = urlString;
        }
        return urlString;
    }

    @Override
    public String toString() {
        return urlNormalizers.stream().map(n -> n.getClass().getSimpleName()).collect(Collectors.joining(", "));
    }
}
