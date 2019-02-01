package org.platon.pulsar.jobs.basic.parse;

import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.persist.CrawlMarks;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import ai.platon.pulsar.persist.metadata.Mark;
import ai.platon.pulsar.crawl.parse.PageParser;
import ai.platon.pulsar.crawl.parse.ParseResult;
import org.platon.pulsar.jobs.core.GoraMapper;
import org.slf4j.Logger;

import java.io.IOException;

import static ai.platon.pulsar.common.config.CapabilityTypes.*;

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
