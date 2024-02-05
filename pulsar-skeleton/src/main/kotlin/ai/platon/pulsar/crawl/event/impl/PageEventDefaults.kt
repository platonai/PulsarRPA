package ai.platon.pulsar.crawl.event.impl

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.BrowseEvent
import ai.platon.pulsar.crawl.CrawlEvent
import ai.platon.pulsar.crawl.LoadEvent
import ai.platon.pulsar.crawl.PageEvent
import ai.platon.pulsar.crawl.event.*
import ai.platon.pulsar.crawl.fetch.driver.rpa.BrowseRPA
import ai.platon.pulsar.crawl.fetch.driver.rpa.DefaultBrowseRPA
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * The default load event handler.
 */
open class DefaultLoadEvent(
    val rpa: BrowseRPA = DefaultBrowseRPA()
) : AbstractLoadEvent()

/**
 * The default crawl event handler.
 */
open class DefaultCrawlEvent : AbstractCrawlEvent()

/**
 * The perfect trace browse event handler.
 */
class PerfectTraceBrowseEvent(
    val rpa: BrowseRPA = DefaultBrowseRPA()
) : AbstractBrowseEvent() {
    
    override val onBrowserLaunched = WebPageWebDriverEventHandler().also {
        it.addLast { page, driver -> rpa.warnUpBrowser(page, driver) }
    }
    
    override val onWillFetch = WebPageWebDriverEventHandler().also {
        it.addLast { page, driver ->
            rpa.waitForReferrer(page, driver)
            rpa.waitForPreviousPage(page, driver)
        }
    }
}

class EmptyBrowseEvent(
    val rpa: BrowseRPA = DefaultBrowseRPA()
) : AbstractBrowseEvent() {

}

typealias DefaultBrowseEvent = EmptyBrowseEvent

open class DefaultPageEvent(
    loadEvent: LoadEvent = DefaultLoadEvent(),
    browseEvent: BrowseEvent = DefaultBrowseEvent(),
    crawlEvent: CrawlEvent = DefaultCrawlEvent()
) : AbstractPageEvent(loadEvent, browseEvent, crawlEvent)

class PageEventFactory(val conf: ImmutableConfig = ImmutableConfig()) {
    private val logger = LoggerFactory.getLogger(PageEventFactory::class.java)

    @Synchronized
    fun create(): PageEvent = create("")
    
    @Synchronized
    fun create(className: String): DefaultPageEvent {
        val gen = when (className) {
            DefaultPageEvent::class.java.name -> DefaultPageEvent()
            else -> createUsingGlobalConfig(conf)
        }
        
        return gen
    }
    
    @Synchronized
    fun create(
        loadEvent: LoadEvent = DefaultLoadEvent(),
        browseEvent: BrowseEvent = DefaultBrowseEvent(),
        crawlEvent: CrawlEvent = DefaultCrawlEvent()
    ): PageEvent = DefaultPageEvent(loadEvent, browseEvent, crawlEvent)
    
    private fun createUsingGlobalConfig(conf: ImmutableConfig): DefaultPageEvent {
        return createUsingGlobalConfig(conf, DefaultPageEvent::class.java.name)
    }
    
    private fun createUsingGlobalConfig(conf: ImmutableConfig, className: String): DefaultPageEvent {
        val defaultClazz = DefaultPageEvent::class.java
        val clazz = try {
            conf.getClass(className, defaultClazz)
        } catch (e: Exception) {
            logger.warn(
                "Configured PageEvent generator {}({}) is not found, use default ({})",
                className, conf.get(className), defaultClazz.simpleName
            )
            defaultClazz
        }
        
        return clazz.constructors.first { it.parameters.isEmpty() }.newInstance() as DefaultPageEvent
    }
}
