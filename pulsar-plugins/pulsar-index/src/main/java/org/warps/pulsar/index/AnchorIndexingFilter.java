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
package org.warps.pulsar.index;

import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.warps.pulsar.common.config.ImmutableConfig;
import org.warps.pulsar.common.config.Params;
import org.warps.pulsar.crawl.index.IndexDocument;
import org.warps.pulsar.crawl.index.IndexingException;
import org.warps.pulsar.crawl.index.IndexingFilter;
import org.warps.pulsar.persist.WebPage;

import java.util.HashSet;
import java.util.Map.Entry;

/**
 * Indexing filter that offers an option to either index all inbound anchor text
 * for a document or deduplicate anchors. Deduplication does have it's con's,
 *
 */
public class AnchorIndexingFilter implements IndexingFilter {

    public static final Logger LOG = LoggerFactory.getLogger(AnchorIndexingFilter.class);
    private ImmutableConfig conf;
    private boolean deduplicate = false;

    public AnchorIndexingFilter() {
    }

    public AnchorIndexingFilter(ImmutableConfig conf) {
        this.conf = conf;
    }

    /**
     * Set the {@link Configuration} object
     */
    @Override
    public void reload(ImmutableConfig conf) {
        this.conf = conf;

        deduplicate = conf.getBoolean("anchorIndexingFilter.deduplicate", true);
    }

    @Override
    public Params getParams() {
        return Params.of("anchor.indexing.filter.deduplicate", deduplicate);
    }

    /**
     * Get the {@link Configuration} object
     */
    @Override
    public ImmutableConfig getConf() {
        return this.conf;
    }

    public void addIndexBackendOptions(Configuration conf) {
    }

    /**
     * The {@link AnchorIndexingFilter} filter object which supports boolean
     * configuration settings for the deduplication of anchors. See
     * {@code anchorIndexingFilter.deduplicate} in pulsar-default.xml.
     *
     * @param doc
     *          The {@link IndexDocument} object
     * @param url
     *          URL to be filtered for anchor text
     * @param page
     *          {@link WebPage} object relative to the URL
     * @return filtered IndexDocument
     */
    @Override
    public IndexDocument filter(IndexDocument doc, String url, WebPage page) throws IndexingException {
        HashSet<String> set = null;

        for (Entry<CharSequence, CharSequence> e : page.getInlinks().entrySet()) {
            String anchor = e.getValue().toString();

            if (anchor.equals(""))
                continue;

            if (deduplicate) {
                if (set == null)
                    set = new HashSet<>();
                String lcAnchor = anchor.toLowerCase();

                // Check if already processed the current anchor
                if (!set.contains(lcAnchor)) {
                    doc.add("anchor", anchor);

                    // Add to set
                    set.add(lcAnchor);
                }
            } else {
                doc.add("anchor", anchor);
            }
        }

        return doc;
    }
}
