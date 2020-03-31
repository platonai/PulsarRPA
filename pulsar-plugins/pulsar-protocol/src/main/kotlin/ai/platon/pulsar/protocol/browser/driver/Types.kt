package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.BrowserControl
import ai.platon.pulsar.common.FlowState
import ai.platon.pulsar.common.HttpHeaders
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.metadata.MultiMetadata
import ai.platon.pulsar.persist.model.ActiveDomMessage
import org.openqa.selenium.support.ui.Sleeper
import java.time.Duration
import java.time.Instant

class BrowseTask(
        val task: FetchTask,
        val driver: ManagedWebDriver,
        val driverConfig: BrowserControl
) {
    val navigateTime = Instant.now()

    val url = task.url
    val page = task.page

    val headers = MultiMetadata(HttpHeaders.Q_REQUEST_TIME, navigateTime.toEpochMilli().toString())

    var status: ProtocolStatus = ProtocolStatus.STATUS_CANCELED
    var activeDomMessage: ActiveDomMessage? = null
    var pageSource = ""
}

class NavigateResult(
        var protocolStatus: ProtocolStatus,
        var activeDomMessage: ActiveDomMessage? = null,
        var state: FlowState = FlowState.CONTINUE
)

class EmulateTask(
        val fetchTask: FetchTask,
        val driverConfig: BrowserControl,
        val driver: ManagedWebDriver
) {
    val url get() = fetchTask.url
    val isCanceled get() = fetchTask.isCanceled
}

class BrowserStatus(
        var status: ProtocolStatus,
        var code: Int = 0
)

class BrowserError(
        val status: ProtocolStatus,
        val activeDomMessage: ActiveDomMessage
) {
    companion object {
        const val CONNECTION_TIMED_OUT = "ERR_CONNECTION_TIMED_OUT"
        const val NO_SUPPORTED_PROXIES = "ERR_NO_SUPPORTED_PROXIES"
        const val CONNECTION_CLOSED = "ERR_CONNECTION_CLOSED"
        const val EMPTY_RESPONSE = "ERR_EMPTY_RESPONSE"
        const val CONNECTION_RESET = "ERR_CONNECTION_RESET"
    }
}

class CancellableSleeper(val task: FetchTask): Sleeper {
    @Throws(CancellationException::class)
    override fun sleep(duration: Duration) {
        try {
            Thread.sleep(duration.toMillis())
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        if (task.isCanceled) {
            throw CancellationException("Task #${task.batchTaskId}}/${task.batchId} is canceled from sleeper")
        }
    }
}
