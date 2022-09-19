package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.FlowState
import ai.platon.pulsar.common.HttpHeaders
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.model.ActiveDOMMessage
import java.time.Duration
import java.time.Instant

class NavigateTask(
        val task: FetchTask,
        val driver: WebDriver,
        val driverSettings: BrowserSettings
) {
    val startTime = Instant.now()

    val url = task.url
    val page = task.page
    var pageSource = ""
    val pageDatum = PageDatum(url)

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
    val fetchTask: FetchTask,
    val browserSettings: BrowserSettings,
    val driver: WebDriver
) {
    val url get() = fetchTask.url
    val isCanceled get() = fetchTask.isCanceled

    val conf get() = fetchTask.volatileConfig
    val interactSettings get() = browserSettings.interactSettings
}

class BrowserStatus(
        var status: ProtocolStatus,
        var code: Int = 0
)

class BrowserError(
        val status: ProtocolStatus,
        val activeDomMessage: ActiveDOMMessage
) {
    companion object {
        const val CONNECTION_TIMED_OUT = "ERR_CONNECTION_TIMED_OUT"
        const val NO_SUPPORTED_PROXIES = "ERR_NO_SUPPORTED_PROXIES"
        const val CONNECTION_CLOSED = "ERR_CONNECTION_CLOSED"
        const val EMPTY_RESPONSE = "ERR_EMPTY_RESPONSE"
        const val CONNECTION_RESET = "ERR_CONNECTION_RESET"
    }
}

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
