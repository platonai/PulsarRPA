package ai.platon.pulsar.skeleton.common.metrics

/**
 * Created by vincent on 17-4-5.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 */
enum class CommonCounter {
    mRows, rRows, mDetail, rDetail, mPersist, mDepth0, mDepth1, mDepth2, mDepth3, mDepthN,
    mDay0, mDay1, mDay2, mDay3, mDay4, mDay5, mDay6, mDay7, mDayN,
    mInlinks, rInlinks, mLinks,

    rDepth0, rDepth1, rDepth2, rDepth3, rDepthN, rPersist,
    rDay0, rDay1, rDay2, rDay3, rDay4, rDay5, rDay6, rDay7, rDayN,
    rLinks, errors,
    stFetched, stRedirTemp, stRedirPerm, stNotModified, stRetry, stUnfetched, stGone;

    companion object {
        init { MetricsSystem.reg.register(CommonCounter::class.java) }
    }
}
