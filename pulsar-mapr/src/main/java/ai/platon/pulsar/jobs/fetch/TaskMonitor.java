package ai.platon.pulsar.jobs.fetch;

import ai.platon.pulsar.common.MetricsSystem;
import ai.platon.pulsar.common.URLUtil;
import ai.platon.pulsar.common.Urls;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.common.config.ReloadableParameterized;
import ai.platon.pulsar.crawl.fetch.FetchTask;
import ai.platon.pulsar.crawl.fetch.TaskStatusTracker;
import ai.platon.pulsar.jobs.fetch.data.PoolId;
import ai.platon.pulsar.jobs.fetch.data.PoolQueue;
import ai.platon.pulsar.jobs.fetch.data.TaskPool;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.metadata.FetchMode;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SortedBidiMap;
import org.apache.commons.collections4.bidimap.DualTreeBidiMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static ai.platon.pulsar.common.config.CapabilityTypes.*;
import static ai.platon.pulsar.common.config.PulsarConstants.FETCH_TASK_REMAINDER_NUMBER;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Tasks Monitor
 * TODO: locks(synchronization) should be refined
 */
public class TaskMonitor implements ReloadableParameterized, AutoCloseable {
  public static final Logger LOG = LoggerFactory.getLogger(TaskMonitor.class);
  private static final Logger REPORT_LOG = MetricsSystem.REPORT_LOG;
  private static AtomicInteger instanceSequence = new AtomicInteger(0);

  private static final int THREAD_SEQUENCE_POS = "FetchThread-".length();

  private ImmutableConfig conf;
  private final int id = instanceSequence.incrementAndGet();
  private final AtomicBoolean feederCompleted = new AtomicBoolean(false);
  private final PoolQueue fetchPools = new PoolQueue();
  private int lastTaskPriority = Integer.MIN_VALUE;

  /**
   * Tracking time cost of each pool
   */
  private final SortedBidiMap<PoolId, Double> poolTimeCosts = new DualTreeBidiMap<>();
  /**
   * Tracking access thread for each each pool
   * */
  private final Multimap<String, String> poolServedThreads = TreeMultimap.create();
  /**
   * The minimal page throughout rate
   * */
  private int minPageThoRate;
  /**
   * Task counters
   * */
  private final AtomicInteger readyTaskCount = new AtomicInteger(0);
  private final AtomicInteger pendingTaskCount = new AtomicInteger(0);
  private final AtomicInteger finishedTaskCount = new AtomicInteger(0);
  /**
   * Delay before crawl
   * */
  private Duration minCrawlDelay;
  private Duration crawlDelay;
  /**
   * The way to group tasks in a pool
   * */
  private URLUtil.GroupMode groupMode;
  /**
   * The maximal number of threads allowed to access a task pool
   * */
  private int poolThreads;
  /**
   * Track the status of each host
   * */
  private TaskStatusTracker statusTracker;

  /**
   * Once timeout, the pending items should be put to the ready pool again.
   */
  private Duration poolPendingTimeout = Duration.ofMinutes(3);

  public TaskMonitor() {
  }

  public TaskMonitor(TaskStatusTracker statusTracker, MetricsSystem metrics, ImmutableConfig conf) {
    this.statusTracker = statusTracker;
    reload(conf);
  }

  @Override
  public ImmutableConfig getConf() {
    return conf;
  }

  @Override
  public Params getParams() {
    return Params.of(
        "className", this.getClass().getSimpleName(),
        "poolThreads", poolThreads,
        "groupMode", groupMode,
        "crawlDelay", crawlDelay,
        "minCrawlDelay", minCrawlDelay,
        "poolPendingTimeout", poolPendingTimeout
    );
  }

  @Override
  public void reload(ImmutableConfig conf) {
    FetchMode fetchMode = conf.getEnum(FETCH_MODE, FetchMode.NATIVE);
    poolThreads = (fetchMode == FetchMode.CROWDSOURCING) ? Integer.MAX_VALUE : conf.getInt(FETCH_THREADS_PER_QUEUE, 1);

    groupMode = conf.getEnum(FETCH_QUEUE_MODE, URLUtil.GroupMode.BY_HOST);
    minPageThoRate = conf.getInt(FETCH_THROUGHPUT_THRESHOLD_PAGES, -1);

    crawlDelay = conf.getDuration(FETCH_QUEUE_DELAY, Duration.ofSeconds(5));
    minCrawlDelay = conf.getDuration(FETCH_QUEUE_MIN_DELAY, Duration.ofSeconds(0));
    poolPendingTimeout = conf.getDuration("fetcher.pending.timeout", Duration.ofMinutes(3));

    LOG.info(getParams().format());
  }

