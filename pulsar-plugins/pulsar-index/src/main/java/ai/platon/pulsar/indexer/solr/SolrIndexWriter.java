/*
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
 */
package ai.platon.pulsar.indexer.solr;

import ai.platon.pulsar.common.DateTimes;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.crawl.index.IndexDocument;
import ai.platon.pulsar.crawl.index.IndexField;
import ai.platon.pulsar.crawl.index.IndexWriter;
import ai.platon.pulsar.crawl.index.IndexerMapping;
import ai.platon.pulsar.persist.HyperLink;
import ai.platon.pulsar.persist.WebDb;
import ai.platon.pulsar.persist.WebPage;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import static ai.platon.pulsar.common.config.CapabilityTypes.*;

public class SolrIndexWriter implements IndexWriter {

    public static final Logger LOG = LoggerFactory.getLogger(SolrIndexWriter.class);
    public static final String INDEXER_PARAMS = "index.additional.params";
    public static final String INDEXER_DELETE = "index.delete";
    public static final String INDEXER_REPORT_PAGE_HOME = "http://pulsar.platon.ai/report/indexer";
    private final List<SolrInputDocument> inputDocs = new ArrayList<>();
    private final List<SolrInputDocument> updateDocs = new ArrayList<>();
    private final List<String> deleteIds = new ArrayList<>();
    private ImmutableConfig conf;
    private String[] solrUrls = ArrayUtils.EMPTY_STRING_ARRAY;
    private String[] zkHosts = ArrayUtils.EMPTY_STRING_ARRAY;
    private String collection;
    private List<SolrClient> solrClients;
    private IndexerMapping indexerMapping;
    private ModifiableSolrParams solrParams;
    private WebDb webDb;
    private boolean isActive = false;
    private int batchSize;
    private int numDeletes = 0;
    private int totalAdds = 0;
    private int totalDeletes = 0;
    private int totalUpdates = 0;
    private boolean delete = false;
    private boolean writeFile = false;

    public SolrIndexWriter(IndexerMapping indexerMapping, ImmutableConfig conf) {
        this.indexerMapping = indexerMapping;
        setup(conf);
    }

    @Override
    public void setup(ImmutableConfig conf) {
        this.conf = conf;

        solrUrls = conf.getStrings(INDEXER_URL, ArrayUtils.EMPTY_STRING_ARRAY);
        zkHosts = conf.getStrings(INDEXER_ZK, ArrayUtils.EMPTY_STRING_ARRAY);
        collection = conf.get(INDEXER_COLLECTION);

        if (solrUrls == null && zkHosts == null) {
            String message = "Either Zookeeper URL or SOLR URL is required";
            message += "\n" + describe();
            LOG.error(message);
            throw new RuntimeException("Failed to init SolrIndexWriter");
        }

        batchSize = conf.getInt(INDEXER_WRITE_COMMIT_SIZE, 250);
        delete = conf.getBoolean(INDEXER_DELETE, false);
        solrParams = new ModifiableSolrParams();
        conf.getKvs(INDEXER_PARAMS).forEach((key, value) -> solrParams.add(key, value));

        LOG.info(getParams().format());
    }

    @Override
    public Params getParams() {
        return Params.of(
                "className", this.getClass().getSimpleName(),
                "batchSize", batchSize,
                "delete", delete,
                "solrParams", solrParams,
                "zkHosts", StringUtils.join(zkHosts, ", "),
                "solrUrls", StringUtils.join(solrUrls, ", "),
                "collection", collection
        );
    }

    public WebDb getWebDb() {
        return webDb;
    }

    public void setWebDb(WebDb webDb) {
        this.webDb = webDb;
    }

    public boolean isActive() {
        return isActive;
    }

    public void open(ImmutableConfig conf) {
        solrClients = SolrUtils.getSolrClients(solrUrls, zkHosts, collection);
        isActive = true;
    }

    public void open(String solrUrl) {
        solrClients = Lists.newArrayList(SolrUtils.getSolrClient(solrUrl));
        isActive = true;
    }

    public void deleteByQuery(String query) throws IOException {
        try {
            LOG.info("SolrWriter: deleting " + query);
            for (SolrClient solrClient : solrClients) {
                solrClient.deleteByQuery(query);
            }
        } catch (final SolrServerException e) {
            LOG.error("Error deleting: " + deleteIds);
            throw new IOException(e);
        }
    }

    @Override
    public void delete(String key) throws IOException {
        try {
            key = URLDecoder.decode(key, "UTF8");
        } catch (UnsupportedEncodingException e) {
            LOG.error("Error decoding: " + key);
            throw new IOException("UnsupportedEncodingException for " + key);
        } catch (IllegalArgumentException e) {
            LOG.warn("Could not decode: " + key + ", it probably wasn't encoded in the first place..");
        }

        // Escape solr hash separator
        key = key.replaceAll("!", "\\!");

        if (delete) {
            deleteIds.add(key);
            totalDeletes++;
        }

        if (deleteIds.size() >= batchSize) {
            push();
        }
    }

    @Override
    public void update(IndexDocument doc) throws IOException {
        write(doc);
    }

