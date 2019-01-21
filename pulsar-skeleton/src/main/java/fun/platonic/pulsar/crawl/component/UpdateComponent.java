/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fun.platonic.pulsar.crawl.component;

import fun.platonic.pulsar.common.MetricsCounters;
import fun.platonic.pulsar.common.MetricsSystem;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.config.Params;
import fun.platonic.pulsar.common.config.ReloadableParameterized;
import fun.platonic.pulsar.crawl.filter.CrawlFilter;
import fun.platonic.pulsar.crawl.schedule.DefaultFetchSchedule;
import fun.platonic.pulsar.crawl.schedule.FetchSchedule;
import fun.platonic.pulsar.crawl.scoring.ScoringFilters;
import fun.platonic.pulsar.crawl.signature.SignatureComparator;
import fun.platonic.pulsar.persist.CrawlStatus;
import fun.platonic.pulsar.persist.PageCounters;
import fun.platonic.pulsar.persist.WebDb;
import fun.platonic.pulsar.persist.WebPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

import static fun.platonic.pulsar.common.config.CapabilityTypes.FETCH_MAX_INTERVAL;
import static fun.platonic.pulsar.common.config.CapabilityTypes.FETCH_MAX_RETRY;
import static fun.platonic.pulsar.common.config.PulsarConstants.TCP_IP_STANDARDIZED_TIME;

/**
 * Parser checker, useful for testing parser. It also accurately reports
 * possible fetching and parsing failures and presents protocol status signals
 * to aid debugging. The tool enables us to retrieve the following data from any
 */
@Component
public class UpdateComponent implements ReloadableParameterized {

    public static final Logger LOG = LoggerFactory.getLogger(UpdateComponent.class);

    static {
        MetricsCounters.register(Counter.class);
    }

    private ImmutableConfig conf;
    private int fetchRetryMax;
    private Duration maxFetchInterval;
    private WebDb webDb;
    private MetricsSystem metricsSystem;
    private FetchSchedule fetchSchedule;
    private ScoringFilters scoringFilters;
    private MetricsCounters metricsCounters;

    public UpdateComponent(ImmutableConfig conf) throws Exception {
        this.conf = conf;

        webDb = new WebDb(conf);
        metricsSystem = new MetricsSystem(webDb, conf);
        fetchSchedule = new DefaultFetchSchedule(conf);
        scoringFilters = new ScoringFilters(conf);
        this.metricsCounters = new MetricsCounters();

        reload(conf);
    }

    public UpdateComponent(WebDb webDb,
                           FetchSchedule fetchSchedule,
                           MetricsSystem metricsSystem,
                           ScoringFilters scoringFilters,
                           ImmutableConfig conf) {
        this.conf = conf;

        this.webDb = webDb;
        this.fetchSchedule = fetchSchedule;
        this.metricsSystem = metricsSystem;
        this.scoringFilters = scoringFilters;
        this.metricsCounters = new MetricsCounters();

        reload(conf);
    }

    @Override
    public Params getParams() {
        return Params.of(
                "className", this.getClass().getSimpleName(),
                "fetchRetryMax", fetchRetryMax,
                "maxFetchInterval", maxFetchInterval,
                "fetchSchedule", fetchSchedule.getClass().getSimpleName()
        );
    }

    @Override
    public ImmutableConfig getConf() {
        return conf;
    }

    @Override
    public void reload(ImmutableConfig conf) {
        fetchRetryMax = conf.getInt(FETCH_MAX_RETRY, 3);
        maxFetchInterval = conf.getDuration(FETCH_MAX_INTERVAL, Duration.ofDays(365));

        // LOG.info(getParams().format());
    }

    public WebDb getWebDb() {
        return webDb;
    }

    public void setWebDb(WebDb webDb) {
        this.webDb = webDb;
    }

    public MetricsSystem getPulsarMetrics() {
        return metricsSystem;
    }

    public void setPulsarMetrics(MetricsSystem metricsSystem) {
        this.metricsSystem = metricsSystem;
    }

