package ai.platon.pulsar.jobs.fetch.data;

import ai.platon.pulsar.common.DateTimeUtil;
import ai.platon.pulsar.common.URLUtil;
import ai.platon.pulsar.common.config.Parameterized;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.crawl.fetch.FetchTask;
import ai.platon.pulsar.jobs.fetch.FetchMonitor;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class handles FetchItems which come from the same host ID (be it
 * a proto/hostname or proto/IP pair).
 *
 * It also keeps track of requests in progress and elapsed time between requests.
 */
public class TaskPool implements Comparable<TaskPool>, Parameterized {

  private static final Logger LOG = FetchMonitor.LOG;

  private final int RECENT_TASKS_COUNT_LIMIT = 100;

  public enum Status { ACTIVITY, INACTIVITY, RETIRED }

  private PoolId id;

  /** Crawl delay for the queue */
  private final Duration crawlDelay;
  /** Minimal crawl delay for the queue */
  private final Duration minCrawlDelay;
  /** Max thread count for this queue */
  private final int allowedThreads;
  /** Host group mode : can be by ip, by host or by domain */
  private final URLUtil.GroupMode groupMode;
  /** Hold all tasks ready to fetch */
  private final Queue<FetchTask> readyTasks = new LinkedList<>();
  /** Hold all tasks are fetching */
  private final Map<Integer, FetchTask> pendingTasks = new TreeMap<>();
  /** Once timeout, the pending items should be put to the ready queue again */
  private final Duration pendingTimeout;
  /** If a task costs more then this duration, it's a slow task */
  private final Duration slowTaskThreshold = Duration.ofMillis(500);
  /** Record timing cost of slow tasks */
  private final CircularFifoQueue<Duration> slowTasksRecorder = new CircularFifoQueue<>(RECENT_TASKS_COUNT_LIMIT);

  /** Next fetch time */
  private Instant nextFetchTime;
  private int recentFinishedTasks = 1;
  private long recentFetchMillis = 1;
  private int totalFinishedTasks = 1;
  private long totalFetchMillis = 1;
  private int unreachableTasks = 0;

  /**
   * If a fetch queue is inactive, the queue does not accept any tasks, nor serve any requests,
   * but still hold pending tasks, waiting to finish
   * */
  private Status status = Status.ACTIVITY;

  public TaskPool(PoolId id, URLUtil.GroupMode groupMode, int allowedThreads,
                  Duration crawlDelay, Duration minCrawlDelay, Duration pendingTimeout) {
    this.id = id;
    this.groupMode = groupMode;
    this.allowedThreads = allowedThreads;
    this.crawlDelay = crawlDelay;
    this.minCrawlDelay = minCrawlDelay;
    this.pendingTimeout = pendingTimeout;
    this.nextFetchTime = Instant.now();
  }

  @Override
  public Params getParams() {
    final DecimalFormat df = new DecimalFormat("###0.##");

    return Params.of(
        "className", getClass().getSimpleName(),
        "status", status,
        "id", id,
        "allowedThreads", allowedThreads,
        "pendingTasks", pendingTasks.size(),
        "crawlDelay", crawlDelay,
        "minCrawlDelay", minCrawlDelay,
        "now", DateTimeUtil.now(),
        "nextFetchTime", DateTimeUtil.format(nextFetchTime),
        "aveTimeCost(s)", df.format(averageTimeCost()),
        "aveThoRate(s)", df.format(averageThoRate()),
        "readyTasks", readyCount(),
        "pendingTasks", pendingCount(),
        "finsihedTasks", finishedCount(),
        "unreachableTasks", unreachableTasks
    );
  }

  public PoolId getId() { return id; }

  public int getPriority() { return id.getPriority(); }

  public String getProtocol() { return id.getProtocal(); }

  public String getHost() { return id.getHost(); }

  public URLUtil.GroupMode getGroupMode() { return groupMode; }

  /** Produce a task to this queue. Retired queues do not accept any tasks */
  public void produce(FetchTask task) {
    if (task == null || status != Status.ACTIVITY) {
      return;
    }

    if (task.getPriority() != id.getPriority() || !task.getHost().equals(id.getHost())) {
      LOG.error("Queue id mismatches with FetchTask #" + task);
    }

    readyTasks.add(task);
  }

  /** Ask a task from this queue. Retired queues do not assign any tasks */
  public FetchTask consume() {
    if (status != Status.ACTIVITY) {
      return null;
    }

    // TODO : Why we need this restriction?
    if (allowedThreads > 0 && pendingTasks.size() >= allowedThreads) {
      return null;
    }

    Instant now = Instant.now();
    if (now.isBefore(nextFetchTime)) {
      return null;
    }

    FetchTask fetchTask = readyTasks.poll();
    if (fetchTask != null) {
      hangUp(fetchTask, now);
    }

    return fetchTask;
  }

  /**
   * Note : We have set response time for each page, @see {HttpBase#getProtocolOutput}
   * */
  public boolean finish(FetchTask fetchTask, boolean asap) {
    int itemId = fetchTask.getItemId();
    fetchTask = pendingTasks.remove(itemId);
    if (fetchTask == null) {
      LOG.warn("Failed to remove FetchTask : " + itemId);
      return false;
    }

    Instant finishTime = Instant.now();
    setNextFetchTime(finishTime, asap);

    Duration timeCost = Duration.between(fetchTask.getPendingStart(), finishTime);
    if (timeCost.compareTo(slowTaskThreshold) > 0) {
      slowTasksRecorder.add(timeCost);
    }

    ++recentFinishedTasks;
    recentFetchMillis += timeCost.toMillis();
    if (recentFinishedTasks > RECENT_TASKS_COUNT_LIMIT) {
      recentFinishedTasks = 1;
      recentFetchMillis = 1;
    }

    ++totalFinishedTasks;
    totalFetchMillis += timeCost.toMillis();

    return true;
  }

