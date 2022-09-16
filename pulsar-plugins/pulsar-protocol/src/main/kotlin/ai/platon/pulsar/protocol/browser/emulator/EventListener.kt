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
    willCheckDOMState,
    didDOMStateCheck,
    willComputeFeature,
    featureComputed,
    willStopTab,
    tabStopped,
}

interface EventListener {

    fun onWillNavigate(handler: (WebPage, WebDriver) -> Unit)

    fun offWillNavigate(handler: (WebPage, WebDriver) -> Unit)

    fun onNavigated(handler: (WebPage, WebDriver) -> Unit)

    fun offNavigated(handler: (WebPage, WebDriver) -> Unit)

    fun onWillCheckDOMState(handler: (WebPage, WebDriver) -> Unit)

    fun offWillCheckDOMState(handler: (WebPage, WebDriver) -> Unit)

    fun onDOMStateChecked(handler: (WebPage, WebDriver) -> Unit)

    fun offDOMStateChecked(handler: (WebPage, WebDriver) -> Unit)

    fun onWillStopTab(handler: (WebPage, WebDriver) -> Unit)

    fun offWillStopTab(handler: (WebPage, WebDriver) -> Unit)

    fun onTabStopped(handler: (WebPage, WebDriver) -> Unit)

    fun offTabStopped(handler: (WebPage, WebDriver) -> Unit)

    suspend fun dispatchEvent(type: EventType, page: WebPage, driver: WebDriver)
}

abstract class AbstractEventListener: EventListener {
    private val logger = getLogger(AbstractEventListener::class)

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

    override fun onWillCheckDOMState(handler: (WebPage, WebDriver) -> Unit) {
        listeners.on(EventType.willCheckDOMState, handler)
    }

    override fun offWillCheckDOMState(handler: (WebPage, WebDriver) -> Unit) {
        listeners.off(EventType.willCheckDOMState, handler)
    }

    override fun onDOMStateChecked(handler: (WebPage, WebDriver) -> Unit) {
        listeners.on(EventType.didDOMStateCheck, handler)
    }

    override fun offDOMStateChecked(handler: (WebPage, WebDriver) -> Unit) {
        listeners.off(EventType.didDOMStateCheck, handler)
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

            EventType.willCheckDOMState -> listeners.notify(EventType.willCheckDOMState, page, driver)
            EventType.didDOMStateCheck -> listeners.notify(EventType.didDOMStateCheck, page, driver)

            EventType.willComputeFeature -> listeners.notify(EventType.willComputeFeature, page, driver)
            EventType.featureComputed -> listeners.notify(EventType.featureComputed, page, driver)

            EventType.willStopTab -> listeners.notify(EventType.willStopTab, page, driver)
            EventType.tabStopped -> listeners.notify(EventType.tabStopped, page, driver)

            else -> {}
        }

        val event = page.event?.simulateEvent ?: return
        when (type) {
            EventType.willNavigate -> notify(type) { event.onWillNavigate(page, driver) }
            EventType.navigated -> notify(type) { event.onNavigated(page, driver) }

            EventType.willCheckDOMState -> notify(type) { event.onWillCheckDOMState(page, driver) }
            EventType.didDOMStateCheck -> notify(type) { event.onDOMStateChecked(page, driver) }

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
