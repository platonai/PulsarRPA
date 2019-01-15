package fun.platonic.pulsar.crawl.inject;

import fun.platonic.pulsar.common.DateTimeUtil;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.config.Params;
import fun.platonic.pulsar.common.config.ReloadableParameterized;
import fun.platonic.pulsar.common.options.CrawlOptions;
import fun.platonic.pulsar.crawl.scoring.ScoringFilters;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.metadata.Mark;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Objects;

import static fun.platonic.pulsar.common.config.PulsarConstants.TCP_IP_STANDARDIZED_TIME;
import static fun.platonic.pulsar.common.config.PulsarConstants.YES_STRING;

/**
 * Created by vincent on 16-9-24.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
public class SeedBuilder implements ReloadableParameterized {

    public static final Logger LOG = LoggerFactory.getLogger(SeedBuilder.class);

    private Instant impreciseNow = Instant.now();
    private ImmutableConfig conf;
    private ScoringFilters scoreFilters;

    public SeedBuilder() {
        this(new ScoringFilters());
    }

    public SeedBuilder(ScoringFilters scoreFilters) {
        this.scoreFilters = scoreFilters;
    }

    public SeedBuilder(ScoringFilters scoreFilters, ImmutableConfig conf) {
        this.scoreFilters = scoreFilters;
        reload(conf);
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
        return Params.of(
                "injectTime", DateTimeUtil.format(impreciseNow)
        );
    }

    @Nonnull
    public WebPage create(Pair<String, String> urlArgs) {
        return create(urlArgs.getKey(), urlArgs.getValue());
    }

    /**
     * @param url  The seed url
     *             A configured url is a string contains the url and arguments.
     * @param args The args
     * @return The created fun.platonic.pulsar.persist.WebPage.
     * If the url is an invalid url or an internal url, return fun.platonic.pulsar.persistWebPage.NIL
     */
    @Nonnull
    public WebPage create(String url, String args) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(args);

        if (url.isEmpty()) {
            return WebPage.NIL;
        }

        WebPage page = WebPage.newWebPage(url);
        return makeSeed(url, args, page) ? page : WebPage.NIL;
    }

    public boolean makeSeed(WebPage page) {
        Objects.requireNonNull(page);
        return makeSeed(page.getUrl(), page.getOptions().toString(), page);
    }

    private boolean makeSeed(String url, String args, WebPage page) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(args);
        Objects.requireNonNull(page);

        if (page.isSeed() || page.isInternal()) {
            return false;
        }

        CrawlOptions options = CrawlOptions.parse(args, conf);

        page.setDistance(0);
        if (page.getCreateTime().isBefore(TCP_IP_STANDARDIZED_TIME)) {
            page.setCreateTime(impreciseNow);
        }
        page.markSeed();

        page.setScore(options.getScore());
        scoreFilters.injectedScore(page);

        page.setFetchTime(impreciseNow);
        page.setFetchInterval(options.getFetchInterval());
        page.setFetchPriority(options.getFetchPriority());

        page.getMarks().put(Mark.INJECT, YES_STRING);

        return true;
    }
}