    @Override
    public void write(IndexDocument doc) throws IOException {
        final SolrInputDocument inputDoc = new SolrInputDocument();

        for (Entry<CharSequence, IndexField> e : doc.getFields().entrySet()) {
            String key = indexerMapping.mapKeyIfExists(e.getKey().toString());
            if (key == null) {
                continue;
            }

            float weight = e.getValue().getWeight();
            for (final Object field : e.getValue().getValues()) {
                // normalise the string representation for a Date
                Object val2 = convertIndexField(field);

                boolean isMultiValued = indexerMapping.isMultiValued(e.getKey().toString());
                if (!isMultiValued) {
                    if (inputDoc.getField(key) == null) {
                        inputDoc.addField(key, val2, weight);
                    }
                } else {
                    inputDoc.addField(key, val2, weight);
                }
            } // for
        } // for

        inputDoc.setDocumentBoost(doc.getWeight());
        inputDocs.add(inputDoc);
        totalAdds++;

        if (inputDocs.size() + numDeletes >= batchSize) {
            push();
        }
    }

    private Object convertIndexField(Object field) {
        Object field2;
        if (field instanceof Date) {
            field2 = DateTimes.INSTANCE.isoInstantFormat((Date) field);
        } else if (field instanceof Instant) {
            field2 = DateTimes.isoInstantFormat((Instant) field);
        } else if (field instanceof org.apache.avro.util.Utf8) {
            field2 = field.toString();
        }

        return field;
    }

    @Override
    public void close() throws IOException {
        if (!isActive) {
            return;
        }

        commit();

        for (SolrClient solrClient : solrClients) {
            solrClient.close();
        }
        solrClients.clear();

        isActive = false;
    }

    @Override
    public void commit() throws IOException {
        if (!isActive || inputDocs.isEmpty()) {
            return;
        }

        push();

        try {
            for (SolrClient solrClient : solrClients) {
                solrClient.commit();
            }
        } catch (SolrServerException | SolrException e) {
            LOG.error("Failed to write to solr " + e.toString());
            LOG.info(describe());
            throw new IOException(e);
        }
    }

    public void push() throws IOException {
        if (inputDocs.size() > 0) {
            String message = "Indexing " + inputDocs.size() + "/" + totalAdds + " documents";
            if (numDeletes > 0) {
                message += ", deleting " + numDeletes + " ones";
            }
            LOG.info(message);

            numDeletes = 0;

            UpdateRequest req = new UpdateRequest();
            req.add(inputDocs);
            req.setAction(AbstractUpdateRequest.ACTION.OPTIMIZE, false, false);
            req.setParams(solrParams);

            try {
                for (SolrClient solrClient : solrClients) {
                    solrClient.request(req);
                }
            } catch (SolrServerException | SolrException e) {
                LOG.error("Failed to write to solr " + e.toString());
                LOG.info(describe());
                throw new IOException(e);
            } finally {
                reportFailure(inputDocs);
                inputDocs.clear();
            }
        }

        if (deleteIds.size() > 0) {
            try {
                LOG.info("SolrIndexer: deleting "
                        + String.valueOf(deleteIds.size()) + "/" + String.valueOf(totalDeletes) + " documents");

                for (SolrClient solrClient : solrClients) {
                    solrClient.deleteById(deleteIds);
                }
            } catch (final SolrServerException | SolrException e) {
                LOG.error("Error deleting: " + deleteIds);
                throw new IOException(e);
            } finally {
                // reportFailure(deleteIds);
                deleteIds.clear();
            }
        }
    }

    private void reportFailure(List<SolrInputDocument> failedDocs) {
        if (webDb == null) return;

        // TODO: use TaskStatusTracker
        WebPage page = WebPage.newInternalPage(INDEXER_REPORT_PAGE_HOME + "/solr/failure", "Failed solr indexing pages");
        failedDocs.stream()
                .map(doc -> doc.get("url").getValue().toString())
                .map(HyperLink::new)
                .forEach(l -> page.getVividLinks().put(l.getUrl(), ""));
        webDb.put(page);
        webDb.flush();
    }

    public String describe() {
        StringBuilder sb = new StringBuilder("SOLRIndexWriter\n");
        sb.append("\t").append(INDEXER_URL).append(" : URL of the indexer instance\n");
        sb.append("\t").append(INDEXER_ZK).append(" : URL of the Zookeeper quorum\n");
        sb.append("\t").append(INDEXER_COLLECTION).append(" : indexer collection\n");
        sb.append("\t").append(INDEXER_WRITE_COMMIT_SIZE).append(" : buffer size when sending to SOLR (default 1000)\n");
        sb.append("\t").append(IndexerMapping.PARAM_INDEXER_MAPPING_FILE)
                .append(" : name of the mapping file for fields (default solrindex-mapping.xml)\n");
        sb.append("\t").append(SolrConstants.USE_AUTH).append(" : use authentication (default false)\n");
        sb.append("\t").append(SolrConstants.USERNAME).append(" : username for authentication\n");
        sb.append("\t").append(SolrConstants.PASSWORD).append(" : password for authentication\n");
        return sb.toString();
    }
}
