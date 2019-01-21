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
package fun.platonic.pulsar.crawl.filter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.persist.HypeLink;
import fun.platonic.pulsar.persist.metadata.PageCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Stream;

/**
 * TODO : need full unit test
 * TODO : Move to plugin, urlfilter/contentfilter, etc
 */
public class CrawlFilters {

    public static final Logger LOG = LoggerFactory.getLogger(CrawlFilters.class);

    public static final String CRAWL_FILTER_RULES = "crawl.filter.rules";

    private String scope;
    private UrlNormalizers urlNormalizers = new UrlNormalizers();
    private UrlFilters urlFilters = new UrlFilters();
    private List<CrawlFilter> crawlFilters = Collections.synchronizedList(new ArrayList<>());

    public CrawlFilters() {

    }

    public CrawlFilters(ImmutableConfig conf) {
        this(Collections.emptyList(), new UrlNormalizers(conf), new UrlFilters(conf), UrlNormalizers.SCOPE_DEFAULT, conf);
    }

    public CrawlFilters(List<CrawlFilter> crawlFilters, UrlNormalizers urlNormalizers, UrlFilters urlFilters, String scope, ImmutableConfig conf) {
        this.urlNormalizers = urlNormalizers;
        this.urlFilters = urlFilters;
        this.crawlFilters.addAll(crawlFilters);
        this.scope = scope;
    }

    public UrlNormalizers getUrlNormalizers() {
        return urlNormalizers;
    }

    public void setUrlNormalizers(UrlNormalizers urlNormalizers) {
        this.urlNormalizers = urlNormalizers;
    }

    public UrlFilters getUrlFilters() {
        return urlFilters;
    }

    public void setUrlFilters(UrlFilters urlFilters) {
        this.urlFilters = urlFilters;
    }

    public List<CrawlFilter> getCrawlFilters() {
        return crawlFilters;
    }

    public void setCrawlFilters(List<CrawlFilter> crawlFilters) {
        this.crawlFilters = crawlFilters;
    }

    public boolean isNormalizedValid(HypeLink hypeLink) {
        return isNormalizedValid(hypeLink.getUrl());
    }

    public boolean isNormalizedValid(String url) {
        return !normalizeToEmpty(url).isEmpty();
    }

    @Nonnull
    public String normalizeToEmpty(String url, String scope) {
        return normalizeToEmpty(url);
    }

    @Nonnull
    public String normalizeToEmpty(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }

        String filteredUrl = temporaryUrlFilter(url);
        if (filteredUrl.isEmpty()) {
            return "";
        }

        // apply noNorm and noFilter
        filteredUrl = urlNormalizers.normalize(filteredUrl, scope);
        filteredUrl = urlFilters.filter(filteredUrl);

        return filteredUrl == null ? "" : filteredUrl;
    }

    // TODO : use suffix-urlfilter instead, this is a quick dirty fix
    private String temporaryUrlFilter(String url) {
        if (Stream.of(CrawlFilter.MEDIA_URL_SUFFIXES).anyMatch(url::endsWith)) {
            url = "";
        }

        if (veryLikelyBeSearchUrl(url) || veryLikelyBeMediaUrl(url)) {
            url = "";
        }

        return url;
    }

    public boolean testUrlSatisfied(String url) {
        if (url == null) return false;

        for (CrawlFilter filter : crawlFilters) {
            if (!filter.testUrlSatisfied(url)) {
                return false;
            }
        }

        return true;
    }

    public boolean testTextSatisfied(String text) {
        if (text == null) return false;

        for (CrawlFilter filter : crawlFilters) {
            if (!filter.testTextSatisfied(text)) {
                return false;
            }
        }

        return true;
    }

    public boolean testKeyRangeSatisfied(String reversedUrl) {
        if (reversedUrl == null) return false;

        for (CrawlFilter filter : crawlFilters) {
            if (filter.testKeyRangeSatisfied(reversedUrl)) {
                return true;
            }
        }

        return true;
    }

    public Map<String, String> getReversedKeyRanges() {
        Map<String, String> keyRanges = new HashMap<>();

        for (CrawlFilter filter : crawlFilters) {
            String reversedStartKey = filter.getReversedStartKey();
            String reversedEndKey = filter.getReversedEndKey();

            if (reversedStartKey != null) {
                keyRanges.put(reversedStartKey, reversedEndKey);
            }
        }

        return keyRanges;
    }

    public String[] getMaxReversedKeyRange() {
        String[] keyRange = {null, null};

        for (CrawlFilter filter : crawlFilters) {
            String reversedStartKey = filter.getReversedStartKey();
            String reversedEndKey = filter.getReversedEndKey();

            if (reversedStartKey != null) {
                if (keyRange[0] == null) {
                    keyRange[0] = reversedStartKey;
                } else if (CrawlFilter.keyLessEqual(reversedStartKey, keyRange[0])) {
                    keyRange[0] = reversedStartKey;
                }
            }

            if (reversedEndKey != null) {
                if (keyRange[1] == null) {
                    keyRange[1] = reversedEndKey;
                } else if (CrawlFilter.keyGreaterEqual(reversedEndKey, keyRange[1])) {
                    keyRange[1] = reversedEndKey;
                }
            }
        }

        return keyRange;
    }

    /**
     * TODO : Tricky logic
     */
    public boolean isAllowed(Node node) {
        if (node == null) {
            return false;
        }

        if (crawlFilters.isEmpty()) {
            return true;
        }

        for (CrawlFilter filter : crawlFilters) {
            if (filter.isAllowed(node)) {
                return true;
            }
        }

        return false;
    }

    /**
     * TODO : Tricky logic
     */
    public boolean isDisallowed(Node node) {
        if (node == null) {
            return true;
        }

        if (isAllowed(node)) {
            return false;
        }

        for (CrawlFilter filter : crawlFilters) {
            if (filter.isDisallowed(node)) {
                return true;
            }
        }

        return false;
    }

    public boolean veryLikelyBeDetailUrl(String url) {
        if (url == null) {
            return false;
        }

        PageCategory pageType = CrawlFilter.sniffPageCategory(url);

        if (pageType == PageCategory.DETAIL) {
            return true;
        }

        for (CrawlFilter filter : crawlFilters) {
            if (filter.isDetailUrl(url)) {
                return true;
            }
        }

        return false;
    }

    public boolean veryLikelyBeIndexUrl(String url) {
        if (url == null) return false;

        PageCategory pageType = CrawlFilter.sniffPageCategory(url);

        if (pageType.isIndex()) {
            return true;
        }

        for (CrawlFilter filter : crawlFilters) {
            if (filter.isIndexUrl(url)) {
                return true;
            }
        }

        return false;
    }

    public boolean veryLikelyBeMediaUrl(String url) {
        if (url == null) return false;

        PageCategory pageType = CrawlFilter.sniffPageCategory(url);

        if (pageType == PageCategory.MEDIA) {
            return true;
        }

        for (CrawlFilter filter : crawlFilters) {
            if (filter.isMediaUrl(url)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Notice : index url is not a search url even if it contains "search"
     */
    public boolean veryLikelyBeSearchUrl(String url) {
        if (url == null) {
            return false;
        }

        PageCategory pageType = CrawlFilter.sniffPageCategory(url);

        if (pageType == PageCategory.SEARCH) {
            return true;
        }

        for (CrawlFilter filter : crawlFilters) {
            if (filter.isSearchUrl(url)) {
                return true;
            }
        }

        return false;
    }

    public String toJson() {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        return gson.toJson(this);
    }

    @Override
    public String toString() {
        return toJson();
    }
}
