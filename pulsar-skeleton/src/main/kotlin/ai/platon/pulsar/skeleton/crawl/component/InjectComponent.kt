package ai.platon.pulsar.skeleton.crawl.component

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.warnForClose
import ai.platon.pulsar.persist.WebDBException
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.impl.WebPageImpl
import ai.platon.pulsar.persist.metadata.Mark
import ai.platon.pulsar.skeleton.common.urls.NormURL
import ai.platon.pulsar.skeleton.crawl.inject.SeedBuilder
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by vincent on 17-5-14.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 */
class InjectComponent(
    private val seedBuilder: SeedBuilder,
    val webDb: WebDb,
    val conf: ImmutableConfig
) : Parameterized, AutoCloseable {
    private val closed = AtomicBoolean(false)

    constructor(webDb: WebDb, conf: ImmutableConfig) : this(SeedBuilder(conf), webDb, conf)

    override fun getParams(): Params {
        return seedBuilder.params
    }

    @Throws(WebDBException::class)
    fun inject(urlArgs: Pair<String, String>): WebPage {
        return inject(urlArgs.first, urlArgs.second)
    }

    @Throws(WebDBException::class)
    fun inject(normURL: NormURL): WebPage {
        return inject(normURL.spec, normURL.args)
    }

    @Throws(WebDBException::class)
    fun inject(url: String, args: String): WebPage {
        var page = webDb.get(url, false)

        if (page.isNil) {
            page = seedBuilder.create(url, args)
            if (page.isSeed) {
                webDb.put(page)
            }
            return page
        }

        // already exist in db, update the status and mark it as a seed
        page.args = args
        return if (inject(page)) page else WebPageImpl.NIL
    }

    @Throws(WebDBException::class)
    fun inject(page: WebPage): Boolean {
        val success = seedBuilder.makeSeed(page)
        if (success) {
            webDb.put(page)
            return true
        }

        return false
    }

    @Throws(WebDBException::class)
    fun unInject(page: WebPage): WebPage {
        if (!page.isSeed) {
            return page
        }
        page.unmarkSeed()
        page.marks.remove(Mark.INJECT)
        webDb.put(page)
        return page
    }

    @Throws(WebDBException::class)
    fun flush() {
        webDb.flush()
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            runCatching { flush() }.onFailure { warnForClose(this, it) }
        }
    }
}
