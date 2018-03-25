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

package fun.platonic.pulsar.crawl.schedule;

import fun.platonic.pulsar.common.MetricsSystem;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.config.Params;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fun.platonic.pulsar.persist.CrawlStatus;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.gora.db.WebDb;
import fun.platonic.pulsar.persist.metadata.Mark;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static fun.platonic.pulsar.common.config.CapabilityTypes.FETCH_DEFAULT_INTERVAL;
import static fun.platonic.pulsar.common.config.CapabilityTypes.FETCH_MAX_INTERVAL;

/**
 * This class provides common methods for implementations of
 * {@link FetchSchedule}.
 *
 * @author Andrzej Bialecki
 */
public abstract class AbstractFetchSchedule implements FetchSchedule {
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractFetchSchedule.class);

    protected ImmutableConfig conf;
    protected MetricsSystem metricsSystem;
    protected Duration defaultInterval;
    protected Duration maxInterval;

    private Instant impreciseNow = Instant.now();

    @Override
    public void reload(ImmutableConfig conf) {
        this.conf = conf;
        defaultInterval = conf.getDuration(FETCH_DEFAULT_INTERVAL, Duration.ofDays(30));
        maxInterval = conf.getDuration(FETCH_MAX_INTERVAL, Duration.ofDays(90));

        if (metricsSystem == null) {
            metricsSystem = new MetricsSystem(new WebDb(conf), conf);
        }
    }

    @Override
    public ImmutableConfig getConf() {
        return conf;
    }

    @Override
    public Params getParams() {
        return Params.of(
                "defaultInterval", defaultInterval,
                "maxInterval", maxInterval
        );
    }

    public MetricsSystem getPulsarMetrics() {
        return metricsSystem;
    }

    public void setPulsarMetrics(MetricsSystem metricsSystem) {
        this.metricsSystem = metricsSystem;
    }

    /**
     * Initialize fetch schedule related data. Implementations should at least set
     * the <code>fetchTime</code> and <code>fetchInterval</code>. The default
     * implementation sets the <code>fetchTime</code> to now, using the default
     * <code>fetchInterval</code>.
     *
     * @param page
     */
    @Override
    public void initializeSchedule(WebPage page) {
        page.setFetchTime(impreciseNow);
        page.setFetchInterval(defaultInterval);
        page.setFetchRetries(0);
        page.setCrawlStatus(CrawlStatus.STATUS_UNFETCHED);
    }

    /**
     * Sets the <code>fetchInterval</code> and <code>fetchTime</code> on a
     * successfully fetched page. NOTE: this implementation resets the retry
     * counter - extending classes should call super.setFetchSchedule() to
     * preserve this behavior.
     */
    @Override
    public void setFetchSchedule(WebPage page, Instant prevFetchTime,
                                 Instant prevModifiedTime, Instant fetchTime, Instant modifiedTime, int state) {
        page.setFetchRetries(0);
    }

    /**
     * This method specifies how to schedule refetching of pages marked as GONE.
     * Default implementation increases fetchInterval by 50% but the value may
     * never exceed <code>maxInterval</code>.
     *
     * @param page
     * @return adjusted page information, including all original information.
     * NOTE: this may be a different instance than
     */
    @Override
    public void setPageGoneSchedule(WebPage page, Instant prevFetchTime,
                                    Instant prevModifiedTime, Instant fetchTime) {
        long prevInterval = page.getFetchInterval(TimeUnit.SECONDS);
        float newInterval = prevInterval;

        // no page is truly GONE ... just increase the interval by 50%
        // and try much later.
        if (newInterval < maxInterval.getSeconds()) {
            newInterval = prevInterval * 1.5f;
        } else {
            newInterval = maxInterval.getSeconds() * 0.9f;
        }

        page.setFetchInterval(newInterval);
        page.setFetchTime(fetchTime.plus(page.getFetchInterval()));
    }

    /**
     * This method adjusts the fetch schedule if fetching needs to be re-tried due
     * to transient errors. The default implementation sets the next fetch time 1
     * day in the future and increases the retry counter.
     *
     * @param page             WebPage to retry
     * @param prevFetchTime    previous fetch time
     * @param prevModifiedTime previous modified time
     * @param fetchTime        current fetch time
     */
    @Override
    public void setPageRetrySchedule(WebPage page, Instant prevFetchTime, Instant prevModifiedTime, Instant fetchTime) {
        page.setFetchTime(fetchTime.plus(1, ChronoUnit.DAYS));
        page.setFetchRetries(page.getFetchRetries() + 1);
    }

    /**
     * This method return the last fetch time of the WebPage
     *
     * @return the date as a long.
     */
    @Override
    public Instant calculateLastFetchTime(WebPage page) {
        return page.getFetchTime().minus(page.getFetchInterval());
    }

    /**
     * This method provides information whether the page is suitable for selection
     * in the current fetchlist. NOTE: a true return value does not guarantee that
     * the page will be fetched, it just allows it to be included in the further
     * selection process based on scores. The default implementation checks
     * <code>fetchTime</code>, if it is higher than the
     *
     * @param page    Web page to fetch
     * @param curTime reference time (usually set to the time when the fetchlist
     *                generation process was started).
     * @return true, if the page should be considered for inclusion in the current
     * fetchlist, otherwise false.
     */
    @Override
    public boolean shouldFetch(WebPage page, Instant curTime) {
        if (page.hasMark(Mark.INACTIVE)) {
            return false;
        }

        // Pages are never truly GONE - we have to check them from time to time.
        // pages with too long fetchInterval are adjusted so that they fit within
        // maximum fetchInterval (batch retention period).
        Instant fetchTime = page.getFetchTime();
        if (curTime.plus(maxInterval).isBefore(fetchTime)) {
            if (page.getFetchInterval().compareTo(maxInterval) > 0) {
                page.setFetchInterval(maxInterval.getSeconds() * 0.9f);
            }
            page.setFetchTime(curTime);
        }

        return fetchTime.isBefore(curTime);
    }

    /**
     * This method resets fetchTime, fetchInterval, modifiedTime,
     * retriesSinceFetch and page text, so that it forces refetching.
     *
     * @param page
     * @param asap if true, force refetch as soon as possible - this sets the
     *             fetchTime to now. If false, force refetch whenever the next fetch
     *             time is set.
     */
    @Override
    public void forceRefetch(WebPage page, boolean asap) {
        if (page.hasMark(Mark.INACTIVE)) {
            return;
        }

        // reduce fetchInterval so that it fits within the max value
        if (page.getFetchInterval().compareTo(maxInterval) > 0) {
            page.setFetchInterval(maxInterval.getSeconds() * 0.9f);
        }

        page.setCrawlStatus(CrawlStatus.STATUS_UNFETCHED);
        page.setFetchRetries(0);
        page.setSignature("".getBytes());
        page.setModifiedTime(Instant.EPOCH);
        if (asap) {
            page.setFetchTime(Instant.now());
        }
    }

    protected void updateRefetchTime(WebPage page, Duration interval, Instant fetchTime, Instant prevModifiedTime, Instant modifiedTime) {
        page.setFetchInterval(interval);
        page.setFetchTime(fetchTime.plus(interval));
        page.setPrevModifiedTime(prevModifiedTime);
        page.setModifiedTime(modifiedTime);
    }
}
