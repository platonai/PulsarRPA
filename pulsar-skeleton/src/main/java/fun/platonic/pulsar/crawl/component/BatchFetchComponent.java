package fun.platonic.pulsar.crawl.component;

import com.google.common.collect.Lists;
import fun.platonic.pulsar.common.GlobalExecutor;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.config.MutableConfig;
import fun.platonic.pulsar.common.options.LoadOptions;
import org.apache.commons.collections4.CollectionUtils;
import fun.platonic.pulsar.crawl.fetch.TaskStatusTracker;
import fun.platonic.pulsar.crawl.protocol.Protocol;
import fun.platonic.pulsar.crawl.protocol.ProtocolFactory;
import fun.platonic.pulsar.crawl.protocol.Response;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.gora.db.WebDb;
import fun.platonic.pulsar.persist.metadata.FetchMode;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static fun.platonic.pulsar.common.config.CapabilityTypes.FETCH_EAGER_FETCH_LIMIT;
import static org.apache.commons.lang3.StringUtils.substringBefore;

public class BatchFetchComponent extends FetchComponent {
    /**
     * The weak index serves lazy fetch. If the component is not available, lazy fetch is disabled
     */
    private WebDb webDb;
    private ProtocolFactory protocolFactory;
    private GlobalExecutor executorService;

    public BatchFetchComponent(
            WebDb webDb, TaskStatusTracker statusTracker, ProtocolFactory protocolFactory, ImmutableConfig immutableConfig) {
        super(protocolFactory, statusTracker, immutableConfig);
        this.protocolFactory = protocolFactory;
        this.webDb = webDb;
        this.executorService = GlobalExecutor.getInstance(immutableConfig);
    }

    /**
     * Fetch all the urls, config property 'fetch.concurrency' controls the concurrency level.
     * If concurrency level is not great than 1, fetch all urls in the caller thread
     * <p>
     * Eager fetch only some urls to response as soon as possible, the rest urls will be fetched in background later
     *
     * @param urls    The urls to fetch
     * @param options The options
     * @return The fetch result
     */
    public Collection<WebPage> fetchAll(Iterable<String> urls, LoadOptions options) {
        Objects.requireNonNull(options);

        return fetchAllInternal(urls, options);
    }

    /**
     * Parallel fetch all the urls
     * <p>
     * Eager fetch only some urls to response as soon as possible, the rest urls will be fetched in background later
     *
     * @param urls    The urls to fetch
     * @param options The options
     * @return The fetch result
     */
    public Collection<WebPage> parallelFetchAll(Iterable<String> urls, LoadOptions options) {
        Objects.requireNonNull(options);

        FetchMode mode = options.getFetchMode();
        Protocol protocol = protocolFactory.getProtocol(mode);
        if (protocol == null) {
            return parallelFetchAllGroupedBySchema(urls, options);
        }

        return parallelFetchAllInternal(urls, protocol, options);
    }

    /**
     * Group all urls by URL schema, and parallel fetch each group
     * <p>
     * Eager fetch only some urls to response as soon as possible, the rest urls will be fetched in background later
     *
     * @param urls    The urls to fetch
     * @param options The options
     * @return The fetch result
     */
    public Collection<WebPage> parallelFetchAllGroupedBySchema(Iterable<String> urls, LoadOptions options) {
        Objects.requireNonNull(options);

        List<WebPage> pages = new ArrayList<>();

        Map<String, List<String>> groupedUrls = optimizeBatchSize(Lists.newArrayList(urls), options).stream()
                .collect(Collectors.groupingBy(url -> substringBefore(url, "://")));

        groupedUrls.forEach((key, gUrls) -> {
            Protocol protocol = protocolFactory.getProtocol(key);
            if (protocol != null) {
                pages.addAll(parallelFetchAllInternal(gUrls, protocol, options));
            } else {
                taskStatusTracker.trackFailed(gUrls);
            }
        });

        return pages;
    }