  public boolean finish(int itemId, boolean asap) {
    FetchTask item = pendingTasks.get(itemId);
    return item != null && finish(item, asap);
  }

  public FetchTask getPendingTask(int itemID) { return pendingTasks.get(itemID); }

  /**
   * Hang up the task and wait for completion. Move the fetch task to pending queue.
   * */
  private void hangUp(FetchTask fetchTask, Instant now) {
    if (fetchTask == null) {
      return;
    }

    fetchTask.setPendingStart(now);
    pendingTasks.put(fetchTask.getItemId(), fetchTask);
  }

  public boolean hasTasks() { return hasReadyTasks() || hasPendingTasks(); }

  public boolean hasReadyTasks() { return !readyTasks.isEmpty(); }

  public boolean hasPendingTasks() { return !pendingTasks.isEmpty(); }

  public boolean pendingTaskExists(int itemId) { return pendingTasks.containsKey(itemId); }

  public int readyCount() { return readyTasks.size(); }

  public int pendingCount() { return pendingTasks.size(); }

  public int finishedCount() { return totalFinishedTasks; }

  public int slowTaskCount() { return slowTasksRecorder.size(); }

  public boolean isSlow() { return isSlow(Duration.ofSeconds(1)); }

  public boolean isSlow(Duration threshold) { return averageRecentTimeCost() > threshold.getSeconds(); }

  /**
   * Average cost in seconds
   * */
  public double averageTimeCost() { return totalFetchMillis / 1000.0 / totalFinishedTasks; }
  public double averageRecentTimeCost() { return recentFetchMillis / 1000.0 / recentFinishedTasks; }

  /**
   * Throughput rate in seconds
   * */
  public double averageThoRate() { return totalFinishedTasks / (totalFetchMillis / 1000.0); }
  public double averageRecentThoRate() { return recentFinishedTasks / (recentFetchMillis / 1000.0); }

  public int getUnreachableTasks() { return unreachableTasks; }

  public void enable() { this.status = Status.ACTIVITY; }

  public void disable() { this.status = Status.INACTIVITY; }

  public void retire() { this.status = Status.RETIRED; }

  public boolean isActive() { return this.status == Status.ACTIVITY; }

  public boolean isInactive() { return this.status == Status.INACTIVITY; }

  public boolean isRetired() { return this.status == Status.RETIRED; }

  public Status status() { return this.status; }

  /**
   * Retune the queue to avoid hung tasks, pending tasks are push to ready queue so they can be re-fetched
   *
   * In crowdsourcing mode, it's a common situation to lost
   * the fetching mission and should the task should be restarted
   *
   * @param force If force is true, reload all pending fetch items immediately, otherwise, reload only exceeds pendingTimeout
   * */
  public void retune(boolean force) {
    Instant now = Instant.now();

    final List<FetchTask> readyList = Lists.newArrayList();
    final Map<Integer, FetchTask> pendingList = new HashMap<>();

    pendingTasks.values().forEach(fetchTask -> {
      if (force || fetchTask.getPendingStart().plus(pendingTimeout).isBefore(now)) {
        readyList.add(fetchTask);
      }
      else {
        pendingList.put(fetchTask.getItemId(), fetchTask);
      }
    });

    pendingTasks.clear();
    readyTasks.addAll(readyList);
    pendingTasks.putAll(pendingList);
  }

  public String getCostReport() {
    return String.format("%1$40s -> aveTimeCost : %2$.2fs/p, avaThoRate : %3$.2fp/s",
        id, averageTimeCost(), averageThoRate());
  }

  public int clearReadyQueue() {
    int count = readyTasks.size();
    readyTasks.clear();
    return count;
  }

  public int clearPendingQueue() {
    int count = pendingTasks.size();
    pendingTasks.clear();
    return count;
  }

  public int clearPendingTasksIfFew(int threshold) {
    int count = pendingTasks.size();

    if (count > threshold) {
      return 0;
    }

    if (pendingTasks.isEmpty()) {
      return 0;
    }

    final Instant now = Instant.now();
    String report = pendingTasks.values().stream()
        .limit(threshold)
        .filter(Objects::nonNull)
        .map(f -> f.getUrl() + " : " + Duration.between(f.getPendingStart(), now))
        .collect(Collectors.joining("\n", "Clearing slow pending itmes : ", ""));
    LOG.info(report);

    pendingTasks.clear();

    return count;
  }

  public void dump() {
    LOG.info(getParams().formatAsLine());

    int i = 0;
    final int limit = 20;
    String report = "\nDrop the following tasks : ";
    FetchTask fetchTask = readyTasks.poll();
    while (fetchTask != null && ++i <= limit) {
      report += "  " + i + ". " + fetchTask.getUrl() + "\t";
      fetchTask = readyTasks.poll();
    }
    LOG.info(report);
  }

  private void setNextFetchTime(Instant finishTime, boolean asap) {
    if (!asap) {
      nextFetchTime = finishTime.plus(allowedThreads > 1 ? minCrawlDelay : crawlDelay);
    }
    else {
      nextFetchTime = finishTime;
    }
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof TaskPool)) {
      return false;
    }

    return id.equals(((TaskPool) other).id);
  }

  @Override
  public int hashCode() { return id.hashCode(); }

  @Override
  public int compareTo(TaskPool other) { return id.compareTo(other.id); }

  @Override
  public String toString() { return id.toString(); }
}
