package org.warps.pulsar.jobs.basic.parse;

import org.slf4j.Logger;
import org.warps.pulsar.common.config.Params;
import org.warps.pulsar.crawl.parse.PageParser;
import org.warps.pulsar.crawl.parse.ParseResult;
import org.warps.pulsar.jobs.core.GoraMapper;
import org.warps.pulsar.persist.CrawlMarks;
import org.warps.pulsar.persist.WebPage;
import org.warps.pulsar.persist.gora.generated.GWebPage;
import org.warps.pulsar.persist.metadata.Mark;

import java.io.IOException;

import static org.warps.pulsar.common.config.CapabilityTypes.*;

public class ParserMapper extends GoraMapper<String, GWebPage, String, GWebPage> {

    public static final Logger LOG = ParserJob.LOG;

    private PageParser pageParser;
    private boolean resume;
    private boolean force;
    private boolean reparse;

    @Override
    public void setup(Context context) throws IOException, InterruptedException {
        pageParser = new PageParser(conf);
        resume = conf.getBoolean(RESUME, false);
        reparse = conf.getBoolean(PARSE_REPARSE, false);
        force = conf.getBoolean(FORCE, false);

        LOG.info(Params.formatAsLine(
                "resume", resume,
                "reparse", reparse,
                "force", force
        ));
    }

    @Override
    public void map(String reversedUrl, GWebPage row, Context context) throws IOException, InterruptedException {
        WebPage page = WebPage.box(reversedUrl, row, true);
        String url = page.getUrl();

        if (!shouldProcess(page)) {
            return;
        }

        ParseResult parseResult = pageParser.parse(page);

        context.write(reversedUrl, page.unbox());
    }

    private boolean shouldProcess(WebPage page) {
        CrawlMarks marks = page.getMarks();
        if (!reparse && !marks.contains(Mark.FETCH)) {
            metricsCounters.increase(PageParser.Counter.notFetched);
            return false;
        }

        if (!reparse && resume && marks.contains(Mark.PARSE)) {
            if (!force) {
                metricsCounters.increase(PageParser.Counter.alreadyParsed);
                return false;
            }
        }

        return true;
    }
}
