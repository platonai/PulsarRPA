package ai.platon.pulsar.crawl.fetch;

import ai.platon.pulsar.common.*;
import ai.platon.pulsar.common.config.CapabilityTypes;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.common.config.ReloadableParameterized;
import ai.platon.pulsar.persist.WebDb;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.metadata.FetchMode;
import ai.platon.pulsar.persist.metadata.PageCategory;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static ai.platon.pulsar.common.PulsarPaths.PATH_UNREACHABLE_HOSTS;
import static ai.platon.pulsar.common.config.CapabilityTypes.PARSE_MAX_URL_LENGTH;
import static ai.platon.pulsar.common.config.PulsarConstants.SEED_HOME_URL;
import static ai.platon.pulsar.common.config.PulsarConstants.URL_TRACKER_HOME_URL;
import static java.util.stream.Collectors.joining;

public class FetchTaskTracker implements ReloadableParameterized, AutoCloseable {
    public static final Logger LOG = LoggerFactory.getLogger(FetchTaskTracker.class);
    public static final int LAZY_FETCH_URLS_PAGE_BASE = 100;
    public static final int TIMEOUT_URLS_PAGE = 1000;
    public static final int FAILED_URLS_PAGE = 1001;
    public static final int DEAD_URLS_PAGE = 1002;
    /**
     * The configuration
     */
    private ImmutableConfig conf;
    /**
     * The mode to group URLs
     */
    private URLUtil.GroupMode groupMode;
    /**
     * The WebDb
     */
    private WebDb webDb;
    /**
     * The Pulsar simple metrics system
     */
    private MetricsSystem metrics;
    /**
     * Index to seeds
     */
    private WeakPageIndexer seedIndexer;
    /**
     * Index to url status trackers
     */
    private WeakPageIndexer urlTrackerIndexer;
    /**
     * The limitation of url length
     */
    private int maxUrlLength;
    /**
     * Tracking statistics for each host
     */
    private final Map<String, FetchStatus> hostStatistics = Collections.synchronizedMap(new HashMap<>());
    /**
     * Tracking unreachable hosts
     */
    private final Set<String> unreachableHosts = Collections.synchronizedSet(new HashSet<>());
    /**
     * Tracking hosts who is failed to fetch tasks.
     * A host is considered to be a unreachable host if there are too many failure
     */
    private final Multiset<String> failedHostTracker = TreeMultiset.create();
    private Set<CharSequence> timeoutUrls = Collections.synchronizedSet(new HashSet<>());
    private Set<CharSequence> failedUrls = Collections.synchronizedSet(new HashSet<>());
    private Set<CharSequence> deadUrls = Collections.synchronizedSet(new HashSet<>());

    private AtomicBoolean isClosed = new AtomicBoolean();
    private AtomicBoolean isReported = new AtomicBoolean();

    public FetchTaskTracker(WebDb webDb, MetricsSystem metrics, ImmutableConfig conf) {
        this.metrics = metrics;

        this.groupMode = conf.getEnum(CapabilityTypes.PARTITION_MODE_KEY, URLUtil.GroupMode.BY_HOST);
        this.webDb = webDb;
        this.seedIndexer = new WeakPageIndexer(SEED_HOME_URL, webDb);
        this.urlTrackerIndexer = new WeakPageIndexer(URL_TRACKER_HOME_URL, webDb);

        timeoutUrls.addAll(urlTrackerIndexer.takeAll(TIMEOUT_URLS_PAGE));
        failedUrls.addAll(urlTrackerIndexer.getAll(FAILED_URLS_PAGE));
        deadUrls.addAll(urlTrackerIndexer.getAll(DEAD_URLS_PAGE));

        reload(conf);
    }

    @Override
    public ImmutableConfig getConf() {
        return conf;
    }

    @Override
    public void reload(ImmutableConfig conf) {
        unreachableHosts.clear();
        unreachableHosts.addAll(LocalFSUtils.readAllLinesSilent(PATH_UNREACHABLE_HOSTS));
        maxUrlLength = conf.getInt(PARSE_MAX_URL_LENGTH, 1024);

        getParams().withLogger(LOG).info(true);
    }

