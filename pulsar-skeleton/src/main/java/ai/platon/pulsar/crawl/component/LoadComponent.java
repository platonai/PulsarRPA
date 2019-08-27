package ai.platon.pulsar.crawl.component;

import ai.platon.pulsar.common.DateTimeUtil;
import ai.platon.pulsar.common.StringUtil;
import ai.platon.pulsar.common.Urls;
import ai.platon.pulsar.common.config.PulsarConstants;
import ai.platon.pulsar.common.options.LinkOptions;
import ai.platon.pulsar.common.options.LoadOptions;
import ai.platon.pulsar.common.options.NormUrl;
import ai.platon.pulsar.crawl.fetch.FetchTaskTracker;
import ai.platon.pulsar.crawl.parse.ParseResult;
import ai.platon.pulsar.persist.*;
import ai.platon.pulsar.persist.gora.generated.GHypeLink;
import org.apache.avro.util.Utf8;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.hadoop.classification.InterfaceStability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ai.platon.pulsar.common.config.PulsarConstants.*;
import static ai.platon.pulsar.persist.metadata.Name.PROXY;
import static ai.platon.pulsar.persist.metadata.Name.RESPONSE_TIME;
import static org.apache.commons.collections4.CollectionUtils.addIgnoreNull;

/**
 * Created by vincent on 17-7-15.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 * <p>
 * Load pages from storage or fetch from the Internet if it's not fetched or expired
 */
@Component
public class LoadComponent {
    public static final Logger LOG = LoggerFactory.getLogger(LoadComponent.class);
    public static final int FETCH_REASON_DO_NOT_FETCH = 0;
    public static final int FETCH_REASON_NEW_PAGE = 1;
    public static final int FETCH_REASON_EXPIRED = 2;
    public static final int FETCH_REASON_SMALL_CONTENT = 3;
    public static final int FETCH_REASON_MISS_FIELD = 4;
    public static final int FETCH_REASON_TEMP_MOVED = 300;
    public static final int FETCH_REASON_RETRY_ON_FAILURE = 301;
    public static final HashMap<Integer, String> fetchReasonCodes = new HashMap<>();

    private final static Set<String> globalFetchingUrls = Collections.synchronizedSet(new HashSet<>());

    static {
        fetchReasonCodes.put(FETCH_REASON_DO_NOT_FETCH, "do_not_fetch");
        fetchReasonCodes.put(FETCH_REASON_NEW_PAGE, "new_page");
        fetchReasonCodes.put(FETCH_REASON_EXPIRED, "expired");
        fetchReasonCodes.put(FETCH_REASON_SMALL_CONTENT, "small");
        fetchReasonCodes.put(FETCH_REASON_MISS_FIELD, "miss_field");
        fetchReasonCodes.put(FETCH_REASON_TEMP_MOVED, "temp_moved");
        fetchReasonCodes.put(FETCH_REASON_RETRY_ON_FAILURE, "retry_on_failure");
    }

    private final WebDb webDb;
    private final BatchFetchComponent fetchComponent;
    private final ParseComponent parseComponent;
    private final UpdateComponent updateComponent;
    private final FetchTaskTracker fetchTaskTracker;

    public LoadComponent(
            WebDb webDb,
            BatchFetchComponent fetchComponent,
            ParseComponent parseComponent,
            UpdateComponent updateComponent
    ) {
        this.webDb = webDb;
        this.fetchComponent = fetchComponent;
        this.parseComponent = parseComponent;
        this.updateComponent = updateComponent;
        this.fetchTaskTracker = fetchComponent.getFetchTaskTracker();
    }

    public BatchFetchComponent getFetchComponent() {
        return fetchComponent;
    }

    public ParseComponent getParseComponent() {
        return parseComponent;
    }

    public static String getFetchReason(int code) {
        return fetchReasonCodes.getOrDefault(code, "unknown");
    }

