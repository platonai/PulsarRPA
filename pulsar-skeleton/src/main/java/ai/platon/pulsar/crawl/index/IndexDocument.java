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
package ai.platon.pulsar.crawl.index;

import ai.platon.pulsar.common.DateTimeUtil;
import ai.platon.pulsar.common.UrlUtil;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.crawl.scoring.ScoringFilters;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A {@link IndexDocument} is the unit of indexing.
 */
public class IndexDocument {

    private CharSequence key = "";
    private CharSequence url = "";
    private Map<CharSequence, IndexField> fields = new LinkedMap<>();
    private float weight = 1.0f;

    public IndexDocument() {
    }

    public IndexDocument(CharSequence key) {
        this.key = key;
        this.url = UrlUtil.unreverseUrl(key.toString());
    }

    public String getKey() {
        return key.toString();
    }

    public String getUrl() {
        return url.toString();
    }

    public void addIfAbsent(CharSequence name, Object value) {
        fields.computeIfAbsent(name, k -> new IndexField(value));
    }

    public void addIfNotEmpty(CharSequence name, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        IndexField field = fields.get(name);
        if (field == null) {
            field = new IndexField(value);
            fields.put(name, field);
        } else {
            field.add(value);
        }
    }

    public void addIfNotNull(CharSequence name, Object value) {
        if (value == null) {
            return;
        }
        IndexField field = fields.get(name);
        if (field == null) {
            field = new IndexField(value);
            fields.put(name, field);
        } else {
            field.add(value);
        }
    }

    public void add(CharSequence name, Object value) {
        IndexField field = fields.get(name);
        if (field == null) {
            field = new IndexField(value);
            fields.put(name, field);
        } else {
            field.add(value);
        }
    }

    public Object getFieldValue(CharSequence name) {
        IndexField field = fields.get(name);
        if (field == null) {
            return null;
        }
        if (field.getValues().size() == 0) {
            return null;
        }
        return field.getValues().get(0);
    }

    public IndexField getField(CharSequence name) {
        return fields.get(name);
    }

    public IndexField removeField(CharSequence name) {
        return fields.remove(name);
    }

    public Collection<CharSequence> getFieldNames() {
        return fields.keySet();
    }

    public List<Object> getFieldValues(CharSequence name) {
        IndexField field = fields.get(name);
        if (field == null) {
            return null;
        }

        return field.getValues();
    }

    public String getFieldValueAsString(CharSequence name) {
        IndexField field = fields.get(name);
        if (field == null || field.getValues().isEmpty()) {
            return null;
        }

        return field.getValues().iterator().next().toString();
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public Map<String, List<String>> asMultimap() {
        return fields.entrySet().stream()
                .map(e -> Pair.of(e.getKey().toString(), e.getValue().getStringValues()))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    @Override
    public String toString() {
        String s = fields.entrySet().stream()
                .map(e -> "\t" + e.getKey() + ":\t" + e.getValue().toString())
                .collect(Collectors.joining("\n"));
        return "doc {\n" + s + "\n}\n";
    }

    public String formatAsLine() {
        String s = fields.entrySet().stream()
                .map(e -> "\t" + e.getKey() + ":\t" + e.getValue().toString())
                .map(l -> StringUtils.replaceChars(l, "[]", ""))
                .collect(Collectors.joining(", "));
        return s;
    }

    public Map<CharSequence, IndexField> getFields() {
        return fields;
    }

    private String format(Object obj) {
        if (obj instanceof Date) {
            return DateTimeUtil.isoInstantFormat((Date) obj);
        } else if (obj instanceof Instant) {
            return DateTimeUtil.isoInstantFormat((Instant) obj);
        } else {
            return obj.toString();
        }
    }

    public static class Builder {
        private static final Log LOG = LogFactory.getLog(new Object() {
        }.getClass().getEnclosingClass());

        private ImmutableConfig conf;
        private IndexingFilters indexingFilters;
        private ScoringFilters scoringFilters;

        public Builder(ImmutableConfig conf) {
            this.conf = conf;
            indexingFilters = new IndexingFilters(conf);
            scoringFilters = new ScoringFilters(conf);
        }

        public static Builder newBuilder(ImmutableConfig conf) {
            return new Builder(conf);
        }

        public Builder with(IndexingFilters indexingFilters) {
            this.indexingFilters = indexingFilters;
            return this;
        }

        public Builder with(ScoringFilters scoringFilters) {
            this.scoringFilters = scoringFilters;
            return this;
        }

        /**
         * Index a {@link WebPage}, here we add the following fields:
         * <ol>
         * <li><tt>id</tt>: default uniqueKey for the {@link IndexDocument}.</li>
         * <li><tt>digest</tt>: Digest is used to identify pages (like unique ID)
         * and is used to remove duplicates during the dedup procedure. It is
         * calculated
         * <li><tt>batchId</tt>: The page belongs to a unique batchId, this is its
         * identifier.</li>
         * <li><tt>boost</tt>: Boost is used to calculate document (field) score
         * which can be used within queries submitted to the underlying indexing
         * library to find the best results. It's part of the scoring algorithms.
         * See scoring.link, scoring.opic, scoring.tld, etc.</li>
         * </ol>
         *
         * @param key  The key of the page (reversed url).
         * @param page The {@link WebPage}.
         * @return The indexed document, or null if skipped by index indexingFilters.
         */
        public IndexDocument build(String key, WebPage page) {
            if (key == null || page == null) {
                return null;
            }

            IndexDocument doc = new IndexDocument(key);

            String url = doc.getUrl();

            doc = indexingFilters.filter(doc, url, page);
            // skip documents discarded by indexing indexingFilters
            if (doc == null) {
                return null;
            }

            doc.addIfAbsent("id", key);

            doc.add("digest", page.getSignatureAsString());

            doc.add("batchId", page.getBatchId());

            float boost = 1.0f;
            // run scoring indexingFilters
            boost = scoringFilters.indexerScore(url, doc, page, boost);

            doc.setWeight(boost);
            // store boost for use by explain and dedup
            doc.add("boost", Float.toString(boost));

            return doc;
        }
    }
}
