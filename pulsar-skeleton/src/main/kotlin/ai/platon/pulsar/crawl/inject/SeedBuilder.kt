package ai.platon.pulsar.crawl.inject

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.options.CrawlOptions.Companion.parse
import ai.platon.pulsar.crawl.scoring.ScoringFilters
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.Mark
import org.apache.commons.lang3.tuple.Pair
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Created by vincent on 16-9-24.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class SeedBuilder(
        private val scoreFilters: ScoringFilters,
        private val conf: ImmutableConfig
) : Parameterized {
    private val impreciseNow = Instant.now()

    constructor(conf: ImmutableConfig): this(ScoringFilters(conf), conf)

    override fun getParams(): Params {
        return Params.of("injectTime", DateTimes.format(impreciseNow))
    }

    fun create(urlArgs: Pair<String, String>): WebPage {
        return create(urlArgs.key, urlArgs.value)
    }

    /**
     * @param url  The seed url
     * A configured url is a string contains the url and arguments.
     * @param args The args
     * @return The created WebPage.
     * If the url is an invalid url or an internal url, return ai.platon.pulsar.persistWebPage.NIL
     */
    fun create(url: String, args: String): WebPage {
        if (url.isEmpty()) {
            return WebPage.NIL
        }
        val page = WebPage.newWebPage(url, conf.toVolatileConfig())
        return if (makeSeed(url, args, page)) page else WebPage.NIL
    }

    fun makeSeed(page: WebPage): Boolean {
        return makeSeed(page.url, page.args, page)
    }

    private fun makeSeed(url: String, args: String, page: WebPage): Boolean {
//        if (page.isSeed) {
//            return false
//        }

        if (page.isInternal) {
            return false
        }
        val options = parse(args, conf)
        page.distance = 0
        if (page.createTime.isBefore(AppConstants.TCP_IP_STANDARDIZED_TIME)) {
            page.createTime = impreciseNow
        }
        page.markSeed()
        page.score = options.score.toFloat()
        scoreFilters.injectedScore(page)
        page.fetchTime = impreciseNow
        page.fetchInterval = options.fetchInterval
        page.fetchPriority = options.fetchPriority
        page.marks.put(Mark.INJECT, AppConstants.YES_STRING)
        return true
    }

    companion object {
        val LOG = LoggerFactory.getLogger(SeedBuilder::class.java)
    }
}