    /**
     * Load an url, options can be specified following the url, see {@link LoadOptions} for all options
     *
     * @param configuredUrl The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, {@link WebPage#NIL} is returned
     */
    @Nonnull
    public WebPage load(String configuredUrl) {
        kotlin.Pair<String, String> urlAndOptions = Urls.splitUrlArgs(configuredUrl);
        LoadOptions options = LoadOptions.Companion.parse(urlAndOptions.getSecond());
        return load(urlAndOptions.getFirst(), options);
    }

    /**
     * Load an url with specified options, see {@link LoadOptions} for all options
     *
     * @param originalUrl The url to load
     * @param options The options
     * @return The WebPage. If there is no web page at local storage nor remote location, {@link WebPage#NIL} is returned
     */
    @Nonnull
    public WebPage load(String originalUrl, String options) {
        return load(originalUrl, LoadOptions.Companion.parse(options));
    }

    /**
     * Load an url with specified options
     * If there is no page in local storage nor at the given remote location, {@link WebPage#NIL} is returned
     *
     * @param originalUrl The url to load
     * @param options The options
     * @return The WebPage.
     */
    @Nonnull
    public WebPage load(String originalUrl, LoadOptions options) {
        Objects.requireNonNull(options);
        return loadOne(new NormUrl(originalUrl, options));
    }

    @Nonnull
    public WebPage load(URL url, LoadOptions options) {
        Objects.requireNonNull(options);
        return loadOne(new NormUrl(url, options));
    }

    /**
     * Load an url in {@link GHypeLink} format
     * If there is no page in local storage nor at the given remote location, {@link WebPage#NIL} is returned
     *
     * @param link    The url in {@link GHypeLink} format to load
     * @param options The options
     * @return The WebPage.
     */
    @Nonnull
    public WebPage load(GHypeLink link, LoadOptions options) {
        Objects.requireNonNull(link);
        Objects.requireNonNull(options);

        WebPage page = load(link.getUrl().toString(), options);
        page.setAnchor(link.getAnchor().toString());

        return page;
    }

    @Nonnull
    public WebPage load(NormUrl normUrl) {
        return loadOne(normUrl);
    }

    /**
     * Load a batch of urls with the specified options.
     * <p>
     * If the option indicates prefer parallel, urls are fetched in a parallel manner whenever applicable.
     * If the batch is too large, only a random part of the urls is fetched immediately, all the rest urls are put into
     * a pending fetch list and will be fetched in background later.
     * <p>
     * If a page does not exists neither in local storage nor at the given remote location, {@link WebPage#NIL} is
     * returned
     *
     * @param normUrls    The urls to load
     * @param options The options
     * @return Pages for all urls.
     */
    public Collection<WebPage> loadAll(Iterable<NormUrl> normUrls, LoadOptions options) {
        Instant startTime = Instant.now();
        Set<NormUrl> filteredUrls = new HashSet<>();
        CollectionUtils.collect(normUrls, url -> addIgnoreNull(filteredUrls, filterUrlToNull(url)));

        if (filteredUrls.isEmpty()) {
            return new ArrayList<>();
        }

        Set<WebPage> knownPages = new HashSet<>();
        Set<String> pendingUrls = new HashSet<>();

        for (NormUrl normUrl : filteredUrls) {
            String url = normUrl.getUrl();
            LoadOptions opt = normUrl.getOptions();

            WebPage page = webDb.getOrNil(url, opt.getShortenKey());

            int reason = getFetchReason(page, opt);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Fetch reason: {} | {} {}", getFetchReason(reason), url, opt);
            }

            ProtocolStatus status = page.getProtocolStatus();
            if (reason == FETCH_REASON_NEW_PAGE) {
                pendingUrls.add(url);
            } else if (reason == FETCH_REASON_EXPIRED) {
                pendingUrls.add(url);
            } else if (reason == FETCH_REASON_SMALL_CONTENT) {
                pendingUrls.add(url);
            } else if (reason == FETCH_REASON_MISS_FIELD) {
                pendingUrls.add(url);
            } else if (reason == FETCH_REASON_TEMP_MOVED) {
                // TODO: batch redirect
                page = redirect(page, opt);
                if (status.isSuccess()) {
                    knownPages.add(page);
                }
            } else if (reason == FETCH_REASON_DO_NOT_FETCH) {
                if (status.isSuccess()) {
                    knownPages.add(page);
                } else {
                    LOG.warn("Don't fetch page with unknown reason {} | {} {}", status, url, opt);
                }
            } else {
                LOG.error("Unknown fetch reason {} | {} {}", reason, url, opt);
            }
        }

