package ai.platon.pulsar.jobs.fetch;

import ai.platon.pulsar.common.*;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.common.config.ReloadableParameterized;
import ai.platon.pulsar.jobs.fetch.indexer.IndexThread;
import ai.platon.pulsar.jobs.fetch.indexer.JITIndexer;
import ai.platon.pulsar.persist.metadata.FetchMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ai.platon.pulsar.common.config.CapabilityTypes.*;
import static ai.platon.pulsar.common.config.PulsarConstants.*;

/**
 * Created by vincent on 16-9-24.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
public class FetchMonitor implements ReloadableParameterized, AutoCloseable {

  public static final Logger LOG = LoggerFactory.getLogger(FetchMonitor.class);
  private static AtomicInteger instanceSequence = new AtomicInteger(0);

  private final int id = instanceSequence.incrementAndGet();
  private ImmutableConfig immutableConfig;

  /**
   * Monitors
   */
  private TaskSchedulers taskSchedulers;
  private TaskMonitor taskMonitor;
  private TaskScheduler taskScheduler;

  private Instant startTime = Instant.now();

  private String jobName;
  private String crawlId;
  private String batchId;
  private FetchMode fetchMode = FetchMode.NATIVE;

  /**
   * Threads
   */
  private int fetchThreadCount = 5;

  private final Set<FetchThread> activeFetchThreads = new ConcurrentSkipListSet<>();
  private final Set<FetchThread> retiredFetchThreads = new ConcurrentSkipListSet<>();
  private final Set<FetchThread> idleFetchThreads = new ConcurrentSkipListSet<>();

  private final AtomicInteger activeFetchThreadCount = new AtomicInteger(0);
  private final AtomicInteger idleFetchThreadCount = new AtomicInteger(0);

  /**
   * Feeder threads
   */
  private final Set<FeedThread> feedThreads = new ConcurrentSkipListSet<>();
  // private final ScheduledExecutorService feederExecutor;

  /**
   * Index server
   */
  private String indexServer;
  private int indexServerPort;

  /**
   * Timing
   */
  private Duration fetchJobTimeout;
  private Duration fetchTaskTimeout;
  private Duration poolRetuneInterval;
  private Duration poolPendingTimeout;
  private Instant poolRetuneTime;

  /**
   * Throughput control
   */
  private int minPageThoRate;
  private Instant thoCheckTime;
  private Duration thoCheckInterval;
  private int maxLowThoCount;
  private int maxTotalLowThoCount;
  private int lowThoCount = 0;
  private int totalLowThoCount = 0;

  /**
   * Monitoring
   */
  private Duration checkInterval;

  /**
   * Scripts
   */
  private Path finishScript;
  private Path commandFile;

  private boolean halt = false;

  public FetchMonitor(TaskMonitor tasksMonitor, TaskSchedulers taskSchedulers, ImmutableConfig immutableConfig) {
    this.taskSchedulers = taskSchedulers;
    this.taskScheduler = taskSchedulers.getFirst();
    this.taskMonitor = tasksMonitor;

    reload(immutableConfig);
  }

  @Override
  public void reload(ImmutableConfig immutableConfig) {
    this.immutableConfig = immutableConfig;

    crawlId = immutableConfig.get(STORAGE_CRAWL_ID);
    batchId = immutableConfig.get(BATCH_ID);

    fetchMode = immutableConfig.getEnum(FETCH_MODE, FetchMode.NATIVE);
    fetchThreadCount = immutableConfig.getInt(FETCH_THREADS_FETCH, 5);

    jobName = immutableConfig.get(PARAM_JOB_NAME, DateTimeUtil.format(Instant.now(), "MMddHHmmss"));

    fetchJobTimeout = immutableConfig.getDuration(FETCH_JOB_TIMEOUT, Duration.ofHours(1));
    fetchTaskTimeout = immutableConfig.getDuration(FETCH_TASK_TIMEOUT, Duration.ofMinutes(10));
    poolRetuneInterval = immutableConfig.getDuration(FETCH_QUEUE_RETUNE_INTERVAL, Duration.ofMinutes(8));
    poolPendingTimeout = immutableConfig.getDuration(FETCH_PENDING_TIMEOUT, poolRetuneInterval.multipliedBy(2));
    poolRetuneTime = startTime;

    /*
     * Used for threshold check, holds pages and bytes processed in the last sec
     * We should keep a minimal fetch speed
     * */
    minPageThoRate = immutableConfig.getInt(FETCH_THROUGHPUT_THRESHOLD_PAGES, -1);
    maxLowThoCount = immutableConfig.getInt(FETCH_THROUGHPUT_THRESHOLD_SEQENCE, 10);
    maxTotalLowThoCount = maxLowThoCount * 10;
    thoCheckInterval = immutableConfig.getDuration(FETCH_THROUGHPUT_CHECK_INTERVAL, Duration.ofSeconds(120));
    thoCheckTime = startTime.plus(thoCheckInterval);

    // indexing
    indexServer = immutableConfig.get(INDEXER_HOSTNAME, DEFAULT_INDEX_SERVER_HOSTNAME);
    indexServerPort = immutableConfig.getInt(INDEXER_PORT, DEFAULT_INDEX_SERVER_PORT);

    // check
    checkInterval = immutableConfig.getDuration(FETCH_CHECK_INTERVAL, Duration.ofSeconds(20));

    // scripts
    commandFile = PulsarPaths.PATH_LOCAL_COMMAND;
    finishScript = PulsarPaths.INSTANCE.get("scripts", "finish_" + jobName + ".sh");

    generateFinishCommand();

    LOG.info(getParams().format());
  }

  @Override
  public ImmutableConfig getConf() {
    return immutableConfig;
  }

  @Override
  public Params getParams() {
    return Params.of(
        "className", this.getClass().getSimpleName(),
        "crawlId", crawlId,
        "batchId", batchId,
        "fetchMode", fetchMode,

        "taskSchedulers", taskSchedulers.name(),
        "taskScheduler", taskScheduler.name(),

        "fetchJobTimeout", fetchJobTimeout,
        "fetchTaskTimeout", fetchTaskTimeout,
        "poolPendingTimeout", poolPendingTimeout,
        "poolRetuneInterval", poolRetuneInterval,
        "poolRetuneTime", DateTimeUtil.format(poolRetuneTime),
        "checkInterval", checkInterval,

        "minPageThoRate", minPageThoRate,
        "maxLowThoCount", maxLowThoCount,
        "thoCheckTime", DateTimeUtil.format(thoCheckTime),

        "finishScript", finishScript
    );
  }

  private void generateFinishCommand() {
    String cmd = "#bin\necho finish " + jobName + " >> " + commandFile;

    try {
      Files.createDirectories(finishScript.getParent());
      Files.write(finishScript, cmd.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
      Files.setPosixFilePermissions(finishScript, PosixFilePermissions.fromString("rwxrw-r--"));
    } catch (IOException e) {
      LOG.error(e.toString());
    }
  }

  public TaskMonitor getTaskMonitor() {
    return taskMonitor;
  }

  public void registerFeedThread(FeedThread feedThread) { feedThreads.add(feedThread); }

  public void unregisterFeedThread(FeedThread feedThread) { feedThreads.remove(feedThread); }

  public boolean isFeederAlive() { return !feedThreads.isEmpty(); }

  public boolean isMissionComplete() {
    return !isFeederAlive() && getTaskMonitor().readyTaskCount() == 0 && getTaskMonitor().pendingTaskCount() == 0;
  }

  public void registerFetchThread(FetchThread fetchThread) {
    activeFetchThreads.add(fetchThread);
    activeFetchThreadCount.incrementAndGet();
  }

  public void unregisterFetchThread(FetchThread fetchThread) {
    activeFetchThreads.remove(fetchThread);
    activeFetchThreadCount.decrementAndGet();

    retiredFetchThreads.add(fetchThread);
  }

  public void registerIdleThread(FetchThread thread) {
    idleFetchThreads.add(thread);
    idleFetchThreadCount.incrementAndGet();
  }

  public void unregisterIdleThread(FetchThread thread) {
    idleFetchThreads.remove(thread);
    idleFetchThreadCount.decrementAndGet();
  }

  public void start(ReducerContext context) throws IOException, InterruptedException {
    startFeedThread(context);

    if (FetchMode.CROWDSOURCING.equals(fetchMode)) {
      startCrowdsourcingThreads(context);
    } else {
      // Threads for native or proxy mode
      startNativeFetcherThreads(context);
    }

    if (taskScheduler.getIndexJIT()) {
      startIndexThreads();
    }

    startCheckAndReportLoop(context);
  }

  @Override
  public void close() {
    try {
      LOG.info("[Destruction] Closing FetchMonitor #" + id);

      feedThreads.forEach(FeedThread::exitAndJoin);
      activeFetchThreads.forEach(FetchThread::exitAndJoin);
      retiredFetchThreads.forEach(FetchThread::report);

      Files.deleteIfExists(finishScript);
    } catch (Throwable e) {
      LOG.error(StringUtil.stringifyException(e));
    }
  }

  public void halt() {
    halt = true;
  }

  public boolean isHalt() {
    return halt;
  }

  /**
   * Start pool feeder thread. The thread fetches webpages from the reduce result
   * and add it into the fetch pool
   * Non-Blocking
   * */
  private void startFeedThread(ReducerContext context) throws IOException, InterruptedException {
    FeedThread feedThread = new FeedThread(this, taskScheduler, taskScheduler.getTasksMonitor(), context, immutableConfig);
    feedThread.start();
  }

  /**
   * Start crowd sourcing threads
   * Non-Blocking
   * */
  private void startCrowdsourcingThreads(ReducerContext context) {
    startFetchThreads(fetchThreadCount, context);
  }

  /**
   * Start native fetcher threads
   * Non-Blocking
   * */
  private void startNativeFetcherThreads(ReducerContext context) {
    startFetchThreads(fetchThreadCount, context);
  }

  private void startFetchThreads(int threadCount, ReducerContext context) {
    for (int i = 0; i < threadCount; i++) {
      FetchThread fetchThread = new FetchThread(this, taskScheduler, immutableConfig, context);
      fetchThread.start();
    }
  }

  /**
   * Start index threads
   * Non-Blocking
   * */
  private void startIndexThreads() throws IOException {
    JITIndexer JITIndexer = taskScheduler.getJitIndexer();
    if (JITIndexer == null) {
      LOG.error("Unexpected null JITIndexer");
      return;
    }

    for (int i = 0; i < JITIndexer.getIndexThreadCount(); i++) {
      IndexThread indexThread = new IndexThread(JITIndexer, immutableConfig);
      indexThread.start();
    }
  }

  private void startCheckAndReportLoop(ReducerContext context) throws IOException {
    if (checkInterval.getSeconds() < 5) {
      LOG.warn("Check frequency is too high, it might cause a serious performance problem");
    }

    do {
      TaskScheduler.Status status = taskScheduler.waitAndReport(checkInterval);

      String statusString = taskScheduler.getStatusString(status);

      /* Status string shows in yarn admin ui */
      context.setStatus(statusString);

      /* And also log it */
      LOG.info(statusString);

      Instant now = Instant.now();
      Duration jobTime = Duration.between(startTime, now);
      Duration idleTime = Duration.between(taskScheduler.getLastTaskFinishTime(), now);

      /*
       * Check if any fetch tasks are hung
       * */
      retuneFetchQueues(now, idleTime);

      /*
       * Dump the remainder fetch items if feeder thread is no available and fetch item is few
       * */
      if (!isFeederAlive() && taskMonitor.taskCount() <= FETCH_TASK_REMAINDER_NUMBER) {
        LOG.info("Totally remains only " + taskMonitor.taskCount() + " tasks");
        handleFewFetchItems();
      }

      /*
       * Check throughput(fetch speed)
       * */
      if (now.isAfter(thoCheckTime) && status.pagesThoRate < minPageThoRate) {
        checkFetchThroughput();
        thoCheckTime = thoCheckTime.plus(thoCheckInterval);
      }

      /*
       * Halt command is received
       * */
      if (isHalt()) {
        LOG.info("Received halt command, exit the job ...");
        break;
      }

      /*
       * Read local filesystem for control commands
       * */
      if (RuntimeUtils.hasLocalFileCommand(commandFile.toString(), "finish " + jobName)) {
        handleFinishJobCommand();
        LOG.info("Find finish-job command in " + commandFile + ", exit the job ...");
        halt();
        break;
      }

      /*
       * All fetch tasks are finished
       * */
      if (isMissionComplete()) {
        LOG.info("All done, exit the job ...");
        break;
      }

      if (jobTime.getSeconds() > fetchJobTimeout.getSeconds()) {
        handleJobTimeout();
        LOG.info("Hit fetch job timeout " + jobTime.getSeconds() + "s, exit the job ...");
        break;
      }

      /*
       * No new tasks for too long, some requests seem to hang. We exits the job.
       * */
      if (idleTime.getSeconds() > fetchTaskTimeout.getSeconds()) {
        handleFetchTaskTimeout();
        LOG.info("Hit fetch task timeout " + idleTime.getSeconds() + "s, exit the job ...");
        break;
      }

      if (taskScheduler.getIndexJIT() && !NetUtil.testHttpNetwork(indexServer, indexServerPort)) {
        LOG.warn("Lost index server, exit the job");
        break;
      }
    } while (!activeFetchThreads.isEmpty());
  }

  /**
   * Handle job timeout
   * */
  private int handleJobTimeout() { return taskMonitor.clearReadyTasks(); }

  /**
   * Check if some threads are hung. If so, we should stop the main fetch loop
   * should we stop the main fetch loop
   * */
  private void handleFetchTaskTimeout() {
    if (activeFetchThreads.isEmpty()) {
      return;
    }

    int threads = activeFetchThreads.size();

    LOG.warn("Aborting with " + threads + " hung threads");

    dumpFetchThreads();
  }

  /**
   * Dump fetch threads
   */
  public void dumpFetchThreads() {
    LOG.info("Fetch threads : active : " + activeFetchThreads.size() + ", idle : " + idleFetchThreads.size());

    String report = activeFetchThreads.stream()
        .filter(Thread::isAlive)
        .map(Thread::getStackTrace)
        .flatMap(Stream::of)
        .map(StackTraceElement::toString)
        .collect(Collectors.joining("\n"));
    LOG.info(report);
  }

  private void handleFewFetchItems() {
    taskMonitor.dump(FETCH_TASK_REMAINDER_NUMBER);
  }

  private void handleFinishJobCommand() { taskMonitor.clearReadyTasks(); }

  /**
   * Check pools to see if something is hung
   * */
  private void retuneFetchQueues(Instant now, Duration idleTime) {
    if (taskMonitor.readyTaskCount() + taskMonitor.pendingTaskCount() < 20) {
      poolPendingTimeout = Duration.ofMinutes(2);
    }

    // Do not check in every report loop
    Instant nextCheckTime = poolRetuneTime.plus(checkInterval.multipliedBy(2));
    if (now.isAfter(nextCheckTime)) {
      Instant nextRetuneTime = poolRetuneTime.plus(poolRetuneInterval);
      if (now.isAfter(nextRetuneTime) || idleTime.compareTo(poolPendingTimeout) > 0) {
        taskMonitor.retune(false);
        poolRetuneTime = now;
      }
    }
  }

  /**
   * Check if we're dropping below the threshold (we are too slow)
   * */
  private void checkFetchThroughput() throws IOException {
    int removedSlowTasks;
    if (lowThoCount > maxLowThoCount) {
      // Clear slowest pools
      removedSlowTasks = taskMonitor.tryClearSlowestQueue();

      LOG.info(Params.formatAsLine(
          "Unaccepted throughput", "clearing slowest pool, ",
          "lowThoCount", lowThoCount,
          "maxLowThoCount", maxLowThoCount,
          "minPageThoRate(p/s)", minPageThoRate,
          "removedSlowTasks", removedSlowTasks
      ));

      lowThoCount = 0;
    }

    // Quit if we dropped below threshold too many times
    if (totalLowThoCount > maxTotalLowThoCount) {
      // Clear all pools
      removedSlowTasks = taskMonitor.clearReadyTasks();
      LOG.info(Params.formatAsLine(
          "Unaccepted throughput", "all pools are cleared",
          "lowThoCount", lowThoCount,
          "maxLowThoCount", maxLowThoCount,
          "minPageThoRate(p/s)", minPageThoRate,
          "removedSlowTasks", removedSlowTasks
      ));

      totalLowThoCount = 0;
    }
  }
}
