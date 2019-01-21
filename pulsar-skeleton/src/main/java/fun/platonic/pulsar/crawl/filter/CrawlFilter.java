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
import fun.platonic.pulsar.common.StringUtil;
import fun.platonic.pulsar.common.UrlUtil;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.metadata.PageCategory;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.slf4j.Logger;
import org.w3c.dom.Node;

import java.io.IOException;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * TODO : configurable
 */
public class CrawlFilter extends Configured {

    public static final Logger LOG = CrawlFilters.LOG;
    /**
     * TODO : use suffix-urlfilter instead
     */
    public static final String[] MEDIA_URL_SUFFIXES = {"js", "css", "jpg", "png", "jpeg", "gif"};
    /**
     * The follow patterns are simple rule to indicate a url's category, this is a very simple solution, and the result is
     * not accurate
     */
    public static Pattern[] INDEX_PAGE_URL_PATTERNS = {
            Pattern.compile(".+tieba.baidu.com/.+search.+"),
            Pattern.compile(".+(index|list|tags|chanel).+")
    };
    public static Pattern SEARCH_PAGE_URL_PATTERN = Pattern.compile(".+(search|query|select).+");
    public static Pattern[] DETAIL_PAGE_URL_PATTERNS = {
            Pattern.compile(".+tieba.baidu.com/p/(\\d+)"),
            Pattern.compile(".+(detail|item|article|book|good|product|thread|view|post|content|/20[012][0-9]/{0,1}[01][0-9]/|/20[012]-[0-9]{0,1}-[01][0-9]/|/\\d{2,}/\\d{5,}|\\d{7,}).+")
    };
    public static Pattern MEDIA_PAGE_URL_PATTERN = Pattern.compile(".+(pic|picture|photo|avatar|photoshow|video).+");
    private PageCategory pageCategory = PageCategory.UNKNOWN;
    private String urlRegexRule;
    private TextFilter textFilter;
    private BlockFilter blockFilter;
    private String startKey = null;
    private String endKey = null;
    private String reversedStartKey;
    private String reversedEndKey;

    public CrawlFilter() {

    }

    public CrawlFilter(ImmutableConfig conf) {
        setConf(conf.unbox());
    }

    /**
     * A simple regex rule to sniff the possible category of a web page
     */
    public static PageCategory sniffPageCategory(String urlString) {
        PageCategory pageCategory = PageCategory.UNKNOWN;

        if (StringUtils.isEmpty(urlString)) {
            return pageCategory;
        }

        final String url = urlString.toLowerCase();

        // Notice : ***DO KEEP*** the right order
        if (url.endsWith("/")) {
            pageCategory = PageCategory.INDEX;
        } else if (StringUtils.countMatches(url, "/") <= 3) {
            // http://t.tt/12345678
            pageCategory = PageCategory.INDEX;
        } else if (Stream.of(INDEX_PAGE_URL_PATTERNS).anyMatch(pattern -> pattern.matcher(url).matches())) {
            pageCategory = PageCategory.INDEX;
        } else if (Stream.of(DETAIL_PAGE_URL_PATTERNS).anyMatch(pattern -> pattern.matcher(url).matches())) {
            pageCategory = PageCategory.DETAIL;
        } else if (SEARCH_PAGE_URL_PATTERN.matcher(url).matches()) {
            pageCategory = PageCategory.SEARCH;
        } else if (MEDIA_PAGE_URL_PATTERN.matcher(url).matches()) {
            pageCategory = PageCategory.MEDIA;
        }

        return pageCategory;
    }

    public static boolean keyGreaterEqual(String test, String bound) {
        if (test == null) {
            return false;
        }

        if (bound == null) {
            return true;
        }

        return test.compareTo(bound) >= 0;
    }

    public static boolean keyLessEqual(String test, String bound) {
        if (test == null) {
            return false;
        }

        if (bound == null) {
            return true;
        }

        return test.compareTo(bound) <= 0;
    }

    public static void main(String[] args) throws Exception {
        String test = "com.github:https/about";
        String bound = "com.github:https/\uFFFF";
        System.out.println(test.compareTo(bound));
    }

    @Override
    public void setConf(Configuration conf) {
        super.setConf(conf);
        initialize();
    }

