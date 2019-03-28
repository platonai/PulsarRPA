package ai.platon.pulsar.jobs.app.fetch;

import ai.platon.pulsar.common.CounterUtils;
import ai.platon.pulsar.common.LocalFSUtils;
import ai.platon.pulsar.common.MetricsCounters;
import ai.platon.pulsar.common.PulsarPaths;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.jobs.app.generate.GenerateJob;
import ai.platon.pulsar.jobs.common.FetchEntryWritable;
import ai.platon.pulsar.jobs.core.AppContextAwareGoraMapper;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import ai.platon.pulsar.persist.metadata.Mark;
import org.apache.hadoop.io.IntWritable;

import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static ai.platon.pulsar.common.config.CapabilityTypes.*;

/**
 * <p>
 * Mapper class for Fetcher.
 * </p>
 * <p>
 * This class reads the random integer written by {@link GenerateJob} as its
 * key while outputting the actual key and value arguments through a
 * {@link FetchEntryWritable} instance.
 * </p>
 * <p>
 * This approach (combined with the use of PartitionUrlByHost makes
 * sure that Fetcher is still polite while also randomizing the key order. If
 * one host has a huge number of URLs in your table while other hosts have
 * not, {@link FetchReducer} will not be stuck on one host but process URLs
 * from other hosts as well.
 * </p>
 */
public class FetchMapper extends AppContextAwareGoraMapper<String, GWebPage, IntWritable, FetchEntryWritable> {

  public enum Counter { mNotGenerated, mFetched, mHostGone, mSeeds }
  static { MetricsCounters.register(Counter.class); }

  private String batchId;
  private boolean resume;
  private int limit = 1000000;
  private int count = 0;

  private Random random = new Random();
  private Set<String> unreachableHosts = new HashSet<>();

  @Override
  public void setup(Context context) throws IOException, InterruptedException {
    String crawlId = conf.get(STORAGE_CRAWL_ID);
    String fetchMode = conf.get(FETCH_MODE);
    batchId = conf.get(BATCH_ID);
    int numTasks = conf.getInt(MAPREDUCE_JOB_REDUCES, 2);
    limit = conf.getUint(MAPPER_LIMIT, 1000000);
    limit = limit < 2 * numTasks ? limit : limit/numTasks;

    resume = conf.getBoolean(RESUME, false);

    unreachableHosts.addAll(LocalFSUtils.readAllLinesSilent(PulsarPaths.PATH_UNREACHABLE_HOSTS));

    LOG.info(Params.format(
        "className", this.getClass().getSimpleName(),
        "crawlId", crawlId,
        "fetchMode", fetchMode,
        "resume", resume,
        "numTasks", numTasks,
        "limit", limit,
        "unreachableHostsPath", PulsarPaths.PATH_UNREACHABLE_HOSTS,
        "unreachableHosts", unreachableHosts.size()
    ));
  }

  /**
   * Rows are filtered by batchId first in FetchJob setup, which can be a range search, the time complex is O(ln(N))
   * and then filtered by mapper, which is a scan, the time complex is O(N)
   * */
  @Override
  protected void map(String reversedUrl, GWebPage row, Context context) throws IOException, InterruptedException {
    WebPage page = WebPage.box(reversedUrl, row, true);

    if (!page.hasMark(Mark.GENERATE)) {
      metricsCounters.increase(Counter.mNotGenerated);
      return;
    }

    /*
     * Resume the batch, but ignore rows that are already fetched.
     * If FetchJob runs again but no resume flag set, the pages already fetched should be fetched again.
     * */
    if (page.hasMark(Mark.FETCH)) {
      metricsCounters.increase(Counter.mFetched);
      if (!resume) return;
    }

    // Higher priority comes first
    int shuffleOrder = random.nextInt(10000) - 10000 * page.getFetchPriority();
    context.write(new IntWritable(shuffleOrder), new FetchEntryWritable(conf.unbox(), page.getKey(), page));
    updateStatus(page);

    if (++count > limit) {
      stop("Hit limit " + limit + ", finish the mapper.");
    }
  }

  private void updateStatus(WebPage page) throws IOException, InterruptedException {
    CounterUtils.increaseMDepth(page.getDistance(), metricsCounters);

    if (page.isSeed()) {
      metricsCounters.increase(Counter.mSeeds);
    }
  }
}
