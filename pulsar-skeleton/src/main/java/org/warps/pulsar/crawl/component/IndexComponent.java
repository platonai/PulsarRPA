package org.warps.pulsar.crawl.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.warps.pulsar.common.config.ImmutableConfig;
import org.warps.pulsar.common.config.Params;
import org.warps.pulsar.crawl.index.IndexDocument;
import org.warps.pulsar.crawl.index.IndexWriters;
import org.warps.pulsar.crawl.index.IndexingFilters;
import org.warps.pulsar.persist.WebPage;

import java.io.IOException;
import java.time.Instant;

/**
 * Created by vincent on 16-9-8.
 * Copyright @ 2013-2016 Warpspeed Information. All rights reserved
 */
@Component
public class IndexComponent {

    public static final Logger LOG = LoggerFactory.getLogger(IndexComponent.class);

    private ImmutableConfig conf;
    private IndexingFilters indexingFilters;
    private IndexWriters indexWriters;
    private boolean indexWritersAreOpen = false;

    public IndexComponent(ImmutableConfig conf) {
        this.conf = conf;
    }

    public IndexComponent(String indexerUrl, ImmutableConfig conf) {
        this.conf = conf;

        open(indexerUrl);

        LOG.info(Params.format(
                "className", this.getClass().getSimpleName(),
                "indexerUrl", indexerUrl,
                "indexingFilters", indexingFilters,
                "indexWriters", indexWriters
        ));
    }

    public IndexingFilters getIndexingFilters() {
        return indexingFilters;
    }

    public void setIndexingFilters(IndexingFilters indexingFilters) {
        this.indexingFilters = indexingFilters;
    }

    public IndexWriters getIndexWriters() {
        return indexWriters;
    }

    public void setIndexWriters(IndexWriters indexWriters) {
        this.indexWriters = indexWriters;
    }

    public void open() {
        if (!indexWritersAreOpen) {
            this.indexWriters.open();
            indexWritersAreOpen = true;
        }
    }

    public void open(String indexerUrl) {
        if (!indexWritersAreOpen) {
            this.indexWriters.open(indexerUrl);
            indexWritersAreOpen = true;
        }
    }

    public IndexDocument index(WebPage page) {
        IndexDocument doc = new IndexDocument(page.getKey());
        doc = indexingFilters.filter(doc, page.getUrl(), page);
        if (doc != null && indexWriters != null) {
            indexWriters.write(doc);
        }
        page.putIndexTimeHistory(Instant.now());

        return doc;
    }

    public void commit() throws IOException {
        if (indexWriters != null) {
            indexWriters.commit();
        }
    }
}