        if (pendingUrls.isEmpty()) {
            return knownPages;
        }

        LOG.debug("Fetching {} urls with options {}", pendingUrls.size(), options);

        Collection<WebPage> updatedPages;
        try {
            globalFetchingUrls.addAll(pendingUrls);
            if (options.getPreferParallel()) {
                updatedPages = fetchComponent.parallelFetchAll(pendingUrls, options);
            } else {
                updatedPages = fetchComponent.fetchAll(pendingUrls, options);
            }
        } finally {
            globalFetchingUrls.removeAll(pendingUrls);
        }

        updatedPages.forEach(page -> update(page, options));

        knownPages.addAll(updatedPages);

        if (LOG.isInfoEnabled()) {
            logBatchComplete(updatedPages, startTime);
        }

        return knownPages;
    }

    /**
     * Load a batch of urls with the specified options.
     * <p>
     * Urls are fetched in a parallel manner whenever applicable.
     * If the batch is too large, only a random part of the urls is fetched immediately, all the rest urls are put into
     * a pending fetch list and will be fetched in background later.
     * <p>
     * If a page does not exists neither in local storage nor at the given remote location, {@link WebPage#NIL} is returned
     *
     * @param normUrls    The urls to load
     * @param options The options
     * @return Pages for all urls.
     */
    public Collection<WebPage> parallelLoadAll(Iterable<NormUrl> normUrls, LoadOptions options) {
        options.setPreferParallel(true);
        return loadAll(normUrls, options);
    }

    @Nonnull
    private WebPage loadOne(NormUrl normUrl) {
        Objects.requireNonNull(normUrl);

        if (normUrl.isInvalid()) {
            LOG.warn("Malformed url | {}", normUrl);
            return WebPage.NIL;
        }

        String url = normUrl.getUrl();
        LoadOptions opt = normUrl.getOptions();
        if (globalFetchingUrls.contains(url)) {
            LOG.debug("Load later, it's fetching by someone else | {}", url);
            // TODO: wait for finish?
            return WebPage.NIL;
        }

        boolean ignoreQuery = opt.getShortenKey();
        WebPage page = webDb.getOrNil(url, ignoreQuery);

        int reason = getFetchReason(page, opt);
        LOG.trace("Fetch reason: {}, url: {}, options: {}", getFetchReason(reason), page.getUrl(), opt);

        if (reason == FETCH_REASON_TEMP_MOVED) {
            return redirect(page, opt);
        }

        boolean refresh = reason == FETCH_REASON_NEW_PAGE
                || reason == FETCH_REASON_EXPIRED
                || reason == FETCH_REASON_SMALL_CONTENT
                || reason == FETCH_REASON_MISS_FIELD;
        if (refresh) {
            if (page.isNil()) {
                page = WebPage.newWebPage(url, ignoreQuery);
            }

            page = fetchComponent.initFetchEntry(page, opt);

            // LOG.debug("Fetching: {} | FetchMode: {}", page.getConfiguredUrl(), page.getFetchMode());

            globalFetchingUrls.add(url);
            page = fetchComponent.fetchContent(page);
            globalFetchingUrls.remove(url);

            update(page, opt);

            if (LOG.isInfoEnabled()) {
                LOG.info(getPageCompleteReport(page));
            }
        }

        return page;
    }

    @Nullable
    private NormUrl filterUrlToNull(NormUrl url) {
        String u = filterUrlToNull(url.getUrl());
        if (u == null) return null;
        return new NormUrl(u, url.getOptions());
    }

    @Nullable
    private String filterUrlToNull(String url) {
        if (url.length() <= SHORTEST_VALID_URL_LENGTH || url.contains(NIL_PAGE_URL) || url.contains(EXAMPLE_URL)) {
            return null;
        }

        if (globalFetchingUrls.contains(url)) {
            return null;
        }

        if (fetchTaskTracker.isFailed(url)) {
            return null;
        }

        if (fetchTaskTracker.isTimeout(url)) {

        }

        if (!Urls.isValidUrl(url)) {
            return null;
        }

        // Might use UrlFilter/UrlNormalizer

        return url;
    }

    private int getFetchReason(WebPage page, LoadOptions options) {
        Objects.requireNonNull(page);
        Objects.requireNonNull(options);

        String url = page.getUrl();
        if (page.isNil()) {
            return FETCH_REASON_NEW_PAGE;
        } else if (page.isInternal()) {
            LOG.warn("Do not fetch, page is internal, {}", url);
            return FETCH_REASON_DO_NOT_FETCH;
        }

        ProtocolStatus protocolStatus = page.getProtocolStatus();
        if (protocolStatus.isNotFetched()) {
            return FETCH_REASON_NEW_PAGE;
        } else if (protocolStatus.isTempMoved()) {
            return FETCH_REASON_TEMP_MOVED;
        } else if (protocolStatus.isFailed()) {
            // Page is fetched last time, but failed, if retry is not allowed, just return the failed page

            if (!options.getRetry()) {
                LOG.warn("Ignore failed page, last status: {} | {} {}",
                        page.getProtocolStatus(), page.getUrl(), page.getOptions());
                return FETCH_REASON_DO_NOT_FETCH;
            }
        }

        Instant now = Instant.now();
        Instant lastFetchTime = page.getLastFetchTime(now);

        if (lastFetchTime.isBefore(PulsarConstants.TCP_IP_STANDARDIZED_TIME)) {
            LOG.warn("Invalid last fetch time: {}, last status: {}", lastFetchTime, page.getProtocolStatus());
        }

        // if (now > lastTime + expires), it's expired
        if (now.isAfter(lastFetchTime.plus(options.getExpires()))) {
            return FETCH_REASON_EXPIRED;
        }

        if (page.getContentBytes() < options.getRequireSize()) {
            return FETCH_REASON_SMALL_CONTENT;
        }

        BrowserJsData jsData = page.getBrowserJsData();
        if (jsData != null) {
            BrowserJsData.Stat stat = jsData.getLastStat();
            if (stat.getNi() < options.getRequireImages()) {
                return FETCH_REASON_MISS_FIELD;
            }

            if (stat.getNa() < options.getRequireAnchors()) {
                return FETCH_REASON_MISS_FIELD;
            }
        }

        return FETCH_REASON_DO_NOT_FETCH;
    }

    @Nonnull
    private WebPage redirect(WebPage page, LoadOptions options) {
        String reprUrl = page.getReprUrl();
        if (reprUrl.equalsIgnoreCase(page.getUrl())) {
            LOG.warn("Invalid reprUrl, cyclic redirection, url: " + reprUrl);
            return page;
        }

        if (options.getNoRedirect()) {
            LOG.warn("Redirect is prohibit, url: " + reprUrl);
            return page;
        }

        // do not run into a rabbit hole, never redirects here
        options.setNoRedirect(true);
        WebPage redirectedPage = load(reprUrl, options);
        options.setNoRedirect(false);

        if (options.getHardRedirect()) {
            // soft redirect
            page = redirectedPage;
        } else {
            page.setContent(redirectedPage.getContent());
        }

        return page;
    }

    private void update(WebPage page, LoadOptions options) {
        ProtocolStatus protocolStatus = page.getProtocolStatus();
        if (protocolStatus.isFailed()) {
            // Follow redirection
            updateComponent.updateFetchSchedule(page);
            return;
        }

        if (options.getParse()) {
            ParseResult parseResult = parseComponent.parse(page,
                    options.getQuery(),
                    options.getReparseLinks(),
                    options.getNoFilter());

            if (LOG.isTraceEnabled()) {
                LOG.trace("ParseResult: {} ParseReport: {}", parseResult, parseComponent.getReport());
            }
        }

        updateComponent.updateFetchSchedule(page);

        if (!protocolStatus.isCanceled() && options.getPersist()) {
            webDb.put(page);

            if (!options.getLazyFlush()) {
                flush();
            }

            if (LOG.isTraceEnabled()) {
                LOG.trace("Persisted {} | {}", StringUtil.readableByteCount(page.getContentBytes()), page.getUrl());
            }
        }
    }

    private void logBatchComplete(Collection<WebPage> pages, Instant startTime) {
        Duration elapsed = DateTimeUtil.elapsedTime(startTime);
        String message = String.format("Fetched total %d pages in %s:\n", pages.size(), DateTimeUtil.readableDuration(elapsed));
        StringBuilder sb = new StringBuilder(message);
        AtomicInteger i = new AtomicInteger();
        pages.forEach(p -> sb.append(i.incrementAndGet()).append(".\t").append(getPageCompleteReport(p)).append('\n'));
        LOG.info(sb.toString());
    }

    private String getPageCompleteReport(WebPage page) {
        int bytes = page.getContentBytes();
        if (bytes < 0) {
            return "";
        }

        String responseTime = page.getMetadata().get(RESPONSE_TIME);
        String proxy = page.getMetadata().get(PROXY);
        BrowserJsData jsData = page.getBrowserJsData();
        String jsSate = "";
        if (jsData != null) {
            BrowserJsData.Stat stat = jsData.getLastStat();
            jsSate = String.format(" i/a/nm/st:%d/%d/%d/%d", stat.getNi(), stat.getNa(), stat.getNnm(), stat.getNst());
        }
        return String.format("Fetched%s in %8s%26s, fc:%2d %24s | %s",
                StringUtil.readableByteCount(bytes, 7, false),
                DateTimeUtil.readableDuration(responseTime),
                proxy == null ? "" : " via " + proxy,
                page.getFetchCount(),
                jsSate,
                page.getUrl()
        );
    }

    /**
     * We load everything from the internet, our storage is just a cache
     */
    @InterfaceStability.Evolving
    public Map<String, Object> loadOutPages(
            @Nonnull String url, @Nonnull String loadArgs,
            @Nonnull String linkArgs,
            int start, int limit, @Nonnull String loadArgs2,
            @Nonnull String query,
            int logLevel) {
        return loadOutPages(url, LoadOptions.Companion.parse(loadArgs), LinkOptions.parse(linkArgs),
                start, limit, LoadOptions.Companion.parse(loadArgs2), query, logLevel);
    }

    /**
     * We load everything from the internet, our storage is just a cache
     */
    @InterfaceStability.Evolving
    public Map<String, Object> loadOutPages(
            String url, LoadOptions options,
            LinkOptions linkOptions,
            int start, int limit, LoadOptions loadOptions2,
            String query,
            int logLevel) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(options);
        Objects.requireNonNull(linkOptions);
        Objects.requireNonNull(loadOptions2);
        Objects.requireNonNull(query);

        boolean persist = options.getPersist();
        options.setPersist(false);
        boolean persist2 = loadOptions2.getPersist();
        loadOptions2.setPersist(false);

        WebPage page = load(url, options);

        List<GHypeLink> filteredLinks = Collections.emptyList();
        List<WebPage> outPages = Collections.emptyList();
        List<Map<String, Object>> outDocs = Collections.emptyList();

        int[] counters = {0, 0, 0};
        if (page.getProtocolStatus().isSuccess()) {
            filteredLinks = page.getLiveLinks().values().stream()
                    .filter(l -> !l.getUrl().toString().equals(url))
                    .filter(l -> !page.getDeadLinks().contains(new Utf8(l.getUrl().toString())))
                    .filter(linkOptions.asGHypeLinkPredicate())
                    .collect(Collectors.toList());
            loadOptions2.setQuery(query);
            outPages = loadOutPages(filteredLinks, start, limit, loadOptions2);

            outPages.stream().map(WebPage::getPageCounters).forEach(c -> {
                counters[0] += c.get(PageCounters.Self.missingFields);
                counters[1] += counters[0] > 0 ? 1 : 0;
                counters[2] += c.get(PageCounters.Self.brokenSubEntity);
            });

            updateComponent.updateByOutgoingPages(page, outPages);
            if (persist) {
                webDb.put(page);
            }
            if (persist2) {
                outPages.forEach(webDb::put);
            }
            // log.debug(page.getPageCounters().asStringMap().toString());

            Function<WebPage, Map<String, Object>> converter =
                    p -> new WebPageFormatter(p)
                            .withLinks(loadOptions2.getWithLinks())
                            .withText(loadOptions2.getWithText())
                            .withEntities(loadOptions2.getWithModel())
                            .toMap();
            outDocs = outPages.stream().map(converter).collect(Collectors.toList());
        }

        // Metadata
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalCount", filteredLinks.size());
        response.put("count", outPages.size());

        // Counters
        Map<String, Integer> refCounter = new LinkedHashMap<>();
        refCounter.put("missingFields", counters[0]);
        refCounter.put("brokenEntity", counters[1]);
        refCounter.put("brokenSubEntity", counters[2]);
        response.put("refCounter", refCounter);

        // Main document
        response.put("doc", new WebPageFormatter(page)
                .withLinks(options.getWithLinks())
                .withText(options.getWithText())
                .withEntities(options.getWithModel())
                .toMap());

        // Outgoing document
        response.put("docs", outDocs);
        if (logLevel > 0) {
            options.setPersist(persist);
            loadOptions2.setPersist(persist2);
            response.put("debug", buildDebugInfo(logLevel, linkOptions, options, loadOptions2));
        }

        if (!options.getLazyFlush()) {
            flush();
        }

        return response;
    }

    public List<WebPage> loadOutPages(List<GHypeLink> links, int start, int limit, LoadOptions options) {
        List<WebPage> pages = links.stream()
                .skip(start > 1 ? start - 1 : 0)
                .limit(limit)
                .map(l -> load(l, options))
                .filter(p -> p.getProtocolStatus().isSuccess())
                .collect(Collectors.toList());

        if (!options.getLazyFlush()) {
            flush();
        }

        return pages;
    }

    public void flush() {
        webDb.flush();
    }

    /**
     * Not stable
     */
    @InterfaceStability.Unstable
    private Map<String, String> buildDebugInfo(
            int logLevel, LinkOptions linkOptions, LoadOptions options, LoadOptions loadOptions2) {
        Map<String, String> debugInfo = new HashMap<>();

        final int[] counter = {0};
        String linkReport = linkOptions.getReport().stream()
                .map(r -> (++counter[0]) + ".\t" + r).collect(Collectors.joining("\n"));

        debugInfo.put("logLevel", String.valueOf(logLevel));
        debugInfo.put("options", options.toString());
        debugInfo.put("loadOptions2", loadOptions2.toString());
        debugInfo.put("linkOptions", linkOptions.toString());
        debugInfo.put("linkReport", linkReport);

        return debugInfo;
    }
}