    /**
     * Fetch all urls, if allowParallel is true and the config suggests parallel is preferred, parallel fetch all items
     * <p>
     * Eager fetch only some urls to response as soon as possible, the rest urls will be fetched in background later
     * <p>
     * If the protocol supports native parallel, use the protocol's batch fetch method,
     * Or else parallel fetch pages in a ExecutorService
     */
    private Collection<WebPage> fetchAllInternal(Iterable<String> urls, LoadOptions options) {
        Objects.requireNonNull(options);

        if (options.isPreferParallel()) {
            return parallelFetchAll(urls, options);
        } else {
            return optimizeBatchSize(urls, options).stream()
                    .map(url -> fetch(url, options))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Parallel fetch all urls
     * If the protocol supports native parallel, use the protocol's native batch fetch method,
     * Or else parallel fetch pages in a ExecutorService
     */
    private Collection<WebPage> parallelFetchAllInternal(Iterable<String> urls, Protocol protocol, LoadOptions options) {
        urls = optimizeBatchSize(urls, options);

        if (protocol.supportParallel()) {
            return protocolParallelFetchAll(urls, protocol, options);
        } else {
            return manualParallelFetchAll(urls, options);
        }
    }

    private Collection<WebPage> protocolParallelFetchAll(Iterable<String> urls, Protocol protocol, LoadOptions options) {
        MutableConfig mutableConfig = options.getMutableConfig();

        Collection<WebPage> pages = CollectionUtils.collect(urls, url -> WebPage.newWebPage(url, mutableConfig));
        return protocol.getResponses(pages, mutableConfig).stream()
                .map(response -> forwardResponse(protocol, response, options))
                .collect(Collectors.toList());
    }

    private Collection<WebPage> manualParallelFetchAll(Iterable<String> urls, LoadOptions options) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Manual parallel fetch urls");
        }

        Collection<Future<WebPage>> futures = CollectionUtils.collect(urls,
                url -> executorService.getExecutor().submit(() -> fetch(url, options)));

        return CollectionUtils.collect(futures, this::getResponse);
    }

    /**
     * Forward previous fetched response to protocol for further process: retry, status processing, etc
     */
    @Nonnull
    private WebPage forwardResponse(Protocol protocol, Response response, LoadOptions options) {
        WebPage page = WebPage.newWebPage(response.getUrl(), options.getMutableConfig());
        page.setOptions(options.toString());

        protocol.setResponse(response);
        return processProtocolOutput(page, protocol.getProtocolOutput(page));
    }

    private Collection<String> optimizeBatchSize(Iterable<String> urls, LoadOptions options) {
        if (urls instanceof Collection) {
            return optimizeBatchSize((Collection<String>) urls, options);
        }

        return optimizeBatchSize(Lists.newArrayList(urls), options);
    }

    /**
     * If there are too many urls to fetch, just fetch some of them in the foreground and
     * fetch the rest in the background
     */
    private Collection<String> optimizeBatchSize(Collection<String> urls, LoadOptions options) {
        if (webDb == null) {
            return urls;
        }

        ImmutableConfig mutableConfig = options.getMutableConfig();
        final int eagerFetchLimit = mutableConfig.getUint(FETCH_EAGER_FETCH_LIMIT, 20);
        if (urls.size() <= eagerFetchLimit) {
            return urls;
        }

        List<String> eagerTasks = new ArrayList<>(eagerFetchLimit);
        List<String> lazyTasks = new ArrayList<>(Math.max(0, urls.size() - eagerFetchLimit));
        int i = 0;
        for (String url : urls) {
            if (i < eagerFetchLimit) {
                eagerTasks.add(url);
            } else {
                lazyTasks.add(url);
            }
            ++i;
        }

        if (!lazyTasks.isEmpty()) {
            FetchMode mode = options.getFetchMode();
            // TODO: save url with options
            taskStatusTracker.commitLazyTasks(mode, lazyTasks);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Committed {} lazy tasks in mode {}", lazyTasks.size(), mode);
            }
        }

        return eagerTasks;
    }

    private WebPage getResponse(Future<WebPage> future) {
        try {
            return future.get(35, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted when fetch resource " + e.toString());
        } catch (ExecutionException e) {
            LOG.warn(e.toString());
        } catch (TimeoutException e) {
            LOG.warn("Fetch resource timeout, " + e.toString());
        }

        return WebPage.NIL;
    }
}
