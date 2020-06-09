package ai.platon.pulsar.crawl.component

import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.crawl.common.WeakPageIndexer
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.inject.SeedBuilder
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.Mark
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by vincent on 17-5-14.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class InjectComponent(
        private val seedBuilder: SeedBuilder,
        val webDb: WebDb,
        val conf: ImmutableConfig
) : Parameterized, AutoCloseable {
    private val seedIndexer: WeakPageIndexer = WeakPageIndexer(AppConstants.SEED_HOME_URL, webDb)
    private val closed = AtomicBoolean(false)

    override fun getParams(): Params {
        return seedBuilder.params
    }

    fun inject(urlArgs: Pair<String, String>): WebPage {
        return inject(urlArgs.first, urlArgs.second)
    }

    fun inject(url: String, args: String): WebPage {
        var page = webDb.getOrNil(url, false)

        if (page.isNil) {
            page = seedBuilder.create(url, args)
            if (page.isSeed) {
                webDb.put(page)
                seedIndexer.index(page.url)
            }
            return page
        }

        // already exist in db, update the status and mark it as a seed
        page.options = args
        return if (inject(page)) page else WebPage.NIL
    }

    fun inject(page: WebPage): Boolean {
        val success = seedBuilder.makeSeed(page)
        if (success) {
            webDb.put(page)
            seedIndexer.index(page.url)
            return true
        }

        return false
    }

    fun injectAll(vararg configuredUrls: String): List<WebPage> {
        return configuredUrls.map { inject(Urls.splitUrlArgs(it)) }
    }

    fun injectAll(pages: Collection<WebPage>): List<WebPage> {
        val injectedPages = pages.onEach { inject(it) }.filter { it.isSeed }
        seedIndexer.indexAll(injectedPages.map { it.url })
        LOG.info("Injected " + injectedPages.size + " seeds out of " + pages.size + " pages")
        return injectedPages
    }

    fun unInject(url: String): WebPage {
        val page = webDb.getOrNil(url)
        if (page.isSeed) {
            unInject(page)
        }
        return page
    }

    fun unInject(page: WebPage): WebPage {
        if (!page.isSeed) {
            return page
        }
        page.unmarkSeed()
        page.marks.remove(Mark.INJECT)
        seedIndexer.remove(page.url)
        webDb.put(page)
        return page
    }

    fun unInjectAll(vararg urls: String): List<WebPage> {
        val pages = urls.mapNotNull { webDb.get(it) }.filter { it.isSeed }.onEach { unInject(it) }
        LOG.debug("UnInjected " + pages.size + " urls")
        seedIndexer.removeAll(pages.map { it.url })
        return pages
    }

    fun unInjectAll(pages: Collection<WebPage>): Collection<WebPage> {
        pages.forEach { this.unInject(it) }
        return pages
    }

    fun report(): String {
        val seedHome = webDb.getOrNil(AppConstants.SEED_PAGE_1_URL)
        if (seedHome.isNil) {
            val count = seedHome.liveLinks.size
            return "Total " + count + " seeds in store " + webDb.schemaName
        }
        return "No home page"
    }

    fun commit() {
        webDb.flush()
        seedIndexer.commit()
    }

    override fun close() {
        if (closed.getAndSet(true)) {
            return
        }
        commit()
    }

    private fun loadOrCreate(url: String): WebPage {
        var page = webDb.getOrNil(url)
        if (page.isNil) {
            page = WebPage.newWebPage(url)
        }
        return page
    }

    companion object {
        val LOG = LoggerFactory.getLogger(InjectComponent::class.java)
    }
}