  synchronized public void produce(int jobID, String url, WebPage page) {
    int priority = page.getFetchPriority();
    FetchTask task = FetchTask.create(jobID, priority, url, page, groupMode);

    if (task != null) {
      produce(task);
    } else {
      LOG.warn("Failed to create FetchTask. Url : " + url);
    }
  }

  synchronized public void produce(FetchTask item) {
    doProduce(item);
  }

  /**
   * Find out the FetchQueue with top priority,
   * wait for all pending tasks with higher priority are finished
   * */
  synchronized FetchTask consume() {
    if (fetchPools.isEmpty()) {
      return null;
    }

    final int nextPriority = fetchPools.peek().getPriority();
    final boolean priorityChanged = nextPriority < lastTaskPriority;
    if (priorityChanged && fetchPools.hasPriorPendingTasks(nextPriority)) {
      // Waiting for all pending tasks with higher priority to be finished
      return null;
    }

    if (priorityChanged) {
      LOG.info("Fetch priority changed : " + lastTaskPriority + " -> " + nextPriority);
    }

    TaskPool pool = CollectionUtils.find(fetchPools, this::isConsumable);

    if (pool == null) {
      maintain();
      return null;
    }

    return consumeUnchecked(pool);
  }

  synchronized FetchTask consume(PoolId poolId) {
    TaskPool pool = fetchPools.find(poolId);
    if (pool == null) {
      return null;
    }

    if (isConsumable(pool)) {
      return consumeUnchecked(pool);
    }

    return null;
  }

  synchronized public void finish(int priority, String protocol, String host, int itemId, boolean asap) {
    doFinish(new PoolId(priority, protocol, host), itemId, asap);
  }

  synchronized public void finish(FetchTask item) {
    finish(item.getPriority(), item.getProtocol(), item.getHost(), item.getItemId(), false);
  }

  synchronized public void finishAsap(FetchTask item) {
    finish(item.getPriority(), item.getProtocol(), item.getHost(), item.getItemId(), true);
  }

  public void setFeederCompleted() { feederCompleted.set(true); }

  /**
   * @see FetchMonitor#isFeederAlive
   * @return Return true if the feeder is completed
   * */
  public boolean isFeederCompleted() { return feederCompleted.get(); }

  private boolean isConsumable(TaskPool pool) {
    return pool.isActive() && pool.hasReadyTasks() && statusTracker.isReachable(pool.getHost());
  }

  private void maintain() {
    new ArrayList<>(fetchPools).forEach(this::maintain);
  }

  /** Maintain pool life time, return true if the life time status is changed, false otherwise */
  private TaskPool maintain(TaskPool pool) {
    TaskPool.Status lastStatus = pool.status();
    if (statusTracker.isGone(pool.getHost())) {
      retire(pool);
      LOG.info("Retire pool with unreachable host " + pool.getId());
    } else if (isFeederCompleted() && !pool.hasTasks()) {
      // All tasks are finished, including pending tasks, we can remove the pool from the pools safely
      fetchPools.disable(pool);
    }

    TaskPool.Status status = pool.status();
    if (status != lastStatus) {
      Params.of(
          "FetchQueue", pool.getId(),
          "status", lastStatus + " -> " + pool.status(),
          "ready", pool.readyCount(),
          "pending", pool.pendingCount(),
          "finished", pool.finishedCount()
      ).withLogger(LOG).info(true);
    }

    return pool;
  }

  private FetchTask consumeUnchecked(TaskPool pool) {
    FetchTask item = pool.consume();

    if (item != null) {
      readyTaskCount.decrementAndGet();
      pendingTaskCount.incrementAndGet();
      lastTaskPriority = pool.getPriority();
    }

    return item;
  }

