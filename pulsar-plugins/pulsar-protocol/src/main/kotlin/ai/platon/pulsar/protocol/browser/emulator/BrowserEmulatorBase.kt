package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.protocol.browser.driver.WebDriverControl
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

abstract class BrowserEmulatorBase(
        val driverControl: WebDriverControl,
        val eventHandler: EventHandler,
        val immutableConfig: ImmutableConfig
): Parameterized, AutoCloseable {
    private val log = LoggerFactory.getLogger(BrowserEmulatorBase::class.java)!!
    private val tracer = log.takeIf { it.isTraceEnabled }
    val supportAllCharsets get() = immutableConfig.getBoolean(CapabilityTypes.PARSE_SUPPORT_ALL_CHARSETS, true)
    val charsetPattern = if (supportAllCharsets) SYSTEM_AVAILABLE_CHARSET_PATTERN else DEFAULT_CHARSET_PATTERN
    val fetchMaxRetry = immutableConfig.getInt(CapabilityTypes.HTTP_FETCH_MAX_RETRY, 3)
    val closed = AtomicBoolean(false)
    val isActive get() = !closed.get()
    val meterNavigates by lazy { AppMetrics.reg.meter(this,"navigates") }
    val counterRequests by lazy { AppMetrics.reg.counter(this,"requests") }
    val counterCancels by lazy { AppMetrics.reg.counter(this,"cancels") }

    override fun getParams(): Params {
        return Params.of(
                "pageLoadTimeout", driverControl.pageLoadTimeout,
                "scriptTimeout", driverControl.scriptTimeout,
                "scrollDownCount", driverControl.scrollDownCount,
                "scrollInterval", driverControl.scrollInterval,
                "jsInvadingEnabled", driverControl.jsInvadingEnabled,
                "emulatorEventHandler", immutableConfig.get(CapabilityTypes.BROWSER_EMULATOR_EVENT_HANDLER)
        )
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {

        }
    }

    @Throws(IllegalApplicationContextStateException::class)
    protected fun checkState() {
        if (!isActive) {
            AppContext.tryTerminate()
            throw IllegalApplicationContextStateException("Emulator is closed")
        }
    }

    /**
     * Check task state
     * every direct or indirect IO operation is a checkpoint for the context reset event
     * */
    @Throws(NavigateTaskCancellationException::class, IllegalApplicationContextStateException::class)
    protected fun checkState(driver: WebDriver) {
        checkState()

        if (driver.isCanceled) {
            // the task is canceled, so the navigation is stopped, the driver is closed, the privacy context is reset
            // and all the running tasks should be redo
            throw NavigateTaskCancellationException("Task with driver #${driver.id} is canceled | ${driver.url}")
        }
    }

    /**
     * Check task state
     * every direct or indirect IO operation is a checkpoint for the context reset event
     * */
    @Throws(NavigateTaskCancellationException::class, IllegalApplicationContextStateException::class)
    protected fun checkState(task: FetchTask) {
        checkState()

        if (task.isCanceled) {
            // the task is canceled, so the navigation is stopped, the driver is closed, the privacy context is reset
            // and all the running tasks should be redo
            throw NavigateTaskCancellationException("Task #${task.batchTaskId}/${task.batchId} is canceled | ${task.url}")
        }
    }
}