    public FetchSchedule getFetchSchedule() {
        return fetchSchedule;
    }

    public void setFetchSchedule(FetchSchedule fetchSchedule) {
        this.fetchSchedule = fetchSchedule;
    }

    public ScoringFilters getScoringFilters() {
        return scoringFilters;
    }

    public void setScoringFilters(ScoringFilters scoringFilters) {
        this.scoringFilters = scoringFilters;
    }

    public void updateByOutgoingPage(WebPage page, WebPage outgoingPage) {
        PageCounters pageCounters = page.getPageCounters();

        pageCounters.increase(PageCounters.Ref.page);
        page.updateRefContentPublishTime(outgoingPage.getContentPublishTime());

        if (outgoingPage.getPageCategory().isDetail() || CrawlFilter.sniffPageCategory(outgoingPage.getUrl()).isDetail()) {
            pageCounters.increase(PageCounters.Ref.ch, outgoingPage.getContentTextLen());
            pageCounters.increase(PageCounters.Ref.article);
        }

        PageCounters outPageCounters = outgoingPage.getPageCounters();
        int missingFields = outPageCounters.get(PageCounters.Self.missingFields);
        int brokenSubEntity = outPageCounters.get(PageCounters.Self.brokenSubEntity);
        pageCounters.increase(PageCounters.Ref.missingFields, missingFields);
        pageCounters.increase(PageCounters.Ref.brokenEntity, missingFields > 0 ? 1 : 0);
        pageCounters.increase(PageCounters.Ref.brokenSubEntity, brokenSubEntity);

        if (outgoingPage.getProtocolStatus().isFailed()) {
            page.getDeadLinks().add(outgoingPage.getUrl());

            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to fetch out page: " + outgoingPage.getUrl() + " <= " + page.getUrl());
            }
        }

