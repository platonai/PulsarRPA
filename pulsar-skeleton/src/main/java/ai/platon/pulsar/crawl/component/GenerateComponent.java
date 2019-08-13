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

package ai.platon.pulsar.crawl.component;

import ai.platon.pulsar.common.*;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.common.config.ReloadableParameterized;
import ai.platon.pulsar.crawl.filter.CrawlFilter;
import ai.platon.pulsar.crawl.filter.CrawlFilters;
import ai.platon.pulsar.crawl.filter.UrlFilters;
import ai.platon.pulsar.crawl.filter.UrlNormalizers;
import ai.platon.pulsar.crawl.schedule.FetchSchedule;
import ai.platon.pulsar.crawl.scoring.ScoringFilters;
import ai.platon.pulsar.persist.WebDb;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.metadata.FetchMode;
import ai.platon.pulsar.persist.metadata.Mark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

import static ai.platon.pulsar.common.PulsarPaths.PATH_BANNED_URLS;
import static ai.platon.pulsar.common.PulsarPaths.PATH_UNREACHABLE_HOSTS;
import static ai.platon.pulsar.common.config.CapabilityTypes.*;
import static ai.platon.pulsar.common.config.PulsarConstants.ALL_BATCHES;
import static ai.platon.pulsar.common.config.PulsarConstants.DISTANCE_INFINITE;

/**
 * Parser checker, useful for testing parser. It also accurately reports
 * possible fetching and parsing failures and presents protocol status signals
 * to aid debugging. The tool enables us to retrieve the following data from any
 */
@Component
public class GenerateComponent implements ReloadableParameterized {

    public static final Logger LOG = LoggerFactory.getLogger(GenerateComponent.class);

    static {
        MetricsCounters.register(Counter.class);
    }

    private final Set<String> unreachableHosts = new HashSet<>();
    private final Set<String> bannedUrls = new HashSet<>();
    private ImmutableConfig conf;
    private String crawlId = "";
    private String batchId = ALL_BATCHES;
    private FetchMode fetchMode = FetchMode.NATIVE;
    private URLUtil.GroupMode groupMode = URLUtil.GroupMode.BY_HOST;
    private boolean filter = true;
    private boolean normalise = true;
    private Instant pseudoCurrTime = Instant.now();
    private int maxDistance = DISTANCE_INFINITE;
    private boolean reGenerate = false;
    private boolean reGenerateSeeds = false;
    private int topN = -1;
    private long lastGeneratedRows = -1;
    private long lowGeneratedRows = -1;
    private float lowGeneratedRowsRate = 0.8f;
    private Instant startTime = Instant.now();
    private String[] keyRange = {null, null};
    private WebDb webDb;
    private UrlFilters urlFilters;
    private UrlNormalizers urlNormalizers;
    private ScoringFilters scoringFilters;
    private CrawlFilters crawlFilters;
    private FetchSchedule fetchSchedule;
    private MetricsSystem metricsSystem;
    private MetricsCounters metricsCounters = new MetricsCounters();

    public GenerateComponent(CrawlFilters crawlFilters, ImmutableConfig conf) {
        this.conf = conf;
        this.crawlFilters = crawlFilters;

        reload(conf);
    }

    private static void increaseMDaysLater(long days, MetricsCounters metricsCounters) {
        Counter counter;
        if (days == 0) {
            counter = Counter.mLater0;
        } else if (days == 1) {
            counter = Counter.mLater1;
        } else if (days == 2) {
            counter = Counter.mLater2;
        } else if (days == 3) {
            counter = Counter.mLater3;
        } else if (days == 4) {
            counter = Counter.mLater4;
        } else if (days == 5) {
            counter = Counter.mLater5;
        } else if (days == 6) {
            counter = Counter.mLater6;
        } else if (days == 7) {
            counter = Counter.mLater7;
        } else {
            counter = Counter.mLaterN;
        }

        metricsCounters.increase(counter);
        metricsCounters.increase(Counter.mLater);
    }

