package ai.platon.pulsar.jobs.fetch;

import ai.platon.pulsar.common.ReducerContext;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.common.config.ReloadableParameterized;
import ai.platon.pulsar.jobs.common.FetchEntryWritable;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static ai.platon.pulsar.common.UrlUtil.unreverseUrl;
import static ai.platon.pulsar.common.config.CapabilityTypes.*;

/**
 * This class feeds the fetchMonitor with input items, and re-fills them as
 * items are consumed by FetcherThread-s.
 */
public class FeedThread extends Thread implements Comparable<FeedThread>, ReloadableParameterized {
  private final Logger LOG = FetchMonitor.LOG;

  private static AtomicInteger instanceSequence = new AtomicInteger(0);

  private final int id;
  private final ReducerContext context;
  private final FetchMonitor fetchMonitor;
  private final TaskScheduler taskScheduler;
  private final TaskMonitor tasksMonitor;

  private ImmutableConfig conf;
  private Instant jobDeadline;
  private Duration checkInterval = Duration.ofSeconds(2);
  private int fetchThreads;
  private int initBatchSize;
  private AtomicBoolean completed = new AtomicBoolean(false);

  private Iterator<FetchEntryWritable> currentIter;
  private int totalTaskCount = 0;

  @SuppressWarnings("rawtypes")
  public FeedThread(FetchMonitor fetchMonitor,
                    TaskScheduler taskScheduler,
                    TaskMonitor tasksMonitor,
                    ReducerContext context,
                    ImmutableConfig conf) {
    this.context = context;

    this.fetchMonitor = fetchMonitor;
    this.taskScheduler = taskScheduler;
    this.tasksMonitor = tasksMonitor;

    this.id = instanceSequence.incrementAndGet();

    this.setDaemon(true);
    this.setName(getClass().getSimpleName() + "-" + id);

    reload(conf);

    LOG.info(getParams().format());
  }

  @Override
  public ImmutableConfig getConf() {
    return conf;
  }

  @Override
  public void reload(ImmutableConfig conf) {
    this.fetchThreads = conf.getUint(FETCH_THREADS_FETCH, 10);
    this.initBatchSize = conf.getUint(FETCH_FEEDER_INIT_BATCH_SIZE, fetchThreads);
    Duration fetchJobTimeout = conf.getDuration(FETCH_JOB_TIMEOUT, Duration.ofMinutes(30));
    this.jobDeadline = Instant.now().plus(fetchJobTimeout);
  }

  @Override
  public Params getParams() {
    return Params.of(
        "className", getClass().getSimpleName(),
        "fetchThreads", fetchThreads,
        "initBatchSize", initBatchSize,
        "id", id
    );
  }

  public void exitAndJoin() {
    completed.set(true);
    try {
      join();
    } catch (InterruptedException e) {
      LOG.error(e.toString());
    }
  }

  public boolean isCompleted() {
    return completed.get();
  }

  @SuppressWarnings("unchecked")
  @Override
  public void run() {
    fetchMonitor.registerFeedThread(this);

    float batchSize = initBatchSize;
    int round = 0;

    try {
      boolean hasMore = context.nextKey();
      if (hasMore) {
        currentIter = context.getValues().iterator();
      }

      while (!isCompleted() && Instant.now().isBefore(jobDeadline) && hasMore) {
        ++round;

        int taskCount = 0;
        while (taskCount < batchSize && currentIter.hasNext() && hasMore) {
          FetchEntryWritable entry = currentIter.next();
          String url = unreverseUrl(entry.getReservedUrl());
          tasksMonitor.produce(context.getJobId(), url, entry.getWebPage());

          ++totalTaskCount;
          ++taskCount;

          if (!currentIter.hasNext()) {
            hasMore = context.nextKey();
            if (hasMore) {
              currentIter = context.getValues().iterator();
            }
          }
        }

        Params.of(
            "Round", round,
            "batchSize", batchSize,
            "feededTasks", taskCount,
            "totalTaskCount", totalTaskCount,
            "readyTasks", tasksMonitor.readyTaskCount(),
            "fetchThreads", fetchThreads
        ).withLogger(LOG).debug(true);

        try {
          Thread.sleep(checkInterval.toMillis());
        } catch (final Exception ignored) {}

        batchSize = adjustFeedBatchSize(batchSize);
      }

      discardAll();

      tasksMonitor.setFeederCompleted();
    } catch (Throwable e) {
      LOG.error("Feeder error reading input, record " + totalTaskCount, e);
    }
    finally {
      fetchMonitor.unregisterFeedThread(this);
    }

    LOG.info("Feeder finished. Feeded " + round + " rounds, Last feed batch size : "
        + batchSize + ", feed total " + totalTaskCount + " records. ");
  }

  private float adjustFeedBatchSize(float batchSize) {
    // TODO : Why readyTasks is always be very small?
    int readyTasks = tasksMonitor.readyTaskCount();
    double pagesThroughput = taskScheduler.getAveragePageThroughput();
    double recentPages = pagesThroughput * checkInterval.getSeconds();
    // TODO : Every batch size should be greater than pages fetched during last wait interval

    if (batchSize <= 1) {
      batchSize = 1;
    }

    if (readyTasks <= fetchThreads) {
      // No ready tasks, increase batch size
      batchSize += batchSize * 0.2;
    }
    else if (readyTasks <= 2 * fetchThreads) {
      // Too many ready tasks, decrease batch size
      batchSize -= batchSize * 0.2;
    }
    else {
      // Ready task number is OK, do not feed this time
      batchSize = 0;
    }

    return batchSize;
  }

  @SuppressWarnings("unchecked")
  private void discardAll() throws IOException, InterruptedException {
    while (context.nextKey()) {
      currentIter = context.getValues().iterator();
      while (currentIter.hasNext()) {
        currentIter.next();
      }
    }
  }

  @Override
  public int compareTo(@Nonnull FeedThread feedThread) {
    return id - feedThread.id;
  }
}
