package ai.platon.pulsar.jobs.fetch;

import ai.platon.pulsar.common.*;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.crawl.fetch.FetchItem;
import ai.platon.pulsar.crawl.fetch.FetchJobForwardingResponse;
import ai.platon.pulsar.crawl.fetch.FetchTask;
import ai.platon.pulsar.crawl.protocol.*;
import ai.platon.pulsar.jobs.fetch.data.PoolId;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.metadata.FetchMode;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static ai.platon.pulsar.common.config.CapabilityTypes.FETCH_MODE;
import static ai.platon.pulsar.common.config.CapabilityTypes.FETCH_QUEUE_MODE;

/**
 * This class picks items from queues and fetches the pages.
 * */
public class FetchThread extends Thread implements Comparable<FetchThread> {

  private enum FetchStatus { Success, Failed }

  private final Logger LOG = FetchMonitor.LOG;
  private final Logger REPORT_LOG = MetricsSystem.REPORT_LOG;

  private static AtomicInteger instanceSequence = new AtomicInteger(0);

  private ReducerContext context;
  private ImmutableConfig conf;

  private final int id;

  private final ProtocolFactory protocolFactory;
  private final FetchMonitor fetchMonitor;
  private final TaskScheduler taskScheduler;
  /**
   * Native, Crowdsourcing, Proxy
   * */
  private final FetchMode fetchMode;
  private boolean debugContent = false;
  private String reprUrl;
  private URLUtil.GroupMode groupMode;

  /** Fix the thread to a specified queue as possible as we can */
  private int currPriority = -1;
  private PoolId currQueueId = null;
  private AtomicBoolean halted = new AtomicBoolean(false);
  private Set<PoolId> servedHosts = new TreeSet<>();
  private int taskCount = 0;

  public FetchThread(FetchMonitor fetchMonitor, TaskScheduler taskScheduler, ImmutableConfig conf, ReducerContext context) {
    this.context = context;
    this.conf = conf;

    this.fetchMonitor = fetchMonitor;
    this.taskScheduler = taskScheduler;

    this.id = instanceSequence.incrementAndGet();

    this.setDaemon(true);
    this.setName(getClass().getSimpleName() + "-" + id);

    this.protocolFactory = ProtocolFactory.create(conf);
    this.fetchMode = conf.getEnum(FETCH_MODE, FetchMode.NATIVE);
    this.groupMode = conf.getEnum(FETCH_QUEUE_MODE, URLUtil.GroupMode.BY_HOST);
    this.debugContent = conf.getBoolean("fetcher.fetch.thread.debug.content", false);
  }

  public String reprUrl() { return reprUrl; }

  public void halt() { halted.set(true); }

  public void exitAndJoin() {
    halted.set(true);
    try {
      join();
    } catch (InterruptedException e) {
      LOG.error(e.toString());
    }
  }

  public boolean isHalted() { return halted.get(); }

  @Override
  public void run() {
    Objects.requireNonNull(context);

    fetchMonitor.registerFetchThread(this);

    FetchItem fetchItem = null;

    try {
      while (!fetchMonitor.isMissionComplete() && !isHalted()) {
        fetchItem = schedule();

        if (fetchItem.getTask() == null) {
          sleepAndRecord();
          continue;
        }

        fetchOne(fetchItem, context);

        ++taskCount;
      } // while
    } catch (final Throwable e) {
      LOG.error("Unexpected throwable : " + StringUtil.stringifyException(e));
    } finally {
      if (fetchItem != null && fetchItem.getTask() != null) {
        taskScheduler.finishUnchecked(fetchItem.getTask());
      }

      fetchMonitor.unregisterFetchThread(this);

      LOG.info("Thread #{} finished", getId());
    }
  }

  public void report() {
    if (servedHosts.isEmpty()) {
      return;
    }

    String report = String.format("Thread #%d served %d tasks for %d hosts : \n", getId(), taskCount, servedHosts.size());
    report += "\n";

    String availableHosts = servedHosts.stream()
        .map(PoolId::toUrl)
        .map(Urls::reverseHost).sorted().map(Urls::unreverseHost)
        .map(host -> String.format("%1$40s", host))
        .collect(Collectors.joining("\n"));

    report += availableHosts;
    report += "\n";

    REPORT_LOG.info(report);
  }

