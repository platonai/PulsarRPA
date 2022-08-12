package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.browser.common.EmulateSettings
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.DEFAULT_CHARSET_PATTERN
import ai.platon.pulsar.common.SYSTEM_AVAILABLE_CHARSET_PATTERN
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.protocol.browser.driver.WebDriverSettings
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

abstract class BrowserEmulatorBase(
    val driverSettings: WebDriverSettings,
    /**
     * The event handler to handle page content
     * */
    val browserEmulatorEventHandler: BrowserEmulatorEventHandler,
    val immutableConfig: ImmutableConfig
): Parameterized, AutoCloseable {
    private val log = LoggerFactory.getLogger(BrowserEmulatorBase::class.java)!!
    private val tracer = log.takeIf { it.isTraceEnabled }
    val supportAllCharsets get() = immutableConfig.getBoolean(CapabilityTypes.PARSE_SUPPORT_ALL_CHARSETS, true)
    val charsetPattern = if (supportAllCharsets) SYSTEM_AVAILABLE_CHARSET_PATTERN else DEFAULT_CHARSET_PATTERN
    val closed = AtomicBoolean(false)
    val isActive get() = !closed.get() && AppContext.isActive
    val meterNavigates by lazy { AppMetrics.reg.meter(this,"navigates") }
    val counterRequests by lazy { AppMetrics.reg.counter(this,"requests") }
    val counterJsEvaluates by lazy { AppMetrics.reg.counter(this,"jsEvaluates") }
    val counterJsWaits by lazy { AppMetrics.reg.counter(this,"jsWaits") }
    val counterCancels by lazy { AppMetrics.reg.counter(this,"cancels") }

    override fun getParams(): Params {
        val emulateSettings = EmulateSettings(immutableConfig)
        return Params.of(
                "pageLoadTimeout", emulateSettings.pageLoadTimeout,
                "scriptTimeout", emulateSettings.scriptTimeout,
                "scrollDownCount", emulateSettings.scrollCount,
                "scrollInterval", emulateSettings.scrollInterval,
                "jsInvadingEnabled", driverSettings.jsInvadingEnabled
        )
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {

        }
    }

    @Throws(NavigateTaskCancellationException::class)
    protected fun checkState() {
        if (!isActive) {
            throw NavigateTaskCancellationException("Emulator is closed")
        }
    }

    /**
     * Check task state
     * every direct or indirect IO operation is a checkpoint for the context reset event
     * */
    @Throws(NavigateTaskCancellationException::class)
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
    @Throws(NavigateTaskCancellationException::class)
    protected fun checkState(task: FetchTask) {
        checkState()

        if (task.isCanceled) {
            // the task is canceled, so the navigation is stopped, the driver is closed, the privacy context is reset
            // and all the running tasks should be redo
            throw NavigateTaskCancellationException("Task #${task.batchTaskId}/${task.batchId} is canceled | ${task.url}")
        }
    }
}
