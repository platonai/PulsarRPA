package ai.platon.pulsar.jobs.fetch;

import ai.platon.pulsar.common.*;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.common.config.ReloadableParameterized;
import ai.platon.pulsar.crawl.component.FetchComponent;
import ai.platon.pulsar.crawl.fetch.FetchJobForwardingResponse;
import ai.platon.pulsar.crawl.fetch.FetchTask;
import ai.platon.pulsar.crawl.filter.UrlFilterException;
import ai.platon.pulsar.crawl.filter.UrlNormalizers;
import ai.platon.pulsar.crawl.parse.PageParser;
import ai.platon.pulsar.crawl.parse.ParseResult;
import ai.platon.pulsar.crawl.protocol.Content;
import ai.platon.pulsar.crawl.protocol.ProtocolOutput;
import ai.platon.pulsar.jobs.fetch.data.PoolId;
import ai.platon.pulsar.jobs.fetch.indexer.JITIndexer;
import ai.platon.pulsar.persist.*;
import ai.platon.pulsar.persist.metadata.Mark;
import ai.platon.pulsar.persist.metadata.Name;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AtomicDouble;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static ai.platon.pulsar.common.CommonCounter.rLinks;
import static ai.platon.pulsar.common.CommonCounter.rPersist;
import static ai.platon.pulsar.common.config.CapabilityTypes.*;
import static ai.platon.pulsar.common.config.PulsarConstants.*;
import static ai.platon.pulsar.persist.metadata.ParseStatusCodes.SUCCESS_OK;
import static java.util.stream.Collectors.joining;

public class TaskScheduler implements ReloadableParameterized, AutoCloseable {

    private final Logger LOG = FetchMonitor.LOG;
    public static final Logger REPORT_LOG = MetricsSystem.REPORT_LOG;
    public static final String PROTOCOL_REDIR = "protocol";

    public class Status {
        Status(float pagesThoRate, float bytesThoRate, int readyFetchItems, int pendingFetchItems) {
            this.pagesThoRate = pagesThoRate;
            this.bytesThoRate = bytesThoRate;
            this.readyFetchItems = readyFetchItems;
            this.pendingFetchItems = pendingFetchItems;
        }

        public float pagesThoRate;
        public float bytesThoRate;
        public int readyFetchItems;
        public int pendingFetchItems;
    }

    public enum Counter {
        rMbytes, unknowHosts, rowsInjected,
        rReadyTasks, rPendingTasks, rFinishedTasks,
        rPagesTho, rMbTho, rRedirect,
        rSeeds,
        rParseFailed, rNoParse,
        rIndexed, rNotIndexed,
        rDepthUp
    }

    static {
        MetricsCounters.register(Counter.class);
    }

    private static final AtomicInteger objectSequence = new AtomicInteger(0);

    private ImmutableConfig conf;
    private MetricsSystem metricsSystem;

    private final int id;
    private MetricsCounters metricsCounters = new MetricsCounters();
    private TaskMonitor tasksMonitor;

    private final Queue<FetchJobForwardingResponse> fetchResultQueue = new ConcurrentLinkedQueue<>();

    /**
     * Our own Hardware bandwidth in mbytes, if exceed the limit, slows down the task scheduling.
     */
    private int bandwidth;

    // Parser setting
    private boolean storingContent;
    private boolean parse;
    private PageParser pageParser;
    private boolean skipTruncated;

    /**
     * Fetch threads
     */
    private int initFetchThreadCount;
    private int threadsPerQueue;

    /**
     * Indexer
     */
    private boolean indexJIT;
    private JITIndexer jitIndexer;

    // Timer
    private final long startTime = System.currentTimeMillis(); // Start time of fetcher run
    private final AtomicLong lastTaskStartTime = new AtomicLong(startTime);
    private final AtomicLong lastTaskFinishTime = new AtomicLong(startTime);

