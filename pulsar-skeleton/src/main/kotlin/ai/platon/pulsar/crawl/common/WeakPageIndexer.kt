package ai.platon.pulsar.crawl.common

import ai.platon.pulsar.persist.HypeLink
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import com.google.common.collect.Lists
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Created by vincent on 17-6-18.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * A WebPage based indexer to pages, the index does not support ACID
 */
class WeakPageIndexer(homeUrl: CharSequence, private val webDb: WebDb) {
    private val LOG = LoggerFactory.getLogger(WeakPageIndexer::class.java)
    private val homeUrl = homeUrl.toString()

    fun home(): WebPage {
        return home
    }

    operator fun get(pageNo: Int): WebPage {
        return getIndex(pageNo)
    }

    fun index(url: CharSequence) {
        indexAll(1, Lists.newArrayList(url))
    }

    fun indexAll(urls: Iterable<CharSequence>) {
        indexAll(1, urls)
    }

    fun indexAll(pageNo: Int, urls: Iterable<CharSequence>) {
        updateAll(pageNo, urls, false)
    }

    fun getAll(pageNo: Int): Set<CharSequence> {
        return get(pageNo).vividLinks.keys
    }

    /**
     * Return a copy of all urls in page N, and clear it's urls
     */
    @Synchronized
    fun takeN(pageNo: Int, n: Int): Set<CharSequence> {
        var n = n
        val page = get(pageNo)
        val urls: MutableSet<CharSequence> = HashSet()
        val it: MutableIterator<Map.Entry<CharSequence, CharSequence>> = page.vividLinks.entries.iterator()
        while (n-- > 0 && it.hasNext()) {
            urls.add(it.next().key)
            it.remove()
        }
        if (!urls.isEmpty()) {
            if (LOG.isDebugEnabled) {
                LOG.debug("Taken {} urls from page {}", urls.size, page.url)
            }
        }
        webDb.put(page)
        webDb.flush()
        return urls
    }

    @Synchronized
    fun takeAll(pageNo: Int): Set<CharSequence> {
        return takeN(pageNo, Int.MAX_VALUE)
    }

    fun remove(url: String) {
        remove(1, url)
    }

    fun removeAll(urls: Iterable<CharSequence>) {
        removeAll(1, urls)
    }

    fun remove(pageNo: Int, url: CharSequence) {
        updateAll(pageNo, Lists.newArrayList(url), true)
    }

    fun removeAll(pageNo: Int, urls: Iterable<CharSequence>) {
        updateAll(pageNo, urls, true)
    }

    fun commit() {
        webDb.flush()
    }

    private fun update(pageNo: Int, newHypeLinks: HypeLink, remove: Boolean) {
        updateAll(pageNo, Lists.newArrayList<CharSequence>(newHypeLinks.url), remove)
    }

    @Synchronized
    private fun updateAll(pageNo: Int, urls: Iterable<CharSequence>, remove: Boolean) {
        if (!urls.iterator().hasNext()) {
            return
        }
        val indexPage = getIndex(pageNo)
        val vividLinks = indexPage.vividLinks
        if (remove) {
            urls.forEach { vividLinks.remove(it) }
        } else {
            urls.forEach { vividLinks[it] = "" }
        }

        val message = "Total " + vividLinks.size + " indexed links"
        indexPage.setTextCascaded(message)
        if (LOG.isDebugEnabled) {
            LOG.debug(message + ", indexed in " + indexPage.url)
        }

        // webDb.put(indexPage.getUrl(), indexPage, true);
        webDb.put(indexPage)
        webDb.flush()
    }

    @get:Synchronized
    private val home: WebPage
        get() {
            var home = webDb.getOrNil(homeUrl)
            if (home.isNil) {
                home = WebPage.newInternalPage(homeUrl, "Web Page Index Home")
                LOG.debug("Creating weak index home: $homeUrl")
            }
            webDb.put(home)
            webDb.flush()
            return home
        }

    private fun getIndex(pageNo: Int): WebPage {
        return getIndex(pageNo, "Web Page Index $pageNo")
    }

    @Synchronized
    private fun getIndex(pageNo: Int, pageTitle: String): WebPage {
        val url = "$homeUrl/$pageNo"
        var indexPage = webDb.getOrNil(url)
        if (indexPage.isNil) {
            val home = home
            home.vividLinks[url] = ""
            webDb.put(home)
            indexPage = WebPage.newInternalPage(url, pageTitle)
            webDb.put(indexPage)
            webDb.flush()
            // log.debug("Created weak index: " + url);
        }
        return indexPage
    }
}
