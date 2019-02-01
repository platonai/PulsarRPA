package org.platon.pulsar.jobs.basic.index;

import ai.platon.pulsar.common.MetricsCounters;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.persist.CrawlMarks;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import ai.platon.pulsar.persist.metadata.Mark;
import ai.platon.pulsar.crawl.index.IndexDocument;
import ai.platon.pulsar.crawl.index.IndexWriters;
import ai.platon.pulsar.crawl.index.IndexingFilters;
import org.platon.pulsar.jobs.core.GoraMapper;
import org.slf4j.Logger;

import java.io.IOException;

import static ai.platon.pulsar.common.config.CapabilityTypes.*;

public class IndexMapper extends GoraMapper<String, GWebPage, String, GWebPage> {

    public static final Logger LOG = IndexJob.LOG;

    static {
        MetricsCounters.register(Counter.class);
    }

    private boolean resume;
    private boolean force;
    private boolean repindex;
    private IndexingFilters indexingFilters = new IndexingFilters();
    private IndexWriters indexWriters = new IndexWriters();

    @Override
    public void setup(Context context) throws IOException, InterruptedException {
        resume = conf.getBoolean(RESUME, false);
        repindex = conf.getBoolean(PARSE_REPARSE, false);
        force = conf.getBoolean(FORCE, false);

        LOG.info(Params.format(
                "resume", resume,
                "repindex", repindex,
                "force", force,
                "indexingFilters", indexingFilters,
                "indexWriters", indexWriters
        ));
    }

    @Override
    public void map(String reversedUrl, GWebPage row, Context context) throws IOException, InterruptedException {
        WebPage page = WebPage.box(reversedUrl, row, true);
        String url = page.getUrl();

        if (!shouldProcess(page)) {
            return;
        }

        IndexDocument doc = indexingFilters.filter(new IndexDocument(page.getKey()), page.getUrl(), page);
        if (doc == null) {
            return;
        }

        indexWriters.write(doc);

        CrawlMarks marks = page.getMarks();
        marks.putIfNonNull(Mark.INDEX, marks.get(Mark.PARSE));
        context.write(reversedUrl, page.unbox());
    }

    private boolean shouldProcess(WebPage page) {
        CrawlMarks marks = page.getMarks();

        if (!repindex && !marks.contains(Mark.PARSE)) {
            metricsCounters.increase(Counter.notIndexed);
            return false;
        }

        if (!repindex && resume && marks.contains(Mark.INDEX)) {
            if (!force) {
                return false;
            }
        }

        return true;
    }

    public enum Counter {notIndexed}
}