    // Statistics
    private final AtomicLong totalBytes = new AtomicLong(0);        // total fetched bytes
    private final AtomicInteger totalPages = new AtomicInteger(0);  // total fetched pages
    private final AtomicInteger fetchErrors = new AtomicInteger(0); // total fetch fetchErrors

    private final AtomicDouble averagePageThroughput = new AtomicDouble(0.01);
    private final AtomicDouble averageBytesThroughput = new AtomicDouble(0.01);
    private final AtomicDouble averagePageSize = new AtomicDouble(0.0);

    /**
     * Output
     */
    private Path outputDir;

    /**
     * The reprUrl is the representative url of a redirect, we save a reprUrl for each thread
     * We use a concurrent skip list map to gain the best concurrency
     * <p>
     * TODO : check why we store a reprUrl for each thread?
     */
    private Map<Long, String> reprUrls = new ConcurrentSkipListMap<>();

    public TaskScheduler(PageParser pageParser,
                         TaskMonitor tasksMonitor,
                         MetricsSystem metricsSystem,
                         ImmutableConfig conf) throws IOException {
        this.id = objectSequence.incrementAndGet();
        this.pageParser = pageParser;
        this.tasksMonitor = tasksMonitor;
        this.metricsSystem = metricsSystem;

        reload(conf);
    }

    public TaskScheduler(PageParser pageParser,
                         JITIndexer jitIndexer,
                         TaskMonitor tasksMonitor,
                         MetricsSystem metricsSystem,
                         ImmutableConfig conf) throws IOException {
        this.id = objectSequence.incrementAndGet();
        this.pageParser = pageParser;
        this.jitIndexer = jitIndexer;
        this.tasksMonitor = tasksMonitor;
        this.metricsSystem = metricsSystem;

        reload(conf);
    }

    @Override
    public void reload(ImmutableConfig conf) {
        this.conf = conf;
        this.initFetchThreadCount = conf.getInt(FETCH_THREADS_FETCH, 10);
        this.threadsPerQueue = conf.getInt(FETCH_THREADS_PER_QUEUE, 1);
        this.indexJIT = conf.getBoolean(INDEX_JIT, false);
        if (!indexJIT) jitIndexer = null;
        this.parse = indexJIT || conf.getBoolean(PARSE_PARSE, true);

        this.bandwidth = 1024 * 1024 * conf.getInt("fetcher.net.bandwidth.m", BANDWIDTH_INFINITE);
        this.skipTruncated = conf.getBoolean(PARSE_SKIP_TRUNCATED, true);
        this.storingContent = conf.getBoolean(FETCH_STORE_CONTENT, false);

        this.outputDir = PulsarPaths.INSTANCE.getReportDir();

        LOG.info(getParams().format());
    }

    @Override
    public ImmutableConfig getConf() {
        return conf;
    }

    @Override
    public Params getParams() {
        return Params.of(
                "className", this.getClass().getSimpleName(),

                "id", id,

                "bandwidth", bandwidth,
                "initFetchThreadCount", initFetchThreadCount,
                "threadsPerQueue", threadsPerQueue,

                "skipTruncated", skipTruncated,
                "parse", parse,
                "storingContent", storingContent,

                "indexJIT", indexJIT,
                "outputDir", outputDir
        );
    }

    public int getId() {
        return id;
    }

    public String name() {
        return getClass().getSimpleName() + "-" + id;
    }

    public int getBandwidth() {
        return this.bandwidth;
    }

    public double getAveragePageThroughput() {
        return averagePageThroughput.get();
    }

    public double getAverageBytesThroughput() {
        return averageBytesThroughput.get();
    }

    public TaskMonitor getTasksMonitor() {
        return tasksMonitor;
    }

    public boolean getIndexJIT() {
        return indexJIT;
    }

    public JITIndexer getJitIndexer() {
        return jitIndexer;
    }

    public Instant getLastTaskFinishTime() {
        return Instant.ofEpochMilli(lastTaskFinishTime.get());
    }