  private void doProduce(FetchTask task) {
    if (statusTracker.isGone(task.getHost())) {
      return;
    }

    String url = task.getUrl();
    if (statusTracker.isGone(URLUtil.getHostName(url)) || statusTracker.isGone(url)) {
      LOG.warn("Ignore unreachable url (indicate task.getHost() failed) " + url);
      return;
    }

    PoolId poolId = new PoolId(task.getPriority(), task.getProtocol(), task.getHost());
    TaskPool pool = fetchPools.find(poolId);

    if (pool == null) {
      pool = fetchPools.findExtend(poolId);
      if (pool != null) {
        fetchPools.enable(pool);
      }
      else {
        pool = createFetchQueue(poolId);
        fetchPools.add(pool);
      }
    }
    pool.produce(task);

    readyTaskCount.incrementAndGet();
    poolTimeCosts.put(pool.getId(), 0.0);
  }

  private void doFinish(PoolId poolId, int itemId, boolean asap) {
    TaskPool pool = fetchPools.findExtend(poolId);

    if (pool == null) {
      LOG.warn("Attemp to finish item from unknown pool " + poolId);
      return;
    }

    if (!pool.pendingTaskExists(itemId)) {
      if (!fetchPools.isEmpty()) {
        LOG.warn("Attemp to finish unknown item: <{}, {}>", poolId, itemId);
      }

      return;
    }

    pool.finish(itemId, asap);

    pendingTaskCount.decrementAndGet();
    finishedTaskCount.incrementAndGet();

    poolTimeCosts.put(poolId, pool.averageRecentTimeCost());
    poolServedThreads.put(poolId.getHost(), Thread.currentThread().getName().substring(THREAD_SEQUENCE_POS));
  }

  private void retire(TaskPool pool) {
    pool.retire();
    fetchPools.remove(pool);
  }

  public synchronized void report() {
    dump(FETCH_TASK_REMAINDER_NUMBER);

    statusTracker.report();

    reportCost();

    reportServedThreads();
  }

  public void logSuccessHost(WebPage page) {
    statusTracker.logSuccessHost(page, groupMode);
  }

  public void logFailureHost(String url) {
    boolean isGone = statusTracker.logFailureHost(url, groupMode);
    if (isGone) {
      retune(true);
    }
  }

  /**
   * Reload pending fetch items so that the items can be re-fetched
   * <p>
   * In crowdsourcing mode, it's a common situation to lost
   * the fetching mission and should restart the task
   *
   * @param force reload all pending fetch items immediately
   * */
  synchronized void retune(boolean force) {
    List<TaskPool> unreachablePools = fetchPools.stream()
        .filter(pool -> statusTracker.isGone(pool.getHost())).collect(toList());

    unreachablePools.forEach(this::retire);
    fetchPools.forEach(pool -> pool.retune(force));

    if (!unreachablePools.isEmpty()) {
      String report = unreachablePools.stream().map(TaskPool::getId).map(Object::toString)
          .collect(joining(", ", "Retired unavailable pools : ", ""));
      LOG.info(report);
    }

    calculateTaskCounter();
  }

  synchronized FetchTask findPendingTask(int priority, URL url, int itemID) {
    TaskPool pool = fetchPools.findExtend(new PoolId(priority, url));
    return pool != null ? pool.getPendingTask(itemID) : null;
  }

  /** Get a pending task, the task can be in working pools or in detached pools */
  synchronized FetchTask findPendingTask(PoolId poolId, int itemID) {
    TaskPool pool = fetchPools.findExtend(poolId);
    return pool != null ? pool.getPendingTask(itemID) : null;
  }

  synchronized void dump(int limit) {
    fetchPools.dump(limit);
    calculateTaskCounter();
  }


  synchronized int tryClearSlowestQueue() {
    TaskPool pool = getSlowestQueue();

    if (pool == null) {
      return 0;
    }

    final DecimalFormat df = new DecimalFormat("0.##");

    if (pool.averageThoRate() >= minPageThoRate) {
      Params.of(
          "EfficientQueue", pool.getId(),
          "ReadyTasks", pool.readyCount(),
          "PendingTasks", pool.pendingCount(),
          "FinishedTasks", pool.finishedCount(),
          "SlowTasks", pool.slowTaskCount(),
          "Throughput, ", df.format(pool.averageTimeCost()) + "s/p" + ", " + df.format(pool.averageThoRate()) + "p/s"
          ).withLogger(LOG).info(true);

      return 0;
    }

    // slowest pools should retires as soon as possible
    retire(pool);

    final int minPendingSlowTasks = 2;
    clearPendingTasksIfFew(pool, minPendingSlowTasks);

    int deleted = clearReadyTasks(pool);

    Params.of(
        "SlowestQueue", pool.getId(),
        "ReadyTasks", pool.readyCount(),
        "PendingTasks", pool.pendingCount(),
        "FinishedTasks", pool.finishedCount(),
        "SlowTasks", pool.slowTaskCount(),
        "Throughput, ", df.format(pool.averageTimeCost()) + "s/p" + ", " + df.format(pool.averageThoRate()) + "p/s",
        "Deleted", deleted).withLogger(LOG).info(true);

    return deleted;
  }

