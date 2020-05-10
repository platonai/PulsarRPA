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

package ai.platon.pulsar.index;

import ai.platon.pulsar.common.DateTimes;
import ai.platon.pulsar.crawl.common.URLUtil;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.crawl.index.IndexDocument;
import ai.platon.pulsar.crawl.index.IndexingException;
import ai.platon.pulsar.crawl.index.IndexingFilter;
import ai.platon.pulsar.persist.WebPage;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Indexer which can be configured to extract metadata from the crawldb, parse
 * metadata or content metadata. You can specify the properties "index.db",
 * "index.parse" or "index.content" who's values are comma-delimited
 * <value>key1,key2,key3</value>.
 */
public class MetadataIndexer implements IndexingFilter {
    private static final String PARSE_CONF_PROPERTY = "index.metadata";
    private static final String INDEX_PREFIX = "meta_";
    private static final String PARSE_META_PREFIX = "meta_";

    private static Map<String, String> parseFieldnames = new TreeMap<>();

    private ImmutableConfig conf;

    public MetadataIndexer() {
    }

    public MetadataIndexer(ImmutableConfig conf) {
        setup(conf);
    }

    @Override
    public void setup(ImmutableConfig conf) {
        this.conf = conf;

        conf.getStringCollection(PARSE_CONF_PROPERTY).forEach(metatag -> {
            String key = PARSE_META_PREFIX + metatag.toLowerCase(Locale.ROOT);
            String value = INDEX_PREFIX + metatag;

            parseFieldnames.put(key, value);
        });
    }

    public Params getParams() {
        return new Params();
    }

    public IndexDocument filter(IndexDocument doc, String url, WebPage page) throws IndexingException {
        try {
            addTime(doc, url, page);

            addHost(doc, url, page);

            // MultiMetadata-index does not meet all our requirement
            addGeneralMetadata(doc, url, page);

            addPageMetadata(doc, url, page);
        } catch (IndexingException e) {
            LOG.error(e.toString());
        }

        return doc;
    }

    private void addHost(IndexDocument doc, String url, WebPage page) throws IndexingException {
        String reprUrlString = page.getReprUrl();

        url = reprUrlString.isEmpty() ? url : reprUrlString;

        if (url == null || url.isEmpty()) {
            return;
        }

        try {
            URL u = new URL(url);

            String domain = URLUtil.INSTANCE.getDomainName(u);

            doc.add("url", url);
            doc.add("domain", domain);
            doc.addIfNotNull("host", u.getHost());
        } catch (MalformedURLException e) {
            throw new IndexingException(e);
        }
    }

    private void addTime(IndexDocument doc, String url, WebPage page) {
        Instant now = Instant.now();

        String crawlTimeStr = DateTimes.isoInstantFormat(now);
        Instant firstCrawlTime = page.getFirstCrawlTime(now);
        String fetchTimeHistory = page.getFetchTimeHistory(crawlTimeStr);

        doc.add("first_crawl_time", DateTimes.isoInstantFormat(firstCrawlTime));
        doc.add("last_crawl_time", crawlTimeStr);
        doc.add("fetch_time_history", fetchTimeHistory);

        String indexTimeStr = DateTimes.isoInstantFormat(now);
        Instant firstIndexTime = page.getFirstIndexTime(now);
        String indexTimeHistory = page.getIndexTimeHistory(indexTimeStr);

        doc.add("first_index_time", DateTimes.isoInstantFormat(firstIndexTime));
        doc.add("last_index_time", indexTimeStr);
        doc.add("index_time_history", indexTimeHistory);
    }

    private void addGeneralMetadata(IndexDocument doc, String url, WebPage page) throws IndexingException {
        String contentType = page.getContentType();
        if (!contentType.contains("html")) {
            LOG.warn("Content type " + contentType + " is not fully supported");
            // return doc;
        }

        // get content type
        doc.add("content_type", contentType);
    }

    private IndexDocument addPageMetadata(IndexDocument doc, String url, WebPage page) {
        if (doc == null || parseFieldnames.isEmpty()) {
            return doc;
        }

        for (Map.Entry<String, String> metatag : parseFieldnames.entrySet()) {
            String k = metatag.getValue();
            String metadata = page.getMetadata().get(metatag.getKey());

            if (k != null && metadata != null) {
                k = k.trim();
                metadata = metadata.trim();

                if (!k.isEmpty() && !metadata.isEmpty()) {
                    final String finalK = k;

                    // TODO : avoid this dirty hard coding
                    if (finalK.equalsIgnoreCase("meta_description")) {
                        Arrays.stream(metadata.split("\t")).forEach(v -> doc.addIfAbsent(finalK, v));
                    } else {
                        Arrays.stream(metadata.split("\t")).forEach(v -> doc.add(finalK, v));
                    }
                }
            } // if
        } // for

        return doc;
    }

    @Override
    public ImmutableConfig getConf() {
        return this.conf;
    }
}