    public Set<CharSequence> getUnparsableTypes() {
        return pageParser == null ? Collections.emptySet() : pageParser.getUnparsableTypes();
    }

    public void produce(FetchJobForwardingResponse result) {
        fetchResultQueue.add(result);
    }

    /**
     * Schedule a queue with top priority
     */
    public FetchTask schedule() {
        return schedule(null);
    }

    /**
     * Schedule a queue with the given priority and given queueId
     */
    public FetchTask schedule(PoolId queueId) {
        List<FetchTask> fetchTasks = schedule(queueId, 1);
        return fetchTasks.isEmpty() ? null : fetchTasks.iterator().next();
    }

    /**
     * Schedule the queues with top priority
     */
    public List<FetchTask> schedule(int number) {
        return schedule(null, number);
    }

    /**
     * Null queue id means the queue with top priority
     * Consume a fetch item and try to download the target web page
     */
    public List<FetchTask> schedule(PoolId queueId, int number) {
        List<FetchTask> fetchTasks = Lists.newArrayList();
        if (number <= 0) {
            LOG.warn("Required no fetch item");
            return fetchTasks;
        }

        if (tasksMonitor.pendingTaskCount() * averagePageSize.get() * 8 > 30 * this.getBandwidth()) {
            LOG.info("Bandwidth exhausted, slows down the scheduling");
            return fetchTasks;
        }

        while (number-- > 0) {
            FetchTask fetchTask = queueId == null ? tasksMonitor.consume() : tasksMonitor.consume(queueId);
            if (fetchTask != null) {
                fetchTasks.add(fetchTask);
            }
        }

        if (!fetchTasks.isEmpty()) {
            lastTaskStartTime.set(System.currentTimeMillis());
        }

        return fetchTasks;
    }

    /**
     * Finish the fetch item anyway, even if it's failed to download the target page
     */
    public void finishUnchecked(FetchTask fetchTask) {
        tasksMonitor.finish(fetchTask);
        lastTaskFinishTime.set(System.currentTimeMillis());
        metricsCounters.increase(Counter.rFinishedTasks);
    }

    /**
     * Finished downloading the web page
     * <p>
     * Multiple threaded, non-synchronized class member variables are not allowed inside this method.
     */
    public void finish(PoolId queueId, int itemId, ProtocolOutput output, ReducerContext context) {
        Objects.requireNonNull(context);

        FetchTask fetchTask = tasksMonitor.findPendingTask(queueId, itemId);

        if (fetchTask == null) {
            // Can not find task to finish, The queue might be retuned or cleared up
            LOG.info("Can not find task to finish <{}, {}>", queueId, itemId);

            return;
        }

        try {
            doFinishFetchTask(fetchTask, output, context);
        } catch (final Throwable t) {
            LOG.error("Unexpected error for " + fetchTask.getUrl() + StringUtil.stringifyException(t));

            tasksMonitor.finish(fetchTask);
            fetchErrors.incrementAndGet();
            metricsCounters.increase(CommonCounter.errors);

            try {
                handleResult(fetchTask, null, ProtocolStatus.STATUS_FAILED, CrawlStatus.STATUS_RETRY, context);
            } catch (IOException | InterruptedException e) {
                LOG.error("Unexpected fetcher exception, " + StringUtil.stringifyException(e));
            } finally {
                tasksMonitor.finish(fetchTask);
            }
        } finally {
            lastTaskFinishTime.set(System.currentTimeMillis());
        }
    }

    /**
     * Multiple threaded
     */
    public FetchJobForwardingResponse pollFetchResult() {
        return fetchResultQueue.remove();
    }

    @Override
    public void close() throws Exception {
        LOG.info("[Destruction] Closing TaskScheduler #" + id);

        String border = StringUtils.repeat('.', 40);
        REPORT_LOG.info(border);
        REPORT_LOG.info("[Final Report - " + DateTimeUtil.now() + "]");

        report();

        REPORT_LOG.info("[End Report]");
        REPORT_LOG.info(border);
    }

