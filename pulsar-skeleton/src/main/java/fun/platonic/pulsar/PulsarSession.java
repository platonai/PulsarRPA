package fun.platonic.pulsar;

import fun.platonic.pulsar.common.FastSmallLRUCache;
import fun.platonic.pulsar.common.UrlUtil;
import fun.platonic.pulsar.common.config.AbstractTTLConfiguration;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.options.LoadOptions;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import fun.platonic.pulsar.persist.WebPage;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by vincent on 18-1-17.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
public class PulsarSession extends AbstractTTLConfiguration implements AutoCloseable {
    public static final Logger LOG = LoggerFactory.getLogger(PulsarSession.class);
    public static final Duration SESSION_CACHE_TTL = Duration.ofSeconds(20);
    public static final int SESSION_CACHE_CAPACITY = 1000;
    private static final AtomicInteger objectIdGenerator = new AtomicInteger(0);
    // All sessions share the same cache
    private static FastSmallLRUCache<String, WebPage> theSessionCache;
    private final int id;
    private final Pulsar pulsar;
    private boolean enableCache = true;
    // Session variables
    private Map<String, Object> variables;

    public PulsarSession(ConfigurableApplicationContext applicationContext) {
        id = objectIdGenerator.incrementAndGet();

        ImmutableConfig immutableConfig = applicationContext.getBean(ImmutableConfig.class);
        this.setFallbackConfig(immutableConfig);
        this.pulsar = new Pulsar(applicationContext);

        if (theSessionCache == null) {
            int capacity = getUint("session.cache.size", SESSION_CACHE_CAPACITY);
            synchronized (PulsarSession.class) {
                theSessionCache = new FastSmallLRUCache<>(SESSION_CACHE_TTL.getSeconds(), capacity);
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

    @Nonnull
    public WebPage getPage(String url) {
        WebPage page = getCachedOrGet(url);
        return page == null ? WebPage.NIL : page;
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
     * Parse the web page into a document
     *
     * @param page The page to parse
     * @return The document
     */
    @Nonnull
    public Document parse(WebPage page) {
        return pulsar.parse(page, this);
    }

    private WebPage getCachedOrGet(String url) {
        WebPage page = theSessionCache.get(url);
        if (page != null) {
            return page;
        }

        page = pulsar.get(url);
        theSessionCache.put(url, page);

        return page;
    }

    private WebPage getCachedOrLoad(String url, LoadOptions options) {
        WebPage page = theSessionCache.get(url);
        if (page != null) {
            return page;
        }

        page = pulsar.load(url, options);
        theSessionCache.put(url, page);

        return page;
    }

    private Collection<WebPage> getCachedOrLoadAll(Iterable<String> urls, LoadOptions options) {
        Collection<WebPage> pages = new ArrayList<>();
        Collection<String> pendingUrls = new ArrayList<>();

        for (String url : urls) {
            WebPage page = theSessionCache.get(url);
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