    public void initialize() {
        try {
//      filter = conf.getBoolean(CRAWL_FILTER_FILTER, true);
//      if (filter) {
//        filters = new UrlFilters(conf);
//      }
//      normalise = conf.getBoolean(CRAWL_FILTER_NORMALISE, true);
//      if (normalise) {
//        normalizers = new UrlNormalizers(conf, UrlNormalizers.SCOPE_GENERATE_HOST_COUNT);
//      }

            // CrawlFilter specified RegexURLFilter
            if (urlRegexRule != null) {
                // urlFilter = new RegexURLFilter(urlRegexRule);
            }

            // TODO : move to Table utils just like persist project does
            if (startKey != null) {
                startKey = startKey.replaceAll("\\u0001", "\u0001");
                startKey = startKey.replaceAll("\\\\u0001", "\u0001");

                reversedStartKey = UrlUtil.reverseUrl(startKey);
            }

            if (endKey != null) {
                endKey = endKey.replaceAll("\\uFFFF", "\uFFFF");
                endKey = endKey.replaceAll("\\\\uFFFF", "\uFFFF");

                reversedEndKey = UrlUtil.reverseUrl(endKey);
            }
        } catch (RuntimeException | IOException e) {
            LOG.error(StringUtil.stringifyException(e));
        }
    }

    public WebPage filter(WebPage page) {
        return page;
    }

    public boolean testUrlSatisfied(String url) {
        if (url == null) {
            return false;
        }

        boolean passed = true;

//    try {
//      if (normalizers != null) {
//        url = normalizers.normalize(url, UrlNormalizers.SCOPE_DEFAULT);
//        passed = url != null;
//      }
//
//      if (passed && filters != null) {
//        url = filters.filter(url);
//        passed = url != null;
//      }
//
//      if (passed && urlFilter != null) {
//        url = urlFilter.filter(url);
//        passed = url != null;
//      }
//    } catch (MalformedURLException|UrlFilterException e) {
//      LOG.error(e.toString());
//      passed = false;
//    }

//    if (passed && urlFilter != null) {
//      url = urlFilter.filter(url);
//      passed = url != null;
//    }

        return passed;
    }

    public boolean testKeyRangeSatisfied(String reversedUrl) {
        return keyGreaterEqual(reversedUrl, reversedStartKey) && keyLessEqual(reversedUrl, reversedEndKey);
    }

    public boolean testTextSatisfied(String text) {
        if (text == null) {
            return false;
        }

        if (textFilter == null) {
            return true;
        }

        return textFilter.test(text);
    }

    public boolean isAllowed(Node node) {
        if (blockFilter == null) {
            return true;
        }

        return blockFilter.isAllowed(node);
    }

    public boolean isDisallowed(Node node) {
        return !isAllowed(node);
    }

    public boolean isDetailUrl(String url) {
        return (pageCategory == PageCategory.DETAIL) && testUrlSatisfied(url);
    }

    public boolean isSearchUrl(String url) {
        return (pageCategory == PageCategory.SEARCH) && testUrlSatisfied(url);
    }

    public boolean isMediaUrl(String url) {
        return (pageCategory == PageCategory.MEDIA) && testUrlSatisfied(url);
    }

    public boolean isIndexUrl(String url) {
        return pageCategory.equals(PageCategory.INDEX) && testUrlSatisfied(url);
    }

    public PageCategory getPageType() {
        return pageCategory;
    }

    public void setPageType(PageCategory pageCategory) {
        this.pageCategory = pageCategory;
    }

    public String getUrlFilter() {
        return urlRegexRule;
    }

    public void setUrlFilter(String urlFilter) {
        this.urlRegexRule = urlFilter;
    }

    public TextFilter getTextFilter() {
        return textFilter;
    }

    public void setTextFilter(TextFilter textFilter) {
        this.textFilter = textFilter;
    }

    public BlockFilter getBlockFilter() {
        return blockFilter;
    }

    public void setBlockFilter(BlockFilter blockFilter) {
        this.blockFilter = blockFilter;
    }

    public String getStartKey() {
        return startKey;
    }

    public void setStartKey(String startKey) {
        this.startKey = startKey;
    }

    public String getReversedStartKey() {
        return reversedStartKey;
    }

    public String getEndKey() {
        return endKey;
    }

    public void setEndKey(String endKey) {
        this.endKey = endKey;
    }

    public String getReversedEndKey() {
        return reversedEndKey;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation().create();
        return gson.toJson(this);
    }
}
