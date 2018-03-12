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

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.warps.pulsar.common.config.ImmutableConfig;
import org.warps.pulsar.common.config.Params;
import org.warps.pulsar.crawl.index.IndexDocument;
import org.warps.pulsar.crawl.index.IndexingException;
import org.warps.pulsar.crawl.index.IndexingFilter;
import org.warps.pulsar.persist.WebPage;

import java.util.Map;

/**
 * Adds basic searchable fields to a document. The fields are: host - add host
 * as un-stored, indexed and tokenized url - url is both stored and indexed, so
 * it's both searchable and returned. This is also a required field. content -
 * content is indexed, so that it's searchable, but not stored in index title -
 * title is stored and indexed cache - add cached content/summary display
 * policy, if available tstamp - add timestamp when fetched, for deduplication
 */
public class GeneralIndexingFilter implements IndexingFilter {

    private static int maxContentLength;

    private ImmutableConfig conf;

    public GeneralIndexingFilter() {

    }

    public GeneralIndexingFilter(ImmutableConfig conf) {
        reload(conf);
    }

    /**
     * The {@link GeneralIndexingFilter} filter object which supports boolean
     * configurable value for length of characters permitted within the title @see
     * {@code index.max.title.length} in pulsar-default.xml
     *
     * @param doc
     *          The {@link IndexDocument} object
     * @param url
     *          URL to be filtered for anchor text
     * @param page
     *          {@link WebPage} object relative to the URL
     * @return filtered IndexDocument
     * */
    public IndexDocument filter(IndexDocument doc, String url, WebPage page) throws IndexingException {
        doc.addIfAbsent("id", doc.getKey());
        doc.addIfAbsent("url", url);
        doc.addIfAbsent("seed_url", StringUtils.substringBefore(page.getOptions().toString(), " "));
        addDocFields(doc, url, page);
        return doc;
    }

    private void addDocFields(IndexDocument doc, String url, WebPage page) {
        // Major page entities
        page.getPageModel().unbox().forEach(p -> addDocFields(doc, p.getFields()));

        // Secondary page entities
        // page.getPageModel().getRawPageEntities().forEach(pe -> addDocFields(doc, pe.getFields()));
    }

    private void addDocFields(IndexDocument doc, Map<CharSequence, CharSequence> fields) {
        fields.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().length() < maxContentLength)
                .forEach(e -> doc.addIfAbsent(e.getKey(), e.getValue()));
    }

    /**
     * Set the {@link Configuration} object
     * */
    @Override
    public void reload(ImmutableConfig conf) {
        this.conf = conf;

        maxContentLength = conf.getInt("index.max.content.length", 10 * 10000);

        LOG.info(getParams().formatAsLine());
    }

    @Override
    public Params getParams() {
        return Params.of(
                "className", this.getClass().getSimpleName(),
                "maxContentLength", maxContentLength
        );
    }

    /**
     * Get the {@link Configuration} object
     */
    public ImmutableConfig getConf() {
        return this.conf;
    }
}
