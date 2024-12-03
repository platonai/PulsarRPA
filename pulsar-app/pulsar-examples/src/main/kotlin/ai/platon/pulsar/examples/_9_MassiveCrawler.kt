package ai.platon.pulsar.examples

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.collect.DelayUrl
import ai.platon.pulsar.skeleton.context.PulsarContexts
import ai.platon.pulsar.skeleton.crawl.common.url.ParsableHyperlink
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import java.time.Duration

/**
 * Demonstrates how to crawl a massive collection of urls with various requirements.
 * */
fun main() {
    BrowserSettings.privacy(10).maxOpenTabs(4).headless()

    val session = PulsarContexts.createSession()
    val crawlPool = session.context.crawlPool

    val parseHandler = { _: WebPage, document: FeaturedDocument ->
        // do something wonderful with the document
        println(document.title + "\t|\t" + document.baseURI)
    }
    val urls = LinkExtractors.fromResource("seeds100.txt").map { ParsableHyperlink(it, parseHandler) }

    val (url1, url2, url3, url4, url5) = urls.shuffled()
    session.submit(url1)
    session.submit(url2, "-refresh")
    session.submit(url3, "-i 30s")

    session.submitAll(urls)
    // feel free to submit millions of urls here
    session.submitAll(urls, "-i 7d")

    crawlPool.add(url4)
    crawlPool.add(url5.apply { priority = Priority13.HIGHER4.value })

    crawlPool.highestCache.reentrantQueue.add(url1)
    crawlPool.higher2Cache.nonReentrantQueue.add(url2)
    crawlPool.lower2Cache.nReentrantQueue.add(url3)
    // highest priority
    crawlPool.realTimeCache.reentrantQueue.add(url4)
    // will start 2 hours later
    crawlPool.delayCache.add(DelayUrl(url5, Duration.ofHours(2)))

    // wait for all tasks to be finished.
    session.context.await()
}
