package fun.platonic.pulsar;

import fun.platonic.pulsar.common.ConcurrentLRUCache;
import fun.platonic.pulsar.common.UrlUtil;
import fun.platonic.pulsar.common.config.AbstractTTLConfiguration;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.options.LoadOptions;
import fun.platonic.pulsar.persist.WebPage;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static fun.platonic.pulsar.common.PulsarConstants.APP_CONTEXT_CONFIG_LOCATION;
import static fun.platonic.pulsar.common.config.CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION;

/**
 * Created by vincent on 18-1-17.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class PulsarSession extends AbstractTTLConfiguration implements AutoCloseable {
    public static final Logger LOG = LoggerFactory.getLogger(PulsarSession.class);
    public static final Duration SESSION_PAGE_CACHE_TTL = Duration.ofSeconds(20);
    public static final int SESSION_PAGE_CACHE_CAPACITY = 1000;

    public static final Duration SESSION_DOCUMENT_CACHE_TTL = Duration.ofHours(1);
    public static final int SESSION_DOCUMENT_CACHE_CAPACITY = 10000;
    private static final AtomicInteger objectIdGenerator = new AtomicInteger();
    // All sessions share the same cache
    private static ConcurrentLRUCache<String, WebPage> pageCache;
    private static ConcurrentLRUCache<String, Document> documentCache;
    private final int id;
    private final Pulsar pulsar;
    private boolean enableCache = true;
    // Session variables
    private Map<String, Object> variables;

    public PulsarSession() {
        this(new ClassPathXmlApplicationContext(
                System.getProperty(APPLICATION_CONTEXT_CONFIG_LOCATION, APP_CONTEXT_CONFIG_LOCATION)));
    }

    public PulsarSession(String appConfigLocation) {
        this(new ClassPathXmlApplicationContext(appConfigLocation));
    }

    public PulsarSession(ConfigurableApplicationContext applicationContext) {
        id = objectIdGenerator.incrementAndGet();

        ImmutableConfig immutableConfig = applicationContext.getBean(ImmutableConfig.class);
        this.setFallbackConfig(immutableConfig);
        this.pulsar = new Pulsar(applicationContext);

        if (pageCache == null) {
            int capacity = getUint("session.page.cache.size", SESSION_PAGE_CACHE_CAPACITY);
            synchronized (PulsarSession.class) {
                pageCache = new ConcurrentLRUCache<>(SESSION_PAGE_CACHE_TTL.getSeconds(), capacity);
            }
        }

        if (documentCache == null) {
            int capacity = getUint("session.document.cache.size", SESSION_DOCUMENT_CACHE_CAPACITY);
            synchronized (PulsarSession.class) {
                documentCache = new ConcurrentLRUCache<>(SESSION_DOCUMENT_CACHE_TTL.getSeconds(), capacity);
            }
        }
    }

    public int getId() {
        return id;
    }

    public void disableCache() {
        enableCache = false;
    }

    public Pulsar getPulsar() {
        return pulsar;
    }

    @Nullable
    public String normalize(String url) {
        return pulsar.normalize(url);
    }

    /**
     * Inject a url
     *
     * @param configuredUrl The url followed by config options
     * @return The web page created
     */
    @Nonnull
    public WebPage inject(String configuredUrl) {
        return pulsar.inject(configuredUrl);
    }

    /**
     * Load a url with default options
     *
     * @param configuredUrl The url followed by config options
     * @return The web page
     */
    @Nonnull
    public WebPage load(String configuredUrl) {
        Pair<String, String> urlAndOptions = UrlUtil.splitUrlArgs(configuredUrl);
        LoadOptions options = LoadOptions.parse(urlAndOptions.getValue(), this);

        return load(urlAndOptions.getKey(), options);
    }

    /**
     * Load a url with specified options
     *
     * @param url     The url to load
     * @param options The load options
     * @return The web page
     */
    @Nonnull
    public WebPage load(String url, LoadOptions options) {
        options.setMutableConfig(this);

        if (enableCache) {
            return getCachedOrLoad(url, options);
        } else {
            return pulsar.load(url, options);
        }
    }

    /**
     * Load all urls with specified options, this may cause a parallel fetching if required
     *
     * @param urls    The urls to load
     * @param options The load options for all urls
     * @return The web pages
     */
    public Collection<WebPage> loadAll(Iterable<String> urls, LoadOptions options) {
        options.setMutableConfig(this);

        if (enableCache) {
            return getCachedOrLoadAll(urls, options);
        } else {
            return pulsar.loadAll(urls, options);
        }
    }

    /**
     * Load all urls with specified options, this causes a parallel fetching whenever applicable
     *
     * @param urls    The urls to load
     * @param options The load options
     * @return The web pages
     */
    public Collection<WebPage> parallelLoadAll(Iterable<String> urls, LoadOptions options) {
        options.setMutableConfig(this);
        options.setPreferParallel(true);

        if (enableCache) {
            return getCachedOrLoadAll(urls, options);
        } else {
            return pulsar.loadAll(urls, options);
        }
    }

    /**
     * Parse the Web page using Jsoup.
     * If the Web page is not changed since last parse, use the last result if available
     * */
    public Document parse(WebPage page) {
        String key = page.getKey() + "\t" + page.getFetchTime();

        Document document = documentCache.get(key);
        if (document == null) {
            document = pulsar.parse(page);
            documentCache.put(key, document);

            Instant prevFetchTime = page.getPrevFetchTime();
            if (prevFetchTime.plusSeconds(3600).isAfter(Instant.now())) {
                // It might be still in the cache
                String oldKey = page.getKey() + "\t" + prevFetchTime;
                documentCache.tryRemove(oldKey);
            }
        }

        return document;
    }

    private WebPage getCachedOrGet(String url) {
        WebPage page = pageCache.get(url);
        if (page != null) {
            return page;
        }

        page = pulsar.get(url);
        pageCache.put(url, page);

        return page;
    }

    private WebPage getCachedOrLoad(String url, LoadOptions options) {
        WebPage page = pageCache.get(url);
        if (page != null) {
            return page;
        }

        page = pulsar.load(url, options);
        pageCache.put(url, page);

        return page;
    }

    private Collection<WebPage> getCachedOrLoadAll(Iterable<String> urls, LoadOptions options) {
        Collection<WebPage> pages = new ArrayList<>();
        Collection<String> pendingUrls = new ArrayList<>();

        for (String url : urls) {
            WebPage page = pageCache.get(url);
            if (page != null) {
                pages.add(page);
            } else {
                pendingUrls.add(url);
            }
        }

        Collection<WebPage> freshPages;
        if (options.isPreferParallel()) {
            freshPages = pulsar.parallelLoadAll(pendingUrls, options);
        } else {
            freshPages = pulsar.loadAll(pendingUrls, options);
        }

        pages.addAll(freshPages);

        // Notice: we do not cache batch loaded pages, batch loaded pages are not used frequently
        // do not do this: sessionCachePutAll(freshPages);

        return pages;
    }

    public Map<String, Object> getVariables() {
        if (variables == null) {
            variables = Collections.synchronizedMap(new HashMap<>());
        }
        return variables;
    }

    public Object getVariable(String name) {
        Map<String, Object> vars = getVariables();
        return vars.get(name);
    }

    public void setVariable(String name, Object value) {
        Map<String, Object> vars = getVariables();
        vars.put(name, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PulsarSession)) {
            return false;
        }

        PulsarSession otherSession = (PulsarSession) obj;
        return otherSession == this || otherSession.getId() == getId();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(getId());
    }

    @Override
    public String toString() {
        return "#" + getId();
    }

    @Override
    public void close() {
        pulsar.close();
    }

    @Override
    public boolean isExpired(String key) {
        return false;
    }

}