    @Override
    public Params getParams() {
        return Params.of("className", this.getClass().getSimpleName())
                .merge(Params.of(
                        "crawlId", crawlId,
                        "fetchMode", fetchMode,
                        "batchId", batchId,
                        "groupMode", groupMode,
                        "filter", filter,
                        "normalise", normalise,
                        "maxDistance", maxDistance,
                        "reGenerate", reGenerate,
                        "reGenerateSeeds", reGenerateSeeds,
                        "pseudoCurrTime", DateTimeUtil.format(pseudoCurrTime.truncatedTo(ChronoUnit.SECONDS)),
                        "topN", topN,
                        "lastGeneratedRows", lastGeneratedRows,
                        "lowGeneratedRows", lowGeneratedRows,
                        "lowGeneratedRowsRate", lowGeneratedRowsRate,
                        "fetchSchedule", fetchSchedule.getClass().getSimpleName(),
                        "scoringFilters", scoringFilters,
                        "urlNormalizers", urlNormalizers,
                        "urlFilters", urlFilters,
                        "crawlFilters", crawlFilters,
                        "keyRange", keyRange[0] + " - " + keyRange[1],
                        "unreachableHostsPath", PATH_UNREACHABLE_HOSTS,
                        "unreachableHosts", unreachableHosts.size()
                ))
                .merge(scoringFilters.getParams());
    }

    @Override
    public ImmutableConfig getConf() {
        return conf;
    }

    @Override
    public void reload(ImmutableConfig conf) {
        crawlId = conf.get(STORAGE_CRAWL_ID);
        batchId = conf.get(BATCH_ID, ALL_BATCHES);
        fetchMode = conf.getEnum(FETCH_MODE, FetchMode.NATIVE);
        groupMode = conf.getEnum(FETCH_QUEUE_MODE, URLUtil.GroupMode.BY_HOST);
        reGenerate = conf.getBoolean(GENERATE_REGENERATE, false);
        reGenerateSeeds = conf.getBoolean(GENERATE_REGENERATE_SEEDS, false);
        filter = conf.getBoolean(GENERATE_FILTER, true);
        normalise = conf.getBoolean(GENERATE_NORMALISE, true);
        maxDistance = conf.getUint(CRAWL_MAX_DISTANCE, DISTANCE_INFINITE);
        pseudoCurrTime = conf.getInstant(GENERATE_CUR_TIME, startTime);
        topN = conf.getInt(GENERATE_TOP_N, -1);
        lastGeneratedRows = conf.getInt(GENERATE_LAST_GENERATED_ROWS, -1);
        lowGeneratedRows = (int) (lowGeneratedRowsRate * topN);

        keyRange = crawlFilters.getMaxReversedKeyRange();

        // TODO : move to a filter
        bannedUrls.addAll(FSUtils.readAllLinesSilent(PATH_BANNED_URLS, conf));
        unreachableHosts.addAll(LocalFSUtils.readAllLinesSilent(PATH_UNREACHABLE_HOSTS));
    }

    public WebDb getWebDb() {
        return webDb;
    }

    public void setWebDb(WebDb webDb) {
        this.webDb = webDb;
    }

    public UrlFilters getUrlFilters() {
        return urlFilters;
    }

    public void setUrlFilters(UrlFilters urlFilters) {
        this.urlFilters = urlFilters;
    }

    public UrlNormalizers getUrlNormalizers() {
        return urlNormalizers;
    }

    public void setUrlNormalizers(UrlNormalizers urlNormalizers) {
        this.urlNormalizers = urlNormalizers;
    }

    public ScoringFilters getScoringFilters() {
        return scoringFilters;
    }

    public void setScoringFilters(ScoringFilters scoringFilters) {
        this.scoringFilters = scoringFilters;
    }

    public CrawlFilters getCrawlFilters() {
        return crawlFilters;
    }

    public void setCrawlFilters(CrawlFilters crawlFilters) {
        this.crawlFilters = crawlFilters;
    }

    public FetchSchedule getFetchSchedule() {
        return fetchSchedule;
    }

    public void setFetchSchedule(FetchSchedule fetchSchedule) {
        this.fetchSchedule = fetchSchedule;
    }

    public MetricsSystem getPulsarMetrics() {
        return metricsSystem;
    }

    public void setPulsarMetrics(MetricsSystem metricsSystem) {
        this.metricsSystem = metricsSystem;
    }

