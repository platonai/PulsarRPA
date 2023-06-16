package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.event.EventEmitter
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.WebPage

enum class EmulateEvents {
    willNavigate,
    navigated,
    willInteract,
    didInteract,
    willCheckDocumentState,
    documentActuallyReady,
    willScroll,
    didScroll,
    willComputeFeature,
    featureComputed,
    willStopTab,
    tabStopped,
}

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2023 Platon AI. All rights reserved.
 *
 * About emulate, simulate, mimic and imitate:
 * 1. Emulate is usually used with someone as an object.
 * 2. Simulate has the idea of copying something so that the copy pretends to be the original thing.
 * 3. Mimic, a person who imitate mannerisms of others.
 * 4. Imitate is the most general of the four words, can be used in all the three senses.
 */
interface BrowserEmulator: EventEmitter<EmulateEvents>, AutoCloseable {

    /**
     * Fetch a page using a browser which can render the DOM and execute scripts.
     *
     * @param task The task to fetch
     * @return The result of this fetch
     * */
    @Deprecated("Inappropriate name", ReplaceWith("visit(task, driver)"))
    suspend fun fetch(task: FetchTask, driver: WebDriver): FetchResult

    /**
     * Fetch a page using a browser which can render the DOM and execute scripts.
     *
     * @param task The task to fetch
     * @return The result of this fetch
     * */
    suspend fun visit(task: FetchTask, driver: WebDriver): FetchResult

    fun cancelNow(task: FetchTask)

    suspend fun cancel(task: FetchTask)

    suspend fun onWillNavigate(page: WebPage, driver: WebDriver)

    suspend fun onNavigated(page: WebPage, driver: WebDriver)

    suspend fun onWillInteract(page: WebPage, driver: WebDriver)

    suspend fun onWillCheckDocumentState(page: WebPage, driver: WebDriver)

    suspend fun onDocumentActuallyReady(page: WebPage, driver: WebDriver)

    suspend fun onWillScroll(page: WebPage, driver: WebDriver)

    suspend fun onDidScroll(page: WebPage, driver: WebDriver)

    suspend fun onWillComputeFeature(page: WebPage, driver: WebDriver)

    suspend fun onFeatureComputed(page: WebPage, driver: WebDriver)

    suspend fun onDidInteract(page: WebPage, driver: WebDriver)

    suspend fun onWillStopTab(page: WebPage, driver: WebDriver)

    suspend fun onTabStopped(page: WebPage, driver: WebDriver)
}