    /**
     * Wait for a while and report task status
     *
     * @param reportInterval Report interval
     * @return Status
     */
    public Status waitAndReport(Duration reportInterval) throws IOException {
        float pagesLastSec = totalPages.get();
        long bytesLastSec = totalBytes.get();

        try {
            Thread.sleep(reportInterval.toMillis());
        } catch (InterruptedException ignored) {
        }

        float reportIntervalSec = reportInterval.getSeconds();
        float pagesThoRate = (totalPages.get() - pagesLastSec) / reportIntervalSec;
        float bytesThoRate = (totalBytes.get() - bytesLastSec) / reportIntervalSec;

        int readyFetchItems = tasksMonitor.readyTaskCount();
        int pendingFetchItems = tasksMonitor.pendingTaskCount();

        metricsCounters.setValue(Counter.rReadyTasks, readyFetchItems);
        metricsCounters.setValue(Counter.rPendingTasks, pendingFetchItems);
        metricsCounters.setValue(Counter.rPagesTho, Math.round(pagesThoRate));
        metricsCounters.setValue(Counter.rMbTho, Math.round(bytesThoRate / 1000));

        if (jitIndexer != null) {
            metricsCounters.setValue(Counter.rIndexed, jitIndexer.getIndexedPages());
            metricsCounters.setValue(Counter.rNotIndexed, jitIndexer.getIngoredPages());
        }

        return new Status(pagesThoRate, bytesThoRate, readyFetchItems, pendingFetchItems);
    }

    public String getStatusString(Status status) {
        return getStatusString(status.pagesThoRate, status.bytesThoRate, status.readyFetchItems, status.pendingFetchItems);
    }

    private String getStatusString(float pagesThroughput,
                                   float bytesThroughput, int readyFetchItems, int pendingFetchItems) {
        final DecimalFormat df = new DecimalFormat("0.0");

        this.averagePageSize.set(bytesThroughput / pagesThroughput);

        StringBuilder status = new StringBuilder();
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;

        // status.append(idleFetchThreadCount).append("/").append(activeFetchThreadCount).append(" idle/active threads, ");
        status.append(totalPages).append(" pages, ").append(fetchErrors).append(" errors, ");

        // average speed
        averagePageThroughput.set(1.0 * totalPages.get() / elapsed);
        status.append(df.format(averagePageThroughput.get())).append(" ");
        // instantaneous speed
        status.append(df.format(pagesThroughput)).append(" pages/s, ");

        // average speed
        averageBytesThroughput.set(1.0 * totalBytes.get() / elapsed);
        status.append(df.format(averageBytesThroughput.get() * 8.0 / 1024)).append(" ");
        // instantaneous speed
        status.append(df.format(bytesThroughput * 8.0 / 1024)).append(" kb/s, ");

        status.append(readyFetchItems).append(" ready ");
        status.append(pendingFetchItems).append(" pending ");
        status.append("URLs in ").append(tasksMonitor.getQueueCount()).append(" queues");

        return status.toString();
    }