    @Override
    public Params getParams() {
        return Params.of(
                "unreachableHosts", unreachableHosts.size(),
                "maxUrlLength", maxUrlLength,
                "unreachableHostsPath", PATH_UNREACHABLE_HOSTS,
                "timeoutUrls", timeoutUrls.size(),
                "failedUrls", failedUrls.size(),
                "deadUrls", deadUrls.size()
        );
    }

    public boolean isReachable(String host) {
        return !unreachableHosts.contains(host);
    }

    public boolean isGone(String host) {
        return unreachableHosts.contains(host);
    }

    public boolean isFailed(String url) {
        return failedUrls.contains(url);
    }

    public void trackFailed(String url) {
        failedUrls.add(url);
    }

    public void trackFailed(Collection<String> urls) {
        failedUrls.addAll(urls);
    }

    public boolean isTimeout(String url) {
        return timeoutUrls.contains(url);
    }

    public void trackTimeout(String url) {
        timeoutUrls.add(url);
    }

    /**
     * @param url The url
     * @return True if the host is gone
     */
    public boolean trackHostGone(String url) {
        String host = URLUtil.getHost(url, groupMode);
        if (host.isEmpty()) {
            LOG.warn("Malformed url | <{}>", url);
            return false;
        }

        if (unreachableHosts.contains(host)) {
            return false;
        }

        failedHostTracker.add(host);
        // Only the exception occurs for unknownHostEventCount, it's really add to the black list
        final int failureHostEventCount = 3;
        if (failedHostTracker.count(host) > failureHostEventCount) {
            LOG.info("Host unreachable : " + host);
            unreachableHosts.add(host);
            // retune(true);
            return true;
        }

        return false;
    }

    /**
     * Available hosts statistics
     */
    public void trackSuccess(WebPage page) {
        String url = page.getUrl();
        String host = URLUtil.getHost(url, groupMode);

        if (host.isEmpty()) {
            LOG.warn("Bad host in url : " + url);
            return;
        }

        FetchStatus fetchStatus = hostStatistics.computeIfAbsent(host, FetchStatus::new);

        ++fetchStatus.urls;

        // PageCategory pageCategory = CrawlFilter.sniffPageCategory(page);
        PageCategory pageCategory = page.getPageCategory();
        if (pageCategory.isIndex()) {
            ++fetchStatus.indexUrls;
        } else if (pageCategory.isDetail()) {
            ++fetchStatus.detailUrls;
        } else if (pageCategory.isMedia()) {
            ++fetchStatus.mediaUrls;
        } else if (pageCategory.isSearch()) {
            ++fetchStatus.searchUrls;
        } else if (pageCategory.isBBS()) {
            ++fetchStatus.bbsUrls;
        } else if (pageCategory.isTieBa()) {
            ++fetchStatus.tiebaUrls;
        } else if (pageCategory.isBlog()) {
            ++fetchStatus.blogUrls;
        } else if (pageCategory.isUnknown()) {
            ++fetchStatus.unknownUrls;
        }

        int depth = page.getDistance();
        if (depth == 1) {
            ++fetchStatus.urlsFromSeed;
        }

        if (url.length() > maxUrlLength) {
            ++fetchStatus.urlsTooLong;
            metrics.debugLongUrls(url);
        }

        hostStatistics.put(host, fetchStatus);

        // The host is reachable
        unreachableHosts.remove(host);

        failedHostTracker.remove(host);
    }

    public int countHostTasks(String host) {
        int failedTasks = failedHostTracker.count(host);

        FetchStatus status = hostStatistics.get(host);
        if (status == null) return failedTasks;

        return status.urls + failedTasks;
    }

    public Set<String> getSeeds(FetchMode mode, int limit) {
        int pageNo = 1;
        Instant now = Instant.now();
        return seedIndexer.getAll(pageNo).stream()
                .map(url -> webDb.get(url.toString()))
                .filter(Objects::nonNull)
                .filter(page -> page.getFetchMode() == mode)
                .filter(page -> page.getFetchTime().isBefore(now))
                .limit(limit)
                .map(WebPage::getUrl)
                .collect(Collectors.toSet());
    }

