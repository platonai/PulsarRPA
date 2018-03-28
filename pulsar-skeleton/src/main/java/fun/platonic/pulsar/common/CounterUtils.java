package fun.platonic.pulsar.common;

import fun.platonic.pulsar.persist.CrawlStatus;

/**
 * Created by vincent on 17-4-5.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class CounterUtils {

    public static void increaseMDays(long days, MetricsCounters metricsCounters) {
        CommonCounter counter;
        if (days == 0) {
            counter = CommonCounter.mDay0;
        } else if (days == 1) {
            counter = CommonCounter.mDay1;
        } else if (days == 2) {
            counter = CommonCounter.mDay2;
        } else if (days == 3) {
            counter = CommonCounter.mDay3;
        } else if (days == 4) {
            counter = CommonCounter.mDay4;
        } else if (days == 5) {
            counter = CommonCounter.mDay5;
        } else if (days == 6) {
            counter = CommonCounter.mDay6;
        } else if (days == 7) {
            counter = CommonCounter.mDay7;
        } else {
            counter = CommonCounter.mDayN;
        }

        metricsCounters.increase(counter);
    }

    public static void increaseRDays(long days, MetricsCounters metricsCounters) {
        CommonCounter counter;
        if (days == 0) {
            counter = CommonCounter.rDay0;
        } else if (days == 1) {
            counter = CommonCounter.rDay1;
        } else if (days == 2) {
            counter = CommonCounter.rDay2;
        } else if (days == 3) {
            counter = CommonCounter.rDay3;
        } else if (days == 4) {
            counter = CommonCounter.rDay4;
        } else if (days == 5) {
            counter = CommonCounter.rDay5;
        } else if (days == 6) {
            counter = CommonCounter.rDay6;
        } else if (days == 7) {
            counter = CommonCounter.rDay7;
        } else {
            counter = CommonCounter.rDayN;
        }

        metricsCounters.increase(counter);
    }

    public static void increaseMDepth(int depth, MetricsCounters metricsCounters) {
        CommonCounter counter;
        if (depth == 0) {
            counter = CommonCounter.mDepth0;
        } else if (depth == 1) {
            counter = CommonCounter.mDepth1;
        } else if (depth == 2) {
            counter = CommonCounter.mDepth2;
        } else if (depth == 3) {
            counter = CommonCounter.mDepth3;
        } else {
            counter = CommonCounter.mDepthN;
        }

        metricsCounters.increase(counter);
    }

    public static void increaseRDepth(int depth, MetricsCounters counter) {
        if (depth == 0) {
            counter.increase(CommonCounter.rDepth0);
        } else if (depth == 1) {
            counter.increase(CommonCounter.rDepth1);
        } else if (depth == 2) {
            counter.increase(CommonCounter.rDepth2);
        } else if (depth == 3) {
            counter.increase(CommonCounter.rDepth3);
        } else {
            counter.increase(CommonCounter.rDepthN);
        }
    }

    public static void updateStatusCounter(CrawlStatus crawlStatus, MetricsCounters counter) {
        switch (crawlStatus.getCode()) {
            case CrawlStatus.FETCHED:
                counter.increase(CommonCounter.stFetched);
                break;
            case CrawlStatus.REDIR_TEMP:
                counter.increase(CommonCounter.stRedirTemp);
                break;
            case CrawlStatus.REDIR_PERM:
                counter.increase(CommonCounter.stRedirPerm);
                break;
            case CrawlStatus.NOTMODIFIED:
                counter.increase(CommonCounter.stNotModified);
                break;
            case CrawlStatus.RETRY:
                counter.increase(CommonCounter.stRetry);
                break;
            case CrawlStatus.UNFETCHED:
                counter.increase(CommonCounter.stUnfetched);
                break;
            case CrawlStatus.GONE:
                counter.increase(CommonCounter.stGone);
                break;
            default:
                break;
        }
    }
}