  @Override
  public synchronized void close() throws Exception {
    LOG.info("[Destruction] Closing TasksMonitor #" + id);

    report();

    fetchPools.clear();
    readyTaskCount.set(0);
  }

  synchronized int clearReadyTasks() {
    int count = 0;

    Map<Double, String> costRecorder = new TreeMap<>(Comparator.reverseOrder());
    for (TaskPool pool : fetchPools) {
      costRecorder.put(pool.averageRecentTimeCost(), pool.getId().getHost());

      if (pool.readyCount() == 0) {
        continue;
      }

      count += clearReadyTasks(pool);
    }

    reportCost(costRecorder);

    return count;
  }

  private int clearPendingTasksIfFew(TaskPool pool, int limit) {
    int deleted = pool.clearPendingTasksIfFew(limit);
    pendingTaskCount.addAndGet(-deleted);
    return deleted;
  }

  private int clearReadyTasks(TaskPool pool) {
    int deleted = pool.clearReadyQueue();

    readyTaskCount.addAndGet(-deleted);
    if (readyTaskCount.get() <= 0 && fetchPools.size() == 0) {
      readyTaskCount.set(0);
    }

    return deleted;
  }

  synchronized int getQueueCount() {
    return fetchPools.size();
  }

  int taskCount() { return readyTaskCount.get() + pendingTaskCount(); }

  int readyTaskCount() { return readyTaskCount.get(); }

  int pendingTaskCount() { return pendingTaskCount.get(); }

  int getFinishedTaskCount() {return finishedTaskCount.get(); }

  private void calculateTaskCounter() {
    int[] counter = {0, 0};
    fetchPools.forEach(pool -> {
      counter[0] += pool.readyCount();
      counter[1] += pool.pendingCount();
    });
    readyTaskCount.set(counter[0]);
    pendingTaskCount.set(counter[1]);
  }

  private TaskPool getSlowestQueue() {
    TaskPool pool = null;

    while (!fetchPools.isEmpty() && pool == null) {
      double maxCost = poolTimeCosts.inverseBidiMap().lastKey();
      PoolId id = poolTimeCosts.inverseBidiMap().get(maxCost);
      poolTimeCosts.remove(id);
      pool = fetchPools.find(id);
    }

    return pool;
  }

  private TaskPool createFetchQueue(PoolId poolId) {
    TaskPool pool = new TaskPool(poolId,
        groupMode,
            poolThreads,
        crawlDelay,
        minCrawlDelay,
        poolPendingTimeout);
    LOG.info("FetchQueue created : " + pool);
    return pool;
  }

  private void reportServedThreads() {
    StringBuilder report = new StringBuilder();
    poolServedThreads.keySet().stream()
        .map(Urls::reverseHost).sorted().map(Urls::unreverseHost)
        .forEach(poolId -> {
      String threads = "#" + StringUtils.join(poolServedThreads.get(poolId), ", #");
      String line = String.format("%1$40s -> %2$s\n", poolId, threads);
      report.append(line);
    });

    REPORT_LOG.info("Served threads : \n" + report);
  }

  private void reportCost(Map<Double, String> costRecorder) {
    StringBuilder sb = new StringBuilder();

    sb.append(String.format("\n%s\n", "---------------Queue Cost Report--------------"));
    sb.append(String.format("%25s %s\n", "Ava Time(s)", "Queue Id"));
    final int[] i = {0};
    costRecorder.entrySet().stream().limit(100).forEach(entry -> {
      sb.append(String.format("%1$,4d.%2$,20.2f", ++i[0], entry.getKey()));
      sb.append(" <- ");
      sb.append(entry.getValue());
      sb.append("\n");
    });

    REPORT_LOG.info(sb.toString());
  }

  private void reportCost() {
    String report = "Top slow hosts : \n" + fetchPools.getCostReport();
    report += "\n";
    REPORT_LOG.info(report);
  }
}
