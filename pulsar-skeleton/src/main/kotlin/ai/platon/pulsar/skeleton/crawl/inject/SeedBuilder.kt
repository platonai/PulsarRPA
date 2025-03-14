package ai.platon.pulsar.skeleton.crawl.inject

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.impl.WebPageImpl
import ai.platon.pulsar.skeleton.crawl.scoring.ScoringFilters
import org.apache.commons.lang3.tuple.Pair
import java.time.Instant

/**
 * Created by vincent on 16-9-24.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class SeedBuilder(
    private val scoreFilters: ScoringFilters,
    private val conf: ImmutableConfig
) : Parameterized {

    constructor(conf: ImmutableConfig) : this(ScoringFilters(conf), conf)

    override fun getParams(): Params {
        return Params.of("injectTime", DateTimes.format(Instant.now()))
    }

    fun create(urlArgs: Pair<String, String>): WebPage {
        return create(urlArgs.key, urlArgs.value)
    }

    /**
     * @param url  The seed url
     * A configured url is a string contains the url and arguments.
     * @param args The args
     * @return The created WebPage.
     * If the url is an invalid url or an internal url, return ai.platon.pulsar.persistWebPageImpl.NIL
     */
    fun create(url: String, args: String): WebPage {
        if (url.isEmpty()) {
            return WebPageImpl.NIL
        }
        val page = WebPageImpl.newWebPage(url, conf.toVolatileConfig())
        return if (makeSeed(url, args, page)) page else WebPageImpl.NIL
    }

    fun makeSeed(page: WebPage): Boolean {
        return makeSeed(page.url, page.args, page)
    }

    private fun makeSeed(url: String, args: String, page: WebPage): Boolean {
        if (page.isInternal) {
            return false
        }

        val now = Instant.now()
        page.distance = 0
        page.fetchTime = now
        page.markSeed()
        return true
    }
}
