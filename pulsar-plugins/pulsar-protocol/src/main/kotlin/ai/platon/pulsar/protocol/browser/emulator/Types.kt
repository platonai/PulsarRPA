package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.common.InteractSettings
import ai.platon.pulsar.common.FlowState
import ai.platon.pulsar.common.HttpHeaders
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_INTERACT_SETTINGS
import ai.platon.pulsar.skeleton.crawl.fetch.FetchTask
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.model.ActiveDOMMessage
import java.time.Duration
import java.time.Instant

class NavigateTask constructor(
    val fetchTask: FetchTask,
    val driver: WebDriver,
    val browserSettings: BrowserSettings
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
     * TODO: page.getVar("InteractSettings") is deprecated, use pageConf[BROWSER_INTERACT_SETTINGS] instead
     * */
    val interactSettings get() = page.getVar("InteractSettings") as? InteractSettings
        ?: InteractSettings.fromJson(pageConf[BROWSER_INTERACT_SETTINGS], browserSettings.interactSettings)

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

class CancellableSleeper(val task: FetchTask): Sleeper {
    @Throws(NavigateTaskCancellationException::class)
    override fun sleep(duration: Duration) {
        try {
            Thread.sleep(duration.toMillis())
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        if (task.isCanceled) {
            throw NavigateTaskCancellationException("Task #${task.batchTaskId}}/${task.batchId} is canceled from sleeper")
        }
    }
}
