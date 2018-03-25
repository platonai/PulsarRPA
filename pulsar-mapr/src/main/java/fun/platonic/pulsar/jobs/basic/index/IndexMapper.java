package fun.platonic.pulsar.jobs.basic.index;

import fun.platonic.pulsar.common.MetricsCounters;
import fun.platonic.pulsar.common.config.Params;
import fun.platonic.pulsar.crawl.index.IndexDocument;
import fun.platonic.pulsar.crawl.index.IndexWriters;
import fun.platonic.pulsar.crawl.index.IndexingFilters;
import org.slf4j.Logger;
import fun.platonic.pulsar.jobs.core.GoraMapper;
import fun.platonic.pulsar.persist.CrawlMarks;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.gora.generated.GWebPage;
import fun.platonic.pulsar.persist.metadata.Mark;

import java.io.IOException;

import static fun.platonic.pulsar.common.config.CapabilityTypes.FORCE;
import static fun.platonic.pulsar.common.config.CapabilityTypes.PARSE_REPARSE;
import static fun.platonic.pulsar.common.config.CapabilityTypes.RESUME;

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
