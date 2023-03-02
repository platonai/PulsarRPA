package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.common.InteractSettings
import ai.platon.pulsar.common.FlowState
import ai.platon.pulsar.common.HttpHeaders
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.model.ActiveDOMMessage
import java.lang.ref.WeakReference
import java.time.Duration
import java.time.Instant

class NavigateTask constructor(
    val fetchTask: FetchTask,
    val driver: WebDriver,
    val driverSettings: BrowserSettings
) {
    val startTime = Instant.now()

    val url get() = fetchTask.url
    val page get() = fetchTask.page
    val pageDatum = PageDatum(url)

    var originalContentLength = -1
    var pageSource = ""

    init {
        pageDatum.headers[HttpHeaders.Q_REQUEST_TIME] = startTime.toEpochMilli().toString()
        pageDatum.page = WeakReference(fetchTask.page)
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
    val isCanceled get() = navigateTask.fetchTask.isCanceled

    val conf get() = navigateTask.fetchTask.volatileConfig
    /**
     * TODO: this is a temporary solution to set page scope interaction settings, it should be reviewed and improved
     * */
    val interactSettings get() = page.getVar("InteractSettings") as? InteractSettings
        ?: browserSettings.interactSettings
}

class BrowserStatus(
        var status: ProtocolStatus,
        var code: Int = 0
)

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
