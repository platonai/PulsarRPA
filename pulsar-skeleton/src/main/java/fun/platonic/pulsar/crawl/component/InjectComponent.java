package fun.platonic.pulsar.crawl.component;

import com.beust.jcommander.Parameter;
import fun.platonic.pulsar.common.StringUtil;
import fun.platonic.pulsar.common.UrlUtil;
import fun.platonic.pulsar.common.WeakPageIndexer;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.config.MutableConfig;
import fun.platonic.pulsar.common.config.Params;
import fun.platonic.pulsar.common.config.ReloadableParameterized;
import fun.platonic.pulsar.common.options.CommonOptions;
import fun.platonic.pulsar.crawl.inject.SeedBuilder;
import fun.platonic.pulsar.persist.WebDb;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.metadata.Mark;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fun.platonic.pulsar.common.PulsarParams.*;
import static fun.platonic.pulsar.common.config.CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION;
import static fun.platonic.pulsar.common.config.CapabilityTypes.CRAWL_ID;
import static fun.platonic.pulsar.common.config.PulsarConstants.*;

/**
 * Created by vincent on 17-5-14.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class InjectComponent implements ReloadableParameterized, AutoCloseable {
    public static final Logger LOG = LoggerFactory.getLogger(InjectComponent.class);

    private ImmutableConfig conf;
    private SeedBuilder seedBuilder;
    private WebDb webDb;
    private WeakPageIndexer seedIndexer;
    private AtomicBoolean closed = new AtomicBoolean(false);

    public InjectComponent(SeedBuilder seedBuilder, WebDb webDb, ImmutableConfig conf) {
        this.seedBuilder = seedBuilder;
        this.webDb = webDb;
        this.seedIndexer = new WeakPageIndexer(SEED_HOME_URL, webDb);

        reload(conf);
    }

    public static void main(String[] args) throws Exception {
        InjectOptions opts = new InjectOptions(args);
        opts.parseOrExit();

        ApplicationContext context = new ClassPathXmlApplicationContext(
                System.getProperty(APPLICATION_CONTEXT_CONFIG_LOCATION, APP_CONTEXT_CONFIG_LOCATION));

        MutableConfig conf = context.getBean(MutableConfig.class);
        conf.setIfNotEmpty(CRAWL_ID, opts.crawlId);

        String seeds = opts.seeds.get(0);
        if (seeds.startsWith("@")) {
            seeds = new String(Files.readAllBytes(Paths.get(seeds.substring(1))));
        }
        List<String> configuredUrls = StringUtil.getUnslashedLines(seeds).stream()
                .filter(u -> !u.isEmpty() && !u.startsWith("#"))
                .sorted().distinct().collect(Collectors.toList());

        try (WebDb webDb = context.getBean(WebDb.class)) {
            InjectComponent injectComponent = new InjectComponent(new SeedBuilder(), webDb, conf);
            injectComponent.injectAll(configuredUrls.toArray(new String[0]));
            injectComponent.commit();
            System.out.println(injectComponent.report());
        }
    }

    @Override
    public void reload(ImmutableConfig conf) {
        this.conf = conf;
    }

    @Override
    public ImmutableConfig getConf() {
        return conf;
    }

    @Override
    public Params getParams() {
        return seedBuilder.getParams();
    }

    @Nonnull
    public WebPage inject(Pair<String, String> urlArgs) {
        return inject(urlArgs.getKey(), urlArgs.getValue());
    }

    @Nonnull
    public WebPage inject(@Nonnull String url, String args) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(args);

        WebPage page = webDb.getOrNil(url);

        if (page.isNil()) {
            page = seedBuilder.create(url, args);
            if (page.isSeed()) {
                webDb.put(url, page);
                seedIndexer.index(page.getUrl());
            }
            return page;
        }

        page.setOptions(args);
        return inject(page) ? page : WebPage.NIL;
    }

    public boolean inject(WebPage page) {
        Objects.requireNonNull(page);

        boolean success = seedBuilder.makeSeed(page);

        if (success) {
            webDb.put(page.getUrl(), page);
            seedIndexer.index(page.getUrl());
            return true;
        }

        return false;
    }

    public List<WebPage> injectAll(String... configuredUrls) {
        return Stream.of(configuredUrls)
                .map(UrlUtil::splitUrlArgs)
                .map(this::inject)
                .collect(Collectors.toList());
    }

    public List<WebPage> injectAll(Collection<WebPage> pages) {
        List<WebPage> injectedPages = pages.stream()
                .peek(this::inject)
                .filter(WebPage::isSeed)
                .collect(Collectors.toList());

        seedIndexer.indexAll(injectedPages.stream().map(WebPage::getUrl).collect(Collectors.toList()));

        LOG.info("Injected " + injectedPages.size() + " seeds out of " + pages.size() + " pages");

        return injectedPages;
    }

    @Nonnull
    public WebPage unInject(@Nonnull String url) {
        Objects.requireNonNull(url);

        WebPage page = webDb.getOrNil(url);
        if (page.isSeed()) {
            unInject(page);
        }

        return page;
    }

    @Nonnull
    public WebPage unInject(@Nonnull WebPage page) {
        Objects.requireNonNull(page);
        if (!page.isSeed()) {
            return page;
        }

        page.unmarkSeed();
        page.getMarks().remove(Mark.INJECT);
        seedIndexer.remove(page.getUrl());
        webDb.put(page.getUrl(), page);

        return page;
    }

    public List<WebPage> unInjectAll(String... urls) {
        List<WebPage> pages = Stream.of(urls).map(webDb::getOrNil)
                .filter(WebPage::isSeed)
                .peek(this::unInject)
                .collect(Collectors.toList());
        LOG.debug("UnInjected " + pages.size() + " urls");
        seedIndexer.removeAll(pages.stream().map(WebPage::getUrl).collect(Collectors.toList()));
        return pages;
    }

    public Collection<WebPage> unInjectAll(Collection<WebPage> pages) {
        pages.forEach(this::unInject);
        return pages;
    }

    public String report() {
        WebPage seedHome = webDb.getOrNil(SEED_PAGE_1_URL);
        if (seedHome.isNil()) {
            int count = seedHome.getLiveLinks().size();
            return "Total " + count + " seeds in store " + webDb.getSchemaName();
        }

        return "No home page";
    }

    public WebDb getWebDb() {
        return webDb;
    }

    public void commit() {
        webDb.flush();
        seedIndexer.commit();
    }

    @Override
    public void close() {
        if (closed.getAndSet(true)) {
            return;
        }

        commit();
    }

    private WebPage loadOrCreate(String url) {
        WebPage page = webDb.getOrNil(url);
        if (page.isNil()) {
            page = WebPage.newWebPage(url);
        }
        return page;
    }

    /**
     * Command options for {@link InjectComponent}.
     * Expect the list option which specify the seed or seed file, the @ sign is not supported
     */
    private static class InjectOptions extends CommonOptions {
        @Parameter(required = true, description = "<seeds> \nSeed urls. Use {@code @FILE} syntax to read from file.")
        List<String> seeds = new ArrayList<>();
        @Parameter(names = ARG_LIMIT, description = "task limit")
        int limit = -1;

        public InjectOptions(String[] args) {
            super(args);
            // We may read seeds from a file using @ sign, the file parsing should be handled manually
            setExpandAtSign(false);
        }

        @Override
        public Params getParams() {
            return Params.of(
                    ARG_CRAWL_ID, crawlId,
                    ARG_SEEDS, seeds.get(0)
            );
        }
    }
}
