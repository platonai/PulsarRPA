/**
 * Copyright (c) Vincent Zhang, ivincent.zhang@gmail.com, Platon.AI.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.common.InteractSettings
import ai.platon.pulsar.common.FlowState
import ai.platon.pulsar.common.HttpHeaders
import ai.platon.pulsar.persist.AbstractWebPage
import ai.platon.pulsar.skeleton.crawl.fetch.FetchTask
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.model.ActiveDOMMessage
import java.time.Duration
import java.time.Instant

class NavigateTask constructor(
    val fetchTask: FetchTask,
    val driver: WebDriver
) {
    val startTime = Instant.now()

    val url get() = fetchTask.url
    val page get() = fetchTask.page

    val pageConf get() = fetchTask.page.conf
    /**
     * The page datum.
     * */
    val pageDatum = PageDatum(page)
    /**
     * The original content length, -1 means not specified, or we don't know.
     * */
    var originalContentLength = -1
    /**
     * The page source.
     * */
    var pageSource = ""

    /**
     * The interact settings.
     * */
    val interactSettings: InteractSettings get() {
        require(page is AbstractWebPage)
        return page.getBeanOrNull(InteractSettings::class.java) as? InteractSettings
            ?: page.conf.getBeanOrNull(InteractSettings::class.java)
            ?: driver.browser.settings.interactSettings
    }

    init {
        pageDatum.headers[HttpHeaders.Q_REQUEST_TIME] = startTime.toEpochMilli().toString()
    }
}

class InteractResult(
    var protocolStatus: ProtocolStatus,
    var activeDOMMessage: ActiveDOMMessage? = null,
    var state: FlowState = FlowState.CONTINUE
)

class InteractTask(
    val navigateTask: NavigateTask,
    val browserSettings: BrowserSettings,
    val driver: WebDriver
) {
    val url get() = navigateTask.url
    val page get() = navigateTask.page
    val isCanceled get() = navigateTask.fetchTask.page.isCanceled
    val pageConf get() = navigateTask.fetchTask.page.conf

    /**
     * The interact settings.
     * */
    val interactSettings get() = navigateTask.interactSettings
}

class BrowserErrorResponse(
        val status: ProtocolStatus,
        val activeDOMMessage: ActiveDOMMessage
)

interface Sleeper {
    fun sleep(duration: Duration)
}
