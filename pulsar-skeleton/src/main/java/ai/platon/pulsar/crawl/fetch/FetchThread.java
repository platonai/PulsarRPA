package ai.platon.pulsar.crawl.fetch;

import ai.platon.pulsar.common.MetricsSystem;
import ai.platon.pulsar.common.ReducerContext;
import ai.platon.pulsar.common.StringUtil;
import ai.platon.pulsar.common.Urls;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.crawl.component.FetchComponent;
import ai.platon.pulsar.crawl.fetch.data.PoolId;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.metadata.FetchMode;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static ai.platon.pulsar.common.config.CapabilityTypes.FETCH_MODE;

/**
 * This class picks items from queues and fetches the pages.
 */
public class FetchThread extends Thread implements Comparable<FetchThread> {

    private final Logger LOG = FetchMonitor.LOG;
    private final Logger REPORT_LOG = MetricsSystem.REPORT_LOG;

    private static AtomicInteger instanceSequence = new AtomicInteger(0);

    private ReducerContext context;

    private final int id;

    private final FetchComponent fetchComponent;
    private final FetchMonitor fetchMonitor;
    private final TaskScheduler taskScheduler;
    /**
     * Native, Crowdsourcing, Proxy
     */
    private final FetchMode fetchMode;

    /**
     * Fix the thread to a specified queue as possible as we can
     */
    private int currPriority = -1;
    private PoolId currQueueId = null;
    private AtomicBoolean halted = new AtomicBoolean(false);
    private Set<PoolId> servedHosts = new TreeSet<>();
    private int taskCount = 0;

    public FetchThread(
            FetchComponent fetchComponent,
            FetchMonitor fetchMonitor,
            TaskScheduler taskScheduler,
            ImmutableConfig immutableConfig,
            ReducerContext context) {
        this.context = context;

        this.fetchComponent = fetchComponent;
        this.fetchMonitor = fetchMonitor;
        this.taskScheduler = taskScheduler;

        this.id = instanceSequence.incrementAndGet();

        this.setDaemon(true);
        this.setName(getClass().getSimpleName() + "-" + id);

        this.fetchMode = immutableConfig.getEnum(FETCH_MODE, FetchMode.NATIVE);
    }

    public void halt() {
        halted.set(true);
    }

    public void exitAndJoin() {
        halted.set(true);
        try {
            join();
        } catch (InterruptedException e) {
            LOG.error(e.toString());
        }
    }

    public boolean isHalted() {
        return halted.get();
    }

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

                WebPage page = fetchOne(fetchItem);
                write(page.getKey(), page);

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
        } catch (final Exception ignored) {
        }

        fetchMonitor.unregisterIdleThread(this);
    }

    private FetchItem schedule() {
        FetchJobForwardingResponse fetchResult = null;
        FetchTask fetchTask = null;

        if (fetchMode.equals(FetchMode.CROWDSOURCING)) {
            fetchResult = taskScheduler.pollFetchResult();

            if (fetchResult != null) {
                URL url = Urls.getURLOrNull(fetchResult.getQueueId());
                fetchTask = taskScheduler.getTasksMonitor().findPendingTask(fetchResult.getPriority(), url, fetchResult.getItemId());

                if (fetchTask == null) {
                    LOG.warn("Bad fetch item id {}-{}", fetchResult.getQueueId(), fetchResult.getItemId());
                }
            }
        } else {
            if (currQueueId == null) {
                fetchTask = taskScheduler.schedule();
            } else {
                fetchTask = taskScheduler.schedule(currQueueId);
            }

            if (fetchTask != null) {
                // the next time, we fetch items from the same queue as this time
                currQueueId = new PoolId(fetchTask.getPriority(), fetchTask.getProtocol(), fetchTask.getHost());
                servedHosts.add(currQueueId);
            } else {
                // The current queue is empty, fetch item from top queue the next time
                currQueueId = null;
            }
        }

        return new FetchItem(fetchTask, fetchResult);
    }

    private WebPage fetchOne(FetchItem fetchItem) {
        FetchTask task = fetchItem.getTask();

        WebPage page = fetchComponent.fetchContent(task.getPage());

        PoolId queueId = new PoolId(task.getPriority(), task.getProtocol(), task.getHost());
        taskScheduler.finish(queueId, task.getItemId());

        return page;
    }

    @SuppressWarnings("unchecked")
    private void write(String key, WebPage page) {
        try {
            context.write(key, page.unbox());
        } catch (IOException | InterruptedException e) {
            LOG.error("Failed to write to hdfs" + StringUtil.stringifyException(e));
        } catch (Throwable e) {
            LOG.error(StringUtil.stringifyException(e));
        }
    }

    @Override
    public int compareTo(@Nonnull FetchThread fetchThread) {
        return id - fetchThread.id;
    }
}