        scoringFilters.updateContentScore(page);
    }

    public void updateByOutgoingPages(WebPage page, Collection<WebPage> outgoingPages) {
        PageCounters lastPageCounters = page.getPageCounters().clone();

        outgoingPages.forEach(outgoingPage -> updateByOutgoingPage(page, outgoingPage));

        updatePageCounters(lastPageCounters, page.getPageCounters(), page);
    }

    public void updatePageCounters(PageCounters lastPageCounters, PageCounters pageCounters, WebPage page) {
        int lastMissingFields = lastPageCounters.get(PageCounters.Ref.missingFields);
        int lastBrokenEntity = lastPageCounters.get(PageCounters.Ref.brokenEntity);
        int lastBrokenSubEntity = lastPageCounters.get(PageCounters.Ref.brokenSubEntity);

        int missingFieldsLastRound = pageCounters.get(PageCounters.Ref.missingFields) - lastMissingFields;
        int brokenEntityLastRound = pageCounters.get(PageCounters.Ref.brokenEntity) - lastBrokenEntity;
        int brokenSubEntityLastRound = pageCounters.get(PageCounters.Ref.brokenSubEntity) - lastBrokenSubEntity;

        pageCounters.set(PageCounters.Ref.missingFieldsLastRound, missingFieldsLastRound);
        pageCounters.set(PageCounters.Ref.brokenEntityLastRound, brokenEntityLastRound);
        pageCounters.set(PageCounters.Ref.brokenSubEntityLastRound, brokenSubEntityLastRound);

        if (missingFieldsLastRound != 0 || brokenEntityLastRound != 0 || brokenSubEntityLastRound != 0) {
            String message = Params.of(
                    "missingFields", missingFieldsLastRound,
                    "brokenEntity", brokenEntityLastRound,
                    "brokenSubEntity", brokenSubEntityLastRound
            ).formatAsLine();

            metricsSystem.reportBrokenEntity(page.getUrl(), message);
            LOG.warn(message);
        }
    }

    /**
     * A simple update procedure
     */
    public void updateByIncomingPages(Collection<WebPage> incomingPages, WebPage page) {
        int smallestDepth = page.getDistance();
        WebPage shallowestPage = null;

        for (WebPage incomingPage : incomingPages) {
            // LOG.debug(incomingPage.url() + " -> " + page.url());
            if (incomingPage.getDistance() + 1 < smallestDepth) {
                smallestDepth = incomingPage.getDistance() + 1;
                shallowestPage = incomingPage;
            }
        }

        if (shallowestPage != null) {
            page.setReferrer(shallowestPage.getUrl());
            // TODO: Not the best options
            page.setOptions(shallowestPage.getOptions());
            page.setDistance(shallowestPage.getDistance() + 1);
        }
    }

    public void updateFetchSchedule(WebPage page) {
        if (page.getMarks().isInactive()) {
            return;
        }

        CrawlStatus crawlStatus = page.getCrawlStatus();

        switch (crawlStatus.getCode()) {
            case CrawlStatus.FETCHED: // successful fetch
            case CrawlStatus.REDIR_TEMP: // successful fetch, redirected
            case CrawlStatus.REDIR_PERM:
            case CrawlStatus.NOTMODIFIED: // successful fetch, not modified
                int modified = FetchSchedule.STATUS_UNKNOWN;
                if (crawlStatus.getCode() == CrawlStatus.NOTMODIFIED) {
                    modified = FetchSchedule.STATUS_NOTMODIFIED;
                }

                ByteBuffer prevSig = page.getPrevSignature();
                ByteBuffer signature = page.getSignature();
                if (prevSig != null && signature != null) {
                    if (SignatureComparator.compare(prevSig, signature) != 0) {
                        modified = FetchSchedule.STATUS_MODIFIED;
                    } else {
                        modified = FetchSchedule.STATUS_NOTMODIFIED;
                    }
                }

                Instant prevFetchTime = page.getPrevFetchTime();
                Instant fetchTime = page.getFetchTime();

                Instant prevModifiedTime = page.getPrevModifiedTime();
                Instant modifiedTime = page.getModifiedTime();
                Instant newModifiedTime = page.sniffModifiedTime();
                if (newModifiedTime.isAfter(modifiedTime)) {
                    prevModifiedTime = modifiedTime;
                    modifiedTime = newModifiedTime;
                }

                fetchSchedule.setFetchSchedule(page, prevFetchTime, prevModifiedTime, fetchTime, modifiedTime, modified);

                Duration fetchInterval = page.getFetchInterval();
                if (fetchInterval.compareTo(maxFetchInterval) > 0 && !page.getMarks().isInactive()) {
                    LOG.info("Force refetch page " + page.getUrl() + ", fetch interval : " + fetchInterval);
                    fetchSchedule.forceRefetch(page, false);
                }

                if (modifiedTime.isBefore(TCP_IP_STANDARDIZED_TIME)) {
                    metricsCounters.increase(Counter.rBadModTime);
                    metricsSystem.reportBadModifiedTime(Params.of(
                            "PFT", prevFetchTime, "FT", fetchTime,
                            "PMT", prevModifiedTime, "MT", modifiedTime,
                            "HMT", page.getHeaders().getLastModified(),
                            "U", page.getUrl()
                    ).formatAsLine());
                }

                break;
            case CrawlStatus.RETRY:
                fetchSchedule.setPageRetrySchedule(page, Instant.EPOCH, page.getPrevModifiedTime(), page.getFetchTime());
                if (page.getFetchRetries() < fetchRetryMax) {
                    page.setCrawlStatus(CrawlStatus.UNFETCHED);
                } else {
                    page.setCrawlStatus(CrawlStatus.GONE);
                }
                break;
            case CrawlStatus.GONE:
                fetchSchedule.setPageGoneSchedule(page, Instant.EPOCH, page.getPrevModifiedTime(), page.getFetchTime());
                break;
        }
    }

    public enum Counter {
        rCreated, rNewDetail, rPassed, rLoaded,
        rNotExist,
        rDepthUp, rUpdated, rTotalUpdates,
        rBadModTime
    }
}