    /**
     * Thread safe
     */
    private void doFinishFetchTask(FetchTask fetchTask, ProtocolOutput output, ReducerContext context)
            throws IOException, InterruptedException, UrlFilterException {
        // un-block queue
        tasksMonitor.finish(fetchTask);

        String url = fetchTask.getUrl();
        Content content = output.getContent();
        if (content == null) {
            LOG.warn("No content for " + url);
        }

        ProtocolHeaders headers = fetchTask.getPage().getHeaders();
        output.getHeaders().asMultimap().entries().forEach(e -> headers.put(e.getKey(), e.getValue()));

        ProtocolStatus protocolStatus = output.getStatus();
        int minorCode = protocolStatus.getMinorCode();

        if (protocolStatus.isSuccess()) {
            if (ProtocolStatus.NOTMODIFIED == minorCode) {
                handleResult(fetchTask, null, protocolStatus, CrawlStatus.STATUS_NOTMODIFIED, context);
            } else if (ProtocolStatus.SUCCESS_OK == minorCode) {
                handleResult(fetchTask, content, protocolStatus, CrawlStatus.STATUS_FETCHED, context);
                tasksMonitor.logSuccessHost(fetchTask.getPage());
            }

            return;
        }

        switch (minorCode) {
            case ProtocolStatus.WOULDBLOCK:
                // retry ?
                tasksMonitor.produce(fetchTask);
                break;

            case ProtocolStatus.MOVED:         // redirect
            case ProtocolStatus.TEMP_MOVED:
                boolean temp = (minorCode == ProtocolStatus.TEMP_MOVED);

                final String newUrl = protocolStatus.getArgOrDefault("location", "");
                if (!newUrl.isEmpty()) {
                    handleRedirect(url, newUrl, temp, PROTOCOL_REDIR, fetchTask.getPage());
                }

                CrawlStatus crawlStatus = temp ? CrawlStatus.STATUS_REDIR_TEMP : CrawlStatus.STATUS_REDIR_PERM;
                handleResult(fetchTask, content, protocolStatus, crawlStatus, context);
                break;

            case ProtocolStatus.THREAD_TIMEOUT:
            case ProtocolStatus.WEB_DRIVER_TIMEOUT:
            case ProtocolStatus.REQUEST_TIMEOUT:
            case ProtocolStatus.UNKNOWN_HOST:
                handleResult(fetchTask, null, protocolStatus, CrawlStatus.STATUS_GONE, context);
                tasksMonitor.logFailureHost(url);
                break;
            case ProtocolStatus.EXCEPTION:
                logFetchFailure(protocolStatus.toString());

      /* FALL THROUGH **/
            case ProtocolStatus.RETRY:          // retry
            case ProtocolStatus.BLOCKED:
                handleResult(fetchTask, null, protocolStatus, CrawlStatus.STATUS_RETRY, context);
                break;

            case ProtocolStatus.GONE:           // gone
            case ProtocolStatus.NOTFOUND:
            case ProtocolStatus.ACCESS_DENIED:
            case ProtocolStatus.ROBOTS_DENIED:
                handleResult(fetchTask, null, protocolStatus, CrawlStatus.STATUS_GONE, context);
                break;
            default:
                LOG.warn("Unknown ProtocolStatus : " + protocolStatus.toString());
                handleResult(fetchTask, null, protocolStatus, CrawlStatus.STATUS_RETRY, context);
        }
    }

    private void handleRedirect(String url, String newUrl, boolean temp, String redirType, WebPage page)
            throws UrlFilterException, IOException, InterruptedException {
        newUrl = pageParser.getCrawlFilters().normalizeToEmpty(newUrl, UrlNormalizers.SCOPE_FETCHER);
        if (newUrl.isEmpty() || newUrl.equals(url)) {
            return;
        }

        page.addLiveLink(new HypeLink(newUrl));
        page.getMetadata().set(Name.REDIRECT_DISCOVERED, YES_STRING);

        long threadId = Thread.currentThread().getId();
        String reprUrl = reprUrls.getOrDefault(threadId, url);
        reprUrl = URLUtil.chooseRepr(reprUrl, newUrl, temp);

        if (reprUrl.length() < SHORTEST_VALID_URL_LENGTH) {
            LOG.warn("reprUrl is too short");
            return;
        }

        page.setReprUrl(reprUrl);
        reprUrls.put(threadId, reprUrl);
        metricsSystem.reportRedirects(String.format("[%s] - %100s -> %s\n", redirType, url, reprUrl));
        metricsCounters.increase(Counter.rRedirect);
    }

    /**
     * Do not redirect too much
     * TODO : Check why we need to save reprUrl for each thread
     */
    private String handleRedirectUrl(WebPage page, String url, String newUrl, boolean temp) {
        long threadId = Thread.currentThread().getId();
        String reprUrl = reprUrls.getOrDefault(threadId, url);
        reprUrl = URLUtil.chooseRepr(reprUrl, newUrl, temp);

        if (reprUrl.length() < SHORTEST_VALID_URL_LENGTH) {
            LOG.warn("reprUrl is too short");
            return reprUrl;
        }

        page.setReprUrl(reprUrl);
        reprUrls.put(threadId, reprUrl);

        return reprUrl;
    }