    /**
     * TODO : We may move some filters to hbase query filters directly
     * TODO : Move to CrawlFilter
     */
    public boolean shouldFetch(String url, String reversedUrl, WebPage page) {
        if (reGenerateSeeds && page.isSeed()) {
            return true;
        }

        int distance = page.getDistance();

        if (!checkFetchSchedule(page)) {
            return false;
        }

        if (!checkHost(url)) {
            return false;
        }

        if (bannedUrls.contains(url)) {
            metricsCounters.increase(Counter.mBanned);
            return false;
        }

        if (unreachableHosts.contains(URLUtil.getHost(page.getUrl(), groupMode))) {
            metricsCounters.increase(Counter.mHostGone);
            return false;
        }

        if (page.hasMark(Mark.GENERATE)) {
            metricsCounters.increase(Counter.mGenerated);

            /*
             * Fetch entries are generated, empty webpage entries are created in the database(HBase)
             * case 1. another fetcher job is fetching the generated batch. In this case, we should not generate it.
             * case 2. another fetcher job handled the generated batch, but failed, which means the pages are not fetched.
             *
             * There are three ways to fetch pages that are generated but not fetched nor fetching.
             * 1. Restart a text with ignoreGenerated set to be false
             * 2. Resume a FetchJob with resume set to be true
             * */
            if (!reGenerate) {
                long days = Duration.between(page.getGenerateTime(), startTime).toDays();
                if (days == 1) {
                    // may be used by other jobs, or not fetched correctly
                    return false;
                } else if (days <= 3) {
                    // force re-generate
                } else {
                    // ignore pages too old
                    return false;
                }
            } else {
                // re-generate
            }
        } // if

        int distanceBias = 0;
        // Filter on distance
        if (distance > maxDistance + distanceBias) {
            metricsCounters.increase(Counter.mTooDeep);
            return false;
        }

        // TODO : Url range filtering should be applied to HBase query filter
        // key before start key
        if (!CrawlFilter.keyGreaterEqual(reversedUrl, keyRange[0])) {
            metricsCounters.increase(Counter.mBeforeStart);
            return false;
        }

        // key after end key, finish the mapper
        if (!CrawlFilter.keyLessEqual(reversedUrl, keyRange[1])) {
//      stop("Complete mapper, reason : hit end key " + reversedUrl
//          + ", upper bound : " + keyRange[1]
//          + ", diff : " + reversedUrl.compareTo(keyRange[1]));
            return false;
        }

        // key not fall in key ranges
        if (!crawlFilters.testKeyRangeSatisfied(reversedUrl)) {
            metricsCounters.increase(Counter.mNotInRange);
            return false;
        }

        // If filtering is on don't generate URLs that don't pass UrlFilters
        if (normalise) {
            url = urlNormalizers.normalize(url, UrlNormalizers.SCOPE_GENERATE_HOST_COUNT);
        }

        if (url == null) {
            metricsCounters.increase(Counter.mNotNormal);
            return false;
        }

        if (filter && urlFilters.filter(url) == null) {
            metricsCounters.increase(Counter.mUrlFiltered);
            return false;
        }

        return true;
    }

    /**
     * Fetch schedule, timing filter
     */
    private boolean checkFetchSchedule(WebPage page) {
        if (fetchSchedule.shouldFetch(page, pseudoCurrTime)) {
            return true;
        }

        // INACTIVE mark is already filtered in HBase query phrase, double check here for diagnoses
        if (page.hasMark(Mark.INACTIVE)) {
            metricsCounters.increase(Counter.mInactive);
        }

        Instant fetchTime = page.getFetchTime();
        long hours = ChronoUnit.HOURS.between(pseudoCurrTime, fetchTime);
        if (hours <= 6 && 0 < lastGeneratedRows && lastGeneratedRows < lowGeneratedRows) {
            // There are enough resource to do tasks ahead of time
            // TODO : we can expend maxDistance to gain a bigger web graph if the machines are idle
            long fetchInterval = ChronoUnit.HOURS.between(page.getPrevFetchTime(), pseudoCurrTime);
            if (fetchInterval > 6) {
                metricsCounters.increase(Counter.mAhead);
                if (page.isSeed()) {
                    metricsCounters.increase(Counter.mSeedAhead);
                }
                return true;
            }
        }

        if (hours <= 30 * 24) {
            increaseMDaysLater(hours / 24, metricsCounters);

            if (page.isSeed()) {
                metricsCounters.increase(Counter.mSeedLater);
                metricsSystem.debugFetchLaterSeeds(page);
            }
        }

        return false;
    }

    // Check Host
    private boolean checkHost(String url) {
        String host = URLUtil.getHost(url, groupMode);

        if (host.isEmpty()) {
            metricsCounters.increase(Counter.mUrlMalformed);
            return false;
        }

        if (unreachableHosts.contains(host)) {
            metricsCounters.increase(Counter.mHostGone);
            return false;
        }

        return true;
    }

    public enum Counter {
        mSeeds, mBanned, mHostGone, lastGenerated,
        mBeforeStart, mNotInRange,
        mUrlMalformed, mNotNormal, mUrlFiltered, mUrlOldDate,
        tieba, bbs, news, blog,
        mGenerated, mTooDeep,
        mLater, mLater0, mLater1, mLater2, mLater3, mLater4, mLater5, mLater6, mLater7, mLaterN,
        mAhead, mSeedAhead, mSeedLater, mInactive
    }
}
