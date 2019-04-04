package ai.platon.pulsar.crawl.component;

import ai.platon.pulsar.common.URLUtil;
import ai.platon.pulsar.common.Urls;
import ai.platon.pulsar.common.config.PulsarConstants;
import ai.platon.pulsar.common.options.LinkOptions;
import ai.platon.pulsar.common.options.LoadOptions;
import ai.platon.pulsar.persist.*;
import ai.platon.pulsar.persist.gora.generated.GHypeLink;
import ai.platon.pulsar.crawl.fetch.TaskStatusTracker;
import ai.platon.pulsar.crawl.parse.ParseResult;
import org.apache.avro.util.Utf8;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import static ai.platon.pulsar.common.config.PulsarConstants.*;
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
    public static final int FETCH_REASON_TEMP_MOVED = 3;
    public static final int FETCH_REASON_RETRY_ON_FAILURE = 4;
    public static final HashMap<Integer, String> fetchReasonCodes = new HashMap<>();

    static {
        fetchReasonCodes.put(FETCH_REASON_DO_NOT_FETCH, "do_not_fetch");
        fetchReasonCodes.put(FETCH_REASON_NEW_PAGE, "new_page");
        fetchReasonCodes.put(FETCH_REASON_EXPIRED, "expired");
        fetchReasonCodes.put(FETCH_REASON_TEMP_MOVED, "temp_moved");
        fetchReasonCodes.put(FETCH_REASON_RETRY_ON_FAILURE, "retry_on_failure");
    }

    private final WebDb webDb;
    private final BatchFetchComponent fetchComponent;
    private final ParseComponent parseComponent;
    private final UpdateComponent updateComponent;
    private final Set<String> fetchingUrls = Collections.synchronizedSet(new HashSet<>());
    private final TaskStatusTracker taskStatusTracker;

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
        this.taskStatusTracker = fetchComponent.getTaskStatusTracker();
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
     * Load a url, options can be specified following the url, see {@link LoadOptions} for all options
     *
     * @param configuredUrl The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, {@link WebPage#NIL} is returned
     */
    @Nonnull
    public WebPage load(String configuredUrl) {
        kotlin.Pair<String, String> urlAndOptions = Urls.splitUrlArgs(configuredUrl);
        LoadOptions options = LoadOptions.parse(urlAndOptions.getSecond());
        return load(urlAndOptions.getFirst(), options);
    }

    /**
     * Load a url with specified options, see {@link LoadOptions} for all options
     *
     * @param originalUrl The url to load
     * @param options The options
     * @return The WebPage. If there is no web page at local storage nor remote location, {@link WebPage#NIL} is returned
     */
    @Nonnull
    public WebPage load(String originalUrl, String options) {
        return load(originalUrl, LoadOptions.parse(options));
    }

    /**
     * Load a url with specified options
     * If there is no page in local storage nor at the given remote location, {@link WebPage#NIL} is returned
     *
     * @param originalUrl     The url to load
     * @param options The options
     * @return The WebPage.
     */
    @Nonnull
    public WebPage load(String originalUrl, LoadOptions options) {
        Objects.requireNonNull(options);
        return loadInternal(originalUrl, options);
    }

    /**
     * Load a url in {@link GHypeLink} format
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
     * @param originalUrls    The urls to load
     * @param options The options
     * @return Pages for all urls.
     */
    public Collection<WebPage> loadAll(Iterable<String> originalUrls, LoadOptions options) {
        Objects.requireNonNull(options);

        Set<String> filteredUrls = new HashSet<>();
        CollectionUtils.collect(originalUrls, url -> addIgnoreNull(filteredUrls, filterUrlToNull(url)));

        if (filteredUrls.isEmpty()) {
            return new ArrayList<>();
        }

        Set<WebPage> knownPages = new HashSet<>();
        Set<String> pendingUrls = new HashSet<>();

        boolean ignoreFailed = options.isIgnoreFailed();

        for (String originalUrl : filteredUrls) {
            WebPage page = webDb.getOrNil(originalUrl, options.isShortenKey());

            int reason = getFetchReason(page, options.getExpires(), options.isRetry());
            if (LOG.isTraceEnabled()) {
                LOG.trace("Fetch reason: {}, url: {}, options: {}", getFetchReason(reason), originalUrl, options);
            }

            if (reason == FETCH_REASON_NEW_PAGE) {
                pendingUrls.add(originalUrl);
            } else if (reason == FETCH_REASON_EXPIRED) {
                pendingUrls.add(originalUrl);
            } else if (reason == FETCH_REASON_TEMP_MOVED) {
                // TODO: batch redirect
                page = redirect(page, options);
                if (page.getProtocolStatus().isSuccess()) {
                    knownPages.add(page);
                }
            } else if (reason == FETCH_REASON_DO_NOT_FETCH) {
                if (page.getProtocolStatus().isSuccess()) {
                    knownPages.add(page);
                }
            } else {
                LOG.error("Unknown fetch reason #{}, url: {}, options: {}", reason, originalUrl, options);
            }
        }

        if (pendingUrls.isEmpty()) {
            return knownPages;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Fetching {} urls with options {}", pendingUrls.size(), options);
        }

        Collection<WebPage> updatedPages;
        fetchingUrls.addAll(pendingUrls);
        if (options.isPreferParallel()) {
            updatedPages = fetchComponent.parallelFetchAll(pendingUrls, options);
        } else {
            updatedPages = fetchComponent.fetchAll(pendingUrls, options);
        }
        fetchingUrls.removeAll(pendingUrls);

        updatedPages.forEach(page -> update(page, options));

        if (ignoreFailed) {
            CollectionUtils.filter(updatedPages, page -> page.getProtocolStatus().isSuccess());
        }
        knownPages.addAll(updatedPages);

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
     * @param originalUrls    The urls to load
     * @param options The options
     * @return Pages for all urls.
     */
    public Collection<WebPage> parallelLoadAll(Iterable<String> originalUrls, LoadOptions options) {
        options.setPreferParallel(true);
        return loadAll(originalUrls, options);
    }

    @Nonnull
    private WebPage loadInternal(String originalUrl, LoadOptions options) {
        Objects.requireNonNull(originalUrl);
        Objects.requireNonNull(options);

        URL u = Urls.getURLOrNull(originalUrl);
        if (u == null) {
            LOG.warn("Malformed url {}", originalUrl);
            return WebPage.NIL;
        }

        if (fetchingUrls.contains(originalUrl)) {
            LOG.debug("Load later, it's fetching by someone else. Url: {}", originalUrl);
            return WebPage.NIL;
        }

        boolean ignoreQuery = options.isShortenKey();
        WebPage page = webDb.getOrNil(originalUrl, ignoreQuery);

        int reason = getFetchReason(page, options.getExpires(), options.isRetry());
        if (LOG.isTraceEnabled()) {
            LOG.trace("Fetch reason: {}, url: {}, options: {}", getFetchReason(reason), originalUrl, options);
        }

        if (reason == FETCH_REASON_TEMP_MOVED) {
            return redirect(page, options);
        }

        boolean refresh = (reason == FETCH_REASON_NEW_PAGE) || (reason == FETCH_REASON_EXPIRED);
        if (refresh) {
            if (page.isNil()) {
                page = WebPage.newWebPage(originalUrl, ignoreQuery);
            }

            page = fetchComponent.initFetchEntry(page, options);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Fetching: " + page.getConfiguredUrl() + " | FetchMode: " + page.getFetchMode());
            }

            fetchingUrls.add(originalUrl);
            page = fetchComponent.fetchContent(page);
            fetchingUrls.remove(originalUrl);

            update(page, options);
        }

        return page;
    }

    @Nullable
    private String filterUrlToNull(String url) {
        if (url.length() <= SHORTEST_VALID_URL_LENGTH || url.contains(NIL_PAGE_URL) || url.contains(EXAMPLE_URL)) {
            return null;
        }

        if (fetchingUrls.contains(url)) {
            return null;
        }

        if (taskStatusTracker.isFailed(url)) {

        }

        if (taskStatusTracker.isTimeout(url)) {

        }

        if (!Urls.isValidUrl(url)) {
            return null;
        }

        // Might use UrlFilter/UrlNormalizer

        return url;
    }

    private int getFetchReason(WebPage page, Duration expires, boolean retry) {
        Objects.requireNonNull(page);
        Objects.requireNonNull(expires);

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

            if (!retry) {
                LOG.warn("Ignore failed page, last status: " + page.getProtocolStatus() + ", options: " + page.getOptions());
                return FETCH_REASON_DO_NOT_FETCH;
            }
        }

        Instant now = Instant.now();
        Instant lastFetchTime = page.getLastFetchTime(now);

        if (lastFetchTime.isBefore(PulsarConstants.TCP_IP_STANDARDIZED_TIME)) {
            LOG.warn("Invalid last fetch time: {}, last status: {}", lastFetchTime, page.getProtocolStatus());
        }

        // if (now > lastTime + expires), it's expired
        if (now.isAfter(lastFetchTime.plus(expires))) {
            return FETCH_REASON_EXPIRED;
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

        if (options.isNoRedirect()) {
            LOG.warn("Redirect is prohibit, url: " + reprUrl);
            return page;
        }

        // do not run into a rabbit hole
        options.setNoRedirect(true);
        WebPage redirectedPage = load(reprUrl, options);
        options.setNoRedirect(false);

        if (options.isHardRedirect()) {
            page = redirectedPage;
        } else {
            // soft redirect
            page.setContent(redirectedPage.getContent());
        }

        return page;
    }

    private void update(WebPage page, LoadOptions options) {
        ProtocolStatus protocolStatus = page.getProtocolStatus();
        if (protocolStatus.isFailed()) {
            // Follow redirection
            if (LOG.isDebugEnabled()) {
                LOG.debug("Fetch failed: " + protocolStatus);
            }
            updateComponent.updateFetchSchedule(page);
            return;
        }

        if (options.isParse()) {
            ParseResult parseResult = parseComponent.parse(page,
                    options.getQuery(),
                    options.isReparseLinks(),
                    options.isNoLinkFilter());

            if (LOG.isTraceEnabled()) {
                LOG.trace("ParseResult: " + parseResult.toString());
                LOG.trace("ParseReport: " + parseComponent.getReport());
            }
        }

        updateComponent.updateFetchSchedule(page);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Fetched: " + page.getConfiguredUrl() + " | LAST_BROWSER: " + page.getLastBrowser());
        }

        if (options.isPersist()) {
            webDb.put(page);

            if (options.isAutoFlush()) {
                flush();
            }

            if (LOG.isTraceEnabled()) {
                LOG.trace("Persisted: " + page.getConfiguredUrl());
            }
        }
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
        return loadOutPages(url, LoadOptions.parse(loadArgs), LinkOptions.parse(linkArgs),
                start, limit, LoadOptions.parse(loadArgs2), query, logLevel);
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

        boolean persist = options.isPersist();
        options.setPersist(false);
        boolean persist2 = loadOptions2.isPersist();
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
                            .withLinks(loadOptions2.isWithLinks())
                            .withText(loadOptions2.isWithText())
                            .withEntities(loadOptions2.isWithModel())
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
                .withLinks(options.isWithLinks())
                .withText(options.isWithText())
                .withEntities(options.isWithModel())
                .toMap());

        // Outgoing document
        response.put("docs", outDocs);
        if (logLevel > 0) {
            options.setPersist(persist);
            loadOptions2.setPersist(persist2);
            response.put("debug", buildDebugInfo(logLevel, linkOptions, options, loadOptions2));
        }

        if (options.isAutoFlush()) {
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

        if (options.isAutoFlush()) {
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
