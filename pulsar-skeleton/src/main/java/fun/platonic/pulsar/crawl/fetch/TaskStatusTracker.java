package fun.platonic.pulsar.crawl.fetch;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import fun.platonic.pulsar.common.*;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.config.Params;
import fun.platonic.pulsar.common.config.ReloadableParameterized;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.gora.db.WebDb;
import fun.platonic.pulsar.persist.metadata.FetchMode;
import fun.platonic.pulsar.persist.metadata.PageCategory;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static fun.platonic.pulsar.common.config.PulsarConstants.*;
import static fun.platonic.pulsar.common.config.CapabilityTypes.PARSE_MAX_URL_LENGTH;
import static java.util.stream.Collectors.joining;

public class TaskStatusTracker implements ReloadableParameterized, AutoCloseable {
    public static final Logger LOG = LoggerFactory.getLogger(TaskStatusTracker.class);
    public static final int LAZY_FETCH_URLS_PAGE_BASE = 100;
    public static final int TIMEOUT_URLS_PAGE = 1000;
    public static final int FAILED_URLS_PAGE = 1001;
    public static final int DEAD_URLS_PAGE = 1002;
    private static final Logger REPORT_LOG = MetricsSystem.REPORT_LOG;
    /**
     * Tracking statistics for each host
     */
    private final Map<String, FetchStatus> hostStatistics = new TreeMap<>();
    /**
     * Tracking unreachable hosts
     */
    private final Set<String> unreachableHosts = new TreeSet<>();
    /**
     * Tracking hosts who is failed to fetch tasks.
     * A host is considered to be a unreachable host if there are too many failure
     */
    private final Multiset<String> failureHostsTracker = TreeMultiset.create();
    /**
     * The configuration
     */
    private ImmutableConfig conf;
    /**
     * The WebDb
     */
    private WebDb webDb;
    /**
     * The Pulsar file manipulation
     */
    private PulsarFiles ps;
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

    private Set<CharSequence> timeoutUrls = Collections.synchronizedSet(new HashSet<>());
    private Set<CharSequence> failedUrls = Collections.synchronizedSet(new HashSet<>());
    private Set<CharSequence> deadUrls = Collections.synchronizedSet(new HashSet<>());

    private boolean isClosed = false;

    public TaskStatusTracker(WebDb webDb, MetricsSystem metrics, ImmutableConfig conf) {
        this.metrics = metrics;
        ps = new PulsarFiles();

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
        unreachableHosts.addAll(LocalFSUtils.readAllLinesSilent(PATH_FILE_UNREACHABLE_HOSTS));
        maxUrlLength = conf.getInt(PARSE_MAX_URL_LENGTH, 1024);

        getParams().withLogger(LOG).info(true);
    }

    @Override
    public Params getParams() {
        return Params.of(
                "unreachableHosts", unreachableHosts.size(),
                "maxUrlLength", maxUrlLength,
                "unreachableHostsPath", PATH_FILE_UNREACHABLE_HOSTS,
                "timeoutUrls", timeoutUrls.size(),
                "failedUrls", failedUrls.size(),
                "deadUrls", deadUrls
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

    public Set<String> getSeeds(FetchMode mode, int limit) {
        int pageNo = 1;
        Instant now = Instant.now();
        return seedIndexer.getAll(pageNo).stream()
                .map(url -> webDb.get(url.toString()))
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

        webDb.put(page.getUrl(), page);
    }

    public void report() {
        reportAndLogUnreachableHosts();
        reportAndLogAvailableHosts();
    }

    /**
     * @param url       The url
     * @param groupMode The group mode
     * @return True if the host is gone
     */
    public boolean logFailureHost(String url, URLUtil.GroupMode groupMode) {
        String host = URLUtil.getHost(url, groupMode);
        if (host.isEmpty()) {
            LOG.warn("Invald unreachable host format, url : " + url);
            return false;
        }

        if (unreachableHosts.contains(host)) {
            return false;
        }

        failureHostsTracker.add(host);
        // Only the exception occurs for unknownHostEventCount, it's really add to the black list
        final int failureHostEventCount = 3;
        if (failureHostsTracker.count(host) > failureHostEventCount) {
            LOG.info("Host unreachable : " + host);
            unreachableHosts.add(host);
            // retune(true);
            return true;
        }

        return false;
    }

    public void reportAndLogUnreachableHosts() {
        REPORT_LOG.info("There are " + unreachableHosts.size() + " unreachable hosts");
        ps.logUnreachableHosts(this.unreachableHosts);
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

        REPORT_LOG.info(report);
    }

    /**
     * Available hosts statistics
     */
    public void logSuccessHost(WebPage page, URLUtil.GroupMode groupMode) {
        String url = page.getUrl();
        String host = URLUtil.getHost(url, groupMode);

        if (host.isEmpty()) {
            LOG.warn("Bad host in url : " + url);
            return;
        }

        FetchStatus fetchStatus = hostStatistics.get(host);
        if (fetchStatus == null) {
            fetchStatus = new FetchStatus(host);
        }

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
        if (unreachableHosts.contains(host)) {
            unreachableHosts.remove(host);
        }

        if (failureHostsTracker.contains(host)) {
            failureHostsTracker.remove(host);
        }
    }

    @Override
    public void close() {
        if (!isClosed) {
            LOG.info("[Destruction] Destructing TaskStatusTracker ...");
            LOG.info("Archive total {} timeout urls, {} failed urls and {} dead urls",
                    timeoutUrls.size(), failedUrls.size(), deadUrls.size());

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

            isClosed = true;
        }
    }
}
