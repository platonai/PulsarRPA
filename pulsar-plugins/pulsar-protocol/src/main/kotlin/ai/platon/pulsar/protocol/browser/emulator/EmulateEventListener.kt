package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.event.ListenerCollection
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.persist.ext.event
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.driver.WebDriverCancellationException
import ai.platon.pulsar.crawl.fetch.driver.WebDriverException
import ai.platon.pulsar.persist.WebPage

enum class EventType {
    willNavigate,
    navigated,
    willInteract,
    didInteract,
    willCheckDocumentState,
    documentActuallyReady,
    willComputeFeature,
    featureComputed,
    willStopTab,
    tabStopped,
}

/**
 * About emulate, simulate, mimic and imitate:
 * 1. Emulate is usually used with someone as an object.
 * 2. Simulate has the idea of copying something so that the copy pretends to be the original thing.
 * 3. Mimic, a person who imitate mannerisms of others.
 * 4. Imitate is the most general of the four words, can be used in all the three senses.
 * */
interface EmulateEventListener {

    fun onWillNavigate(handler: (WebPage, WebDriver) -> Unit)

    fun offWillNavigate(handler: (WebPage, WebDriver) -> Unit)

    fun onNavigated(handler: (WebPage, WebDriver) -> Unit)

    fun offNavigated(handler: (WebPage, WebDriver) -> Unit)

    fun onWillCheckDocumentState(handler: (WebPage, WebDriver) -> Unit)

    fun offWillCheckDocumentState(handler: (WebPage, WebDriver) -> Unit)

    fun onDocumentActuallyReady(handler: (WebPage, WebDriver) -> Unit)

    fun offDocumentActuallyReady(handler: (WebPage, WebDriver) -> Unit)

    fun onWillStopTab(handler: (WebPage, WebDriver) -> Unit)

    fun offWillStopTab(handler: (WebPage, WebDriver) -> Unit)

    fun onTabStopped(handler: (WebPage, WebDriver) -> Unit)

    fun offTabStopped(handler: (WebPage, WebDriver) -> Unit)

    suspend fun dispatchEvent(type: EventType, page: WebPage, driver: WebDriver)
}

abstract class AbstractEmulateEventListener: EmulateEventListener {
    private val logger = getLogger(AbstractEmulateEventListener::class)

    protected val listeners = ListenerCollection<EventType>()

    override fun onWillNavigate(handler: (WebPage, WebDriver) -> Unit) {
        listeners.on(EventType.willNavigate, handler)
    }

    override fun offWillNavigate(handler: (WebPage, WebDriver) -> Unit) {
        listeners.off(EventType.willNavigate, handler)
    }

    override fun onNavigated(handler: (WebPage, WebDriver) -> Unit) {
        listeners.on(EventType.navigated, handler)
    }

    override fun offNavigated(handler: (WebPage, WebDriver) -> Unit) {
        listeners.off(EventType.navigated, handler)
    }

    override fun onWillCheckDocumentState(handler: (WebPage, WebDriver) -> Unit) {
        listeners.on(EventType.willCheckDocumentState, handler)
    }

    override fun offWillCheckDocumentState(handler: (WebPage, WebDriver) -> Unit) {
        listeners.off(EventType.willCheckDocumentState, handler)
    }

    override fun onDocumentActuallyReady(handler: (WebPage, WebDriver) -> Unit) {
        listeners.on(EventType.documentActuallyReady, handler)
    }

    override fun offDocumentActuallyReady(handler: (WebPage, WebDriver) -> Unit) {
        listeners.off(EventType.documentActuallyReady, handler)
    }

    override fun onWillStopTab(handler: (WebPage, WebDriver) -> Unit) {
        listeners.on(EventType.willStopTab, handler)
    }

    override fun offWillStopTab(handler: (WebPage, WebDriver) -> Unit) {
        listeners.off(EventType.willStopTab, handler)
    }

    override fun onTabStopped(handler: (WebPage, WebDriver) -> Unit) {
        listeners.on(EventType.tabStopped, handler)
    }

    override fun offTabStopped(handler: (WebPage, WebDriver) -> Unit) {
        listeners.off(EventType.tabStopped, handler)
    }

    override suspend fun dispatchEvent(type: EventType, page: WebPage, driver: WebDriver) {
        when (type) {
            EventType.willNavigate -> listeners.notify(EventType.willNavigate, page, driver)
            EventType.navigated -> listeners.notify(EventType.navigated, page, driver)

            EventType.willCheckDocumentState -> listeners.notify(EventType.willCheckDocumentState, page, driver)
            EventType.documentActuallyReady -> listeners.notify(EventType.documentActuallyReady, page, driver)

            EventType.willComputeFeature -> listeners.notify(EventType.willComputeFeature, page, driver)
            EventType.featureComputed -> listeners.notify(EventType.featureComputed, page, driver)

            EventType.willStopTab -> listeners.notify(EventType.willStopTab, page, driver)
            EventType.tabStopped -> listeners.notify(EventType.tabStopped, page, driver)

            else -> {}
        }

        val event = page.event?.browseEvent ?: return
        when (type) {
            EventType.willNavigate -> notify(type) { event.onWillNavigate(page, driver) }
            EventType.navigated -> notify(type) { event.onNavigated(page, driver) }

            EventType.willCheckDocumentState -> notify(type) { event.onWillCheckDocumentState(page, driver) }
            EventType.documentActuallyReady -> notify(type) { event.onDocumentActuallyReady(page, driver) }

            EventType.willComputeFeature -> notify(type) { event.onWillComputeFeature(page, driver) }
            EventType.featureComputed -> notify(type) { event.onFeatureComputed(page, driver) }

            EventType.willStopTab -> notify(type) { event.onWillStopTab(page, driver) }
            EventType.tabStopped -> notify(type) { event.onTabStopped(page, driver) }

            else -> {}
        }
    }

    private suspend fun notify(type: EventType, action: suspend () -> Unit) {
        notify(type.name, action)
    }

    private suspend fun notify(name: String, action: suspend () -> Unit) {
        try {
            action()
        } catch (e: WebDriverCancellationException) {
            logger.info("Web driver is cancelled")
        } catch (e: WebDriverException) {
            logger.warn(e.brief("[Ignored][$name] "))
        } catch (e: Exception) {
            logger.warn(e.stringify("[Ignored][$name] "))
        } catch (e: Throwable) {
            logger.error(e.stringify("[Unexpected][$name] "))
        }
    }
}