    @SuppressWarnings("unchecked")
    private void handleResult(
            FetchTask fetchTask, @Nullable Content content, ProtocolStatus protocolStatus, CrawlStatus crawlStatus, ReducerContext context)
            throws IOException, InterruptedException {
        Objects.requireNonNull(fetchTask);
        Objects.requireNonNull(protocolStatus);
        Objects.requireNonNull(crawlStatus);
        Objects.requireNonNull(context);

        String url = fetchTask.getUrl();
        WebPage page = fetchTask.getPage();

        FetchComponent.updateContent(page, content);
        FetchComponent.updateStatus(page, crawlStatus, protocolStatus);
        FetchComponent.updateFetchTime(page);
        FetchComponent.updateMarks(page);

        metricsSystem.debugFetchHistory(page, crawlStatus);

        String reversedUrl = Urls.reverseUrl(url);

        if (parse && crawlStatus.isFetched()) {
            ParseResult parseResult = pageParser.parse(page);

            if (!parseResult.isSuccess()) {
                metricsCounters.increase(Counter.rParseFailed);
                page.getPageCounters().increase(PageCounters.Self.parseErr);
            }

            // Double check success
            if (!page.hasMark(Mark.PARSE)) {
                metricsCounters.increase(Counter.rNoParse);
            }

            if (parseResult.getMinorCode() != SUCCESS_OK) {
                metricsSystem.reportFlawyParsedPage(page, true);
            }

            if (parseResult.isSuccess() && jitIndexer != null) {
                // JIT Index
                jitIndexer.produce(fetchTask);
            }
        }

        // Remove content if storingContent is false. Content is added to page earlier
        // so PageParser is able to parse it, now, we can clear it
        if (page.getContent() != null) {
            page.setContentBytes(page.getContent().array().length);

            if (!storingContent) {
                if (!page.isSeed()) {
                    page.setContent(new byte[0]);
                } else if (page.getFetchCount() > 2) {
                    page.setContent(new byte[0]);
                }
            }
        }

        output(reversedUrl, page, context);

        updateStatus(page);
    }

    @SuppressWarnings("unchecked")
    private void output(String reversedUrl, WebPage page, ReducerContext context) {
        Objects.requireNonNull(context);

        try {
            context.write(reversedUrl, page.unbox());
        } catch (IOException | InterruptedException e) {
            LOG.error("Failed to write to hdfs" + StringUtil.stringifyException(e));
        } catch (Throwable e) {
            LOG.error(StringUtil.stringifyException(e));
        }
    }

    private void updateStatus(WebPage page) throws IOException {
        metricsCounters.increase(rPersist);
        metricsCounters.increase(rLinks, page.getImpreciseLinkCount());

        totalPages.incrementAndGet();
        totalBytes.addAndGet(page.getContentBytes());

        if (page.isSeed()) {
            metricsCounters.increase(Counter.rSeeds);
        }

        CounterUtils.increaseRDepth(page.getDistance(), metricsCounters);

        metricsCounters.increase(Counter.rMbytes, Math.round(page.getContentBytes() / 1024.0f));
    }

    private void logFetchFailure(String message) {
        if (!message.isEmpty()) {
            LOG.warn("Fetch failed, " + message);
        }

        fetchErrors.incrementAndGet();
        metricsCounters.increase(CommonCounter.errors);
    }

    private void report() {
        if (!getUnparsableTypes().isEmpty()) {
            String report = "";
            String hosts = getUnparsableTypes().stream().sorted().collect(joining("\n"));
            report += hosts;
            report += "\n";
            REPORT_LOG.info("# UnparsableTypes : \n" + report);
        }
    }
}