  private void sleepAndRecord() {
    fetchMonitor.registerIdleThread(this);

    try {
      Thread.sleep(1000);
    } catch (final Exception ignored) {}

    fetchMonitor.unregisterIdleThread(this);
  }

  private FetchItem schedule() {
    FetchJobForwardingResponse fetchResult = null;
    FetchTask fetchTask = null;

    if (fetchMode.equals(FetchMode.CROWDSOURCING)) {
      fetchResult = taskScheduler.pollFetchResut();

      if (fetchResult != null) {
        URL url = Urls.getURLOrNull(fetchResult.getQueueId());
        fetchTask = taskScheduler.getTasksMonitor().findPendingTask(fetchResult.getPriority(), url, fetchResult.getItemId());

        if (fetchTask == null) {
          LOG.warn("Bad fetch item id {}-{}", fetchResult.getQueueId(), fetchResult.getItemId());
        }
      }
    }
    else {
      if (currQueueId == null) {
        fetchTask = taskScheduler.schedule();
      }
      else {
        fetchTask = taskScheduler.schedule(currQueueId);
      }

      if (fetchTask != null) {
        // the next time, we fetch items from the same queue as this time
        currQueueId = new PoolId(fetchTask.getPriority(), fetchTask.getProtocol(), fetchTask.getHost());
        servedHosts.add(currQueueId);
      }
      else {
        // The current queue is empty, fetch item from top queue the next time
        currQueueId = null;
      }
    }

    return new FetchItem(fetchTask, fetchResult);
  }

  /**
   * Fetch one web page
   * */
  private FetchStatus fetchOne(FetchItem fetchItem, ReducerContext context) throws ProtocolNotFound, IOException {
    Protocol protocol = getProtocol(fetchItem);
    if (fetchItem.getResponse() != null) {
      protocol.setResponse(fetchItem.getResponse());
    }

    FetchTask task = fetchItem.getTask();
    ForwardingResponse result = fetchItem.getResponse();

    if (task == null) {
      return FetchStatus.Failed;
    }

    if (LOG.isTraceEnabled()) {
      LOG.trace("Fetching <{}, {}>", task.getPriority(), task.getUrl());
    }

    // Blocking until the target web page is loaded
    final ProtocolOutput output = protocol.getProtocolOutput(task.getPage());
    PoolId queueId = new PoolId(task.getPriority(), task.getProtocol(), task.getHost());
    taskScheduler.finish(queueId, task.getItemId(), output, context);

    if (debugContent) {
      cacheContent(result.getUrl(), result.getContent());
    }

    return FetchStatus.Success;
  }

  /**
   * Get network protocol, for example : http, ftp, sftp, and crowd protocol, etc
   * */
  private Protocol getProtocol(FetchItem fetchItem) throws ProtocolNotFound {
    FetchTask task = fetchItem.getTask();

    Protocol protocol;
    if (fetchMode.equals(FetchMode.CROWDSOURCING)) {
      protocol = protocolFactory.getProtocol(fetchMode);
    } else {
      WebPage page = task.getPage();

      // TODO: Check the logic here, why calculate repr url here?
      if (page.getReprUrl().isEmpty()) {
        reprUrl = task.getUrl();
      } else {
        reprUrl = page.getReprUrl();
      }

      // Block, open the network to fetch the web page and wait for a response
      protocol = protocolFactory.getProtocol(task.getUrl());
    }

    return protocol;
  }

  private void cacheContent(String url, byte[] content) {
    try {
      String date = new SimpleDateFormat("mm").format(new Date());

      Path path = PulsarPaths.INSTANCE.get("cache", date, PulsarPaths.INSTANCE.fromUri(url, ".htm"));
      if (date.equals("00")) {
        // make a clean up every hour
        Files.delete(path.getParent());
      }

      Files.createDirectories(path.getParent());
      Files.write(path, content);
    }
    catch (IOException e) {
      LOG.error(e.toString());
    }
  }

  @Override
  public int compareTo(@Nonnull FetchThread fetchThread) {
    return id - fetchThread.id;
  }
} // FetcherThread
