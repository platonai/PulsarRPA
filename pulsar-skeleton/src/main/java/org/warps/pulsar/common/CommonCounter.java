package org.warps.pulsar.common;

/**
 * Created by vincent on 17-4-5.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
public enum CommonCounter {
    mRows,
    rRows,

    mDetail,
    rDetail,

    mPersist,
    rPersist,

    mDepth0, mDepth1, mDepth2, mDepth3, mDepthN,
    rDepth0, rDepth1, rDepth2, rDepth3, rDepthN,

    mDay0, mDay1, mDay2, mDay3, mDay4, mDay5, mDay6, mDay7, mDayN,
    rDay0, rDay1, rDay2, rDay3, rDay4, rDay5, rDay6, rDay7, rDayN,

    mInlinks,
    rInlinks,

    mLinks,
    rLinks,

    errors,

    stFetched,
    stRedirTemp,
    stRedirPerm,
    stNotModified,
    stRetry,
    stUnfetched,
    stGone;

    static {
        MetricsCounters.register(CommonCounter.class);
    }
}
