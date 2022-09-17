package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved.
 */
interface BrowserEmulator: EmulateEventListener, AutoCloseable {
    /**
     * Fetch a page using a browser which can render the DOM and execute scripts.
     *
     * @param task The task to fetch
     * @return The result of this fetch
     * */
    suspend fun fetch(task: FetchTask, driver: WebDriver): FetchResult

    fun cancelNow(task: FetchTask)

    suspend fun cancel(task: FetchTask)
}