    public void commitLazyTasks(FetchMode mode, Collection<String> urls) {
        urls.stream().map(WebPage::newWebPage).forEach(page -> lazyFetch(page, mode));

        // Urls with different fetch mode are indexed in different pages and the page number is just the enum ordinal
        int pageNo = LAZY_FETCH_URLS_PAGE_BASE + mode.ordinal();
        urlTrackerIndexer.indexAll(pageNo, CollectionUtils.collect(urls, url -> (CharSequence) url));
        webDb.flush();
    }

    public Set<CharSequence> getLazyTasks(FetchMode mode) {
        int pageNo = LAZY_FETCH_URLS_PAGE_BASE + mode.ordinal();
        return urlTrackerIndexer.getAll(pageNo);
    }

    public Set<CharSequence> takeLazyTasks(FetchMode mode, int n) {
        int pageNo = LAZY_FETCH_URLS_PAGE_BASE + mode.ordinal();
        return urlTrackerIndexer.takeN(pageNo, n);
    }

    public void commitTimeoutTasks(Collection<CharSequence> urls, FetchMode mode) {
        urlTrackerIndexer.indexAll(TIMEOUT_URLS_PAGE, urls);
    }

    public Set<CharSequence> takeTimeoutTasks(FetchMode mode) {
        return urlTrackerIndexer.takeAll(TIMEOUT_URLS_PAGE);
    }

    private void lazyFetch(WebPage page, FetchMode mode) {
        page.setFetchMode(mode);
        webDb.put(page);
    }

    public void report() {
        if (isReported.getAndSet(true)) {
            return;
        }

        reportAndLogUnreachableHosts();
        reportAndLogAvailableHosts();
    }

    public void reportAndLogUnreachableHosts() {
        LOG.info("There are " + unreachableHosts.size() + " unreachable hosts");
        PulsarFiles.INSTANCE.logUnreachableHosts(this.unreachableHosts);
    }

    public void reportAndLogAvailableHosts() {
        String report = "# Total " + hostStatistics.size() + " available hosts";
        report += "\n";

        String hostsReport = hostStatistics.values().stream().sorted()
                .map(hostInfo -> String.format("%40s -> %-15s %-15s %-15s %-15s %-15s %-15s %-15s %-15s %-15s",
                        hostInfo.hostName,
                        "total : " + hostInfo.urls,
                        "index : " + hostInfo.indexUrls,
                        "detail : " + hostInfo.detailUrls,
                        "search : " + hostInfo.searchUrls,
                        "media : " + hostInfo.mediaUrls,
                        "bbs : " + hostInfo.bbsUrls,
                        "tieba : " + hostInfo.tiebaUrls,
                        "blog : " + hostInfo.blogUrls,
                        "long : " + hostInfo.urlsTooLong))
                .collect(joining("\n"));

        report += hostsReport;
        report += "\n";

        LOG.info(report);
    }

    @Override
    public void close() {
        if (isClosed.getAndSet(true)) {
            return;
        }

        LOG.debug("[Destruction] Destructing TaskStatusTracker ...");
        LOG.info("Archive total {} timeout urls, {} failed urls and {} dead urls",
                timeoutUrls.size(), failedUrls.size(), deadUrls.size());

        report();

        // Trace failed tasks for further fetch as a background tasks
        if (!timeoutUrls.isEmpty()) {
            urlTrackerIndexer.indexAll(TIMEOUT_URLS_PAGE, timeoutUrls);
        }

        if (!failedUrls.isEmpty()) {
            urlTrackerIndexer.indexAll(FAILED_URLS_PAGE, failedUrls);
        }

        if (!deadUrls.isEmpty()) {
            urlTrackerIndexer.indexAll(DEAD_URLS_PAGE, deadUrls);
        }

        urlTrackerIndexer.commit();
    }
}
