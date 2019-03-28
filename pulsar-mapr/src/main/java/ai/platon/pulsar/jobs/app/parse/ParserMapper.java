package ai.platon.pulsar.jobs.app.parse;

import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.crawl.parse.PageParser;
import ai.platon.pulsar.crawl.parse.ParseResult;
import ai.platon.pulsar.jobs.core.GoraMapper;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import ai.platon.pulsar.persist.metadata.Mark;
import org.apache.avro.util.Utf8;
import org.slf4j.Logger;

import java.io.IOException;

import static ai.platon.pulsar.common.config.CapabilityTypes.*;
import static ai.platon.pulsar.common.config.PulsarConstants.ALL_BATCHES;

public class ParserMapper extends GoraMapper<String, GWebPage, String, GWebPage> {

  public static final Logger LOG = ParserJob.LOG;

  private PageParser pageParser;
  private boolean resume;
  private boolean force;
  private boolean reparse;
  private Utf8 batchId;
  private int limit = -1;
  private boolean skipTruncated;

  private int count = 0;

  @Override
  public void setup(Context context) {
    batchId = new Utf8(conf.get(BATCH_ID, ALL_BATCHES));
    pageParser = new PageParser(conf);
    resume = conf.getBoolean(RESUME, false);
    reparse = conf.getBoolean(PARSE_REPARSE, false);
    force = conf.getBoolean(FORCE, false);
    limit = conf.getInt(LIMIT, -1);
    skipTruncated = conf.getBoolean(PARSE_SKIP_TRUNCATED, true);

    LOG.info(Params.format(
        "batchId", batchId,
        "resume", resume,
        "reparse", reparse,
        "force", force,
        "limit", limit,
        "skipTruncated", skipTruncated
    ));
  }

  @Override
  public void map(String reversedUrl, GWebPage row, Context context) throws IOException, InterruptedException {
    WebPage page = WebPage.box(reversedUrl, row, true);
    String url = page.getUrl();

    if (limit > -1 && count > limit) {
      stop("hit limit " + limit + ", finish mapper.");
      return;
    }

    if (!shouldProcess(page)) {
      return;
    }

    ParseResult parseResult = pageParser.parse(page);

    context.write(reversedUrl, page.unbox());

    ++count;
  }

  private boolean shouldProcess(WebPage page) {
    if (!reparse && !page.hasMark(Mark.FETCH)) {
      metricsCounters.increase(PageParser.Counter.notFetched);

      if (LOG.isDebugEnabled()) {
//        LOG.debug("Skipping " + TableUtil.unreverseUrl(key) + "; not fetched yet");
      }

      return false;
    }

    if (!reparse && resume && page.hasMark(Mark.PARSE)) {
      metricsCounters.increase(PageParser.Counter.alreadyParsed);

      if (!force) {
        LOG.debug("Skipping " + page.getUrl() + "; already parsed");
        return false;
      }

      LOG.debug("Forced parsing " + page.getUrl() + "; already parsed");
    } // if resume

    if (skipTruncated && pageParser.isTruncated(page)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Page truncated, ignore");
      }

      metricsCounters.increase(PageParser.Counter.truncated);

      return false;
    }

    return true;
  }

} // ParserMapper
