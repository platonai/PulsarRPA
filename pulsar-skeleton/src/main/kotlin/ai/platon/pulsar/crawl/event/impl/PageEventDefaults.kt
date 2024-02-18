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
    /**
     * The RPA to use.
     * */
    val rpa: BrowseRPA = DefaultBrowseRPA()
) : AbstractBrowseEvent() {
    /**
     * Fire when a browser is launched.
     * */
    override val onBrowserLaunched = WebPageWebDriverEventHandler().also {
        it.addLast { page, driver -> rpa.warnUpBrowser(page, driver) }
    }
    
    /**
     * Fire when a page is about to fetch.
     * */
    override val onWillFetch = WebPageWebDriverEventHandler().also {
        it.addLast { page, driver ->
            rpa.waitForReferrer(page, driver)
            rpa.waitForPreviousPage(page, driver)
        }
    }
}

/**
 * The empty browse event handler.
 */
class EmptyBrowseEvent(
    /**
     * The RPA to use.
     * */
    val rpa: BrowseRPA = DefaultBrowseRPA()
) : AbstractBrowseEvent() {

}

/**
 * The default browse event handler.
 */
typealias DefaultBrowseEvent = EmptyBrowseEvent

/**
 * The default page event handler.
 */
open class DefaultPageEvent(
    loadEvent: LoadEvent = DefaultLoadEvent(),
    browseEvent: BrowseEvent = DefaultBrowseEvent(),
    crawlEvent: CrawlEvent = DefaultCrawlEvent()
) : AbstractPageEvent(loadEvent, browseEvent, crawlEvent)

/**
 * The factory to create page event handler.
 */
class PageEventFactory(val conf: ImmutableConfig = ImmutableConfig()) {
    private val logger = LoggerFactory.getLogger(PageEventFactory::class.java)

    /**
     * Create a page event handler.
     * */
    @Synchronized
    fun create(): PageEvent = createUsingGlobalConfig(conf)
    
    /**
     * Create a page event handler with [className].
     * */
    @Synchronized
    fun create(className: String): PageEvent {
        val gen = when (className) {
            DefaultPageEvent::class.java.name -> DefaultPageEvent()
            else -> createUsingGlobalConfig(conf)
        }
        
        return gen
    }
    
    /**
     * Create a page event handler with [loadEvent], [browseEvent] and [crawlEvent].
     * */
    @Synchronized
    fun create(
        loadEvent: LoadEvent = DefaultLoadEvent(),
        browseEvent: BrowseEvent = DefaultBrowseEvent(),
        crawlEvent: CrawlEvent = DefaultCrawlEvent()
    ): PageEvent = DefaultPageEvent(loadEvent, browseEvent, crawlEvent)
    
    private fun createUsingGlobalConfig(conf: ImmutableConfig): PageEvent {
        return createUsingGlobalConfig(conf, DefaultPageEvent::class.java.name)
    }
    
    private fun createUsingGlobalConfig(conf: ImmutableConfig, className: String): PageEvent {
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
        
        return clazz.constructors.first { it.parameters.isEmpty() }.newInstance() as PageEvent
    }
}
