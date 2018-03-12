package org.warps.pulsar;

import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.nodes.Document;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.warps.pulsar.common.UrlUtil;
import org.warps.pulsar.common.config.ImmutableConfig;
import org.warps.pulsar.common.config.MutableConfig;
import org.warps.pulsar.common.options.LoadOptions;
import org.warps.pulsar.crawl.component.*;
import org.warps.pulsar.crawl.parse.ParseResult;
import org.warps.pulsar.crawl.parse.html.JsoupParser;
import org.warps.pulsar.persist.WebPage;
import org.warps.pulsar.persist.gora.db.WebDb;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

import static org.warps.pulsar.common.PulsarConstants.APP_CONTEXT_CONFIG_LOCATION;
import static org.warps.pulsar.common.config.CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION;

public class Pulsar implements AutoCloseable {

    private final ImmutableConfig immutableConfig;
    private final WebDb webDb;
    private final InjectComponent injectComponent;
    private final LoadComponent loadComponent;
    private MutableConfig defaultMutableConfig;
    private boolean closed = false;

    public Pulsar() {
        this(new ClassPathXmlApplicationContext(
                System.getProperty(APPLICATION_CONTEXT_CONFIG_LOCATION, APP_CONTEXT_CONFIG_LOCATION)));
    }

    public Pulsar(String appConfigLocation) {
        this(new ClassPathXmlApplicationContext(appConfigLocation));
    }

    public Pulsar(ConfigurableApplicationContext applicationContext) {
        this.immutableConfig = applicationContext.getBean(MutableConfig.class);

        this.webDb = applicationContext.getBean(WebDb.class);
        this.injectComponent = applicationContext.getBean(InjectComponent.class);
        this.loadComponent = applicationContext.getBean(LoadComponent.class);

        this.defaultMutableConfig = new MutableConfig(immutableConfig.unbox());
    }

    public Pulsar(
            InjectComponent injectComponent,
            LoadComponent loadComponent,
            ImmutableConfig immutableConfig) {
        this.webDb = injectComponent.getWebDb();

        this.injectComponent = injectComponent;
        this.loadComponent = loadComponent;
        this.immutableConfig = immutableConfig;
    }

    public ImmutableConfig getImmutableConfig() {
        return immutableConfig;
    }

    public WebDb getWebDb() {
        return webDb;
    }

    /**
     * Inject a url
     *
     * @param configuredUrl The url followed by config options
     * @return The web page created
     */
    @Nonnull
    public WebPage inject(String configuredUrl) {
        return injectComponent.inject(UrlUtil.splitUrlArgs(configuredUrl));
    }

    @Nullable
    public WebPage get(String url) {
        return webDb.get(url);
    }

    public WebPage getOrNil(String url) {
        return webDb.getOrNil(url);
    }

    /**
     * Load a url, options can be specified following the url, see {@link LoadOptions} for all options
     *
     * @param configuredUrl The url followed by options
     * @return The WebPage. If there is no web page at local storage nor remote location, {@link WebPage#NIL} is returned
     */
    @Nonnull
    public WebPage load(String configuredUrl) {
        Pair<String, String> urlAndOptions = UrlUtil.splitUrlArgs(configuredUrl);

        LoadOptions options = LoadOptions.parse(urlAndOptions.getValue(), defaultMutableConfig);
        options.setMutableConfig(defaultMutableConfig);

        return loadComponent.load(urlAndOptions.getKey(), options);
    }

    /**
     * Load a url with specified options, see {@link LoadOptions} for all options
     *
     * @param url     The url to load
     * @param options The options
     * @return The WebPage. If there is no web page at local storage nor remote location, {@link WebPage#NIL} is returned
     */
    @Nonnull
    public WebPage load(String url, LoadOptions options) {
        if (options.getMutableConfig() == null) {
            options.setMutableConfig(defaultMutableConfig);
        }
        return loadComponent.load(url, options);
    }

    /**
     * Load a batch of urls with the specified options.
     * <p>
     * If the option indicates prefer parallel, urls are fetched in a parallel manner whenever applicable.
     * If the batch is too large, only a random part of the urls is fetched immediately, all the rest urls are put into
     * a pending fetch list and will be fetched in background later.
     * <p>
     * If a page does not exists neither in local storage nor at the given remote location, {@link WebPage#NIL} is returned
     *
     * @param urls    The urls to load
     * @return Pages for all urls.
     */
    public Collection<WebPage> loadAll(Iterable<String> urls) {
        return loadAll(urls, new LoadOptions());
    }

    /**
     * Load a batch of urls with the specified options.
     * <p>
     * If the option indicates prefer parallel, urls are fetched in a parallel manner whenever applicable.
     * If the batch is too large, only a random part of the urls is fetched immediately, all the rest urls are put into
     * a pending fetch list and will be fetched in background later.
     * <p>
     * If a page does not exists neither in local storage nor at the given remote location, {@link WebPage#NIL} is returned
     *
     * @param urls    The urls to load
     * @param options The options
     * @return Pages for all urls.
     */
    public Collection<WebPage> loadAll(Iterable<String> urls, LoadOptions options) {
        if (options.getMutableConfig() == null) {
            options.setMutableConfig(defaultMutableConfig);
        }
        return loadComponent.loadAll(urls, options);
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
     * @param urls    The urls to load
     * @return Pages for all urls.
     */
    public Collection<WebPage> parallelLoadAll(Iterable<String> urls) {
        return parallelLoadAll(urls, new LoadOptions());
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
     * @param urls    The urls to load
     * @param options The options
     * @return Pages for all urls.
     */
    public Collection<WebPage> parallelLoadAll(Iterable<String> urls, LoadOptions options) {
        if (options.getMutableConfig() == null) {
            options.setMutableConfig(defaultMutableConfig);
        }
        return loadComponent.parallelLoadAll(urls, options);
    }

    /**
     * Parse the WebPage using Jsoup
     * */
    @Nonnull
    public Document parse(WebPage page) {
        JsoupParser parser = new JsoupParser(page, immutableConfig);
        return parser.parse();
    }

    @Nonnull
    public Document parse(WebPage page, MutableConfig mutableConfig) {
        JsoupParser parser = new JsoupParser(page, mutableConfig);
        return parser.parse();
    }

    public void update(WebPage page) {
        webDb.put(page.getUrl(), page);
    }

    public void delete(String url) {
        webDb.delete(url);
    }

    public void delete(WebPage page) {
        webDb.delete(page.getUrl());
    }

    public void flush() {
        webDb.flush();
    }

    @Override
    public void close() {
        if (!closed) {
            injectComponent.close();
            webDb.close();
            closed = true;
        }
        // close all closable managed by this object, there is no one currently
    }
}
