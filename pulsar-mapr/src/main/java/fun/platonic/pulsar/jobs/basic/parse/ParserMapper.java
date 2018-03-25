package fun.platonic.pulsar.jobs.basic.parse;

import fun.platonic.pulsar.common.config.Params;
import fun.platonic.pulsar.crawl.parse.PageParser;
import fun.platonic.pulsar.crawl.parse.ParseResult;
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
