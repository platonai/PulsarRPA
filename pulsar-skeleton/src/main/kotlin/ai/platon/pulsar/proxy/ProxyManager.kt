package ai.platon.pulsar.proxy

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.AppConstants.*
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyException
import ai.platon.pulsar.common.proxy.ProxyPool
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ProxyManager(
        val proxyPool: ProxyPool,
        private val metricsSystem: MetricsSystem,
        private val conf: ImmutableConfig
): AutoCloseable {

    enum class Availability {
        OK, IDLE, NO_PROXY, TEST_IP, WILL_DISCONNECT, WILL_EXPIRE, TEMPORARY_LOST, GONE;

        val isOK get() = this == OK
        val isNotOK get() = !isOK
        val isIdle get() = this == IDLE
    }

    private val log = LoggerFactory.getLogger(ProxyManager::class.java)

    private val connector = ProxyConnector(metricsSystem, conf)
    private val watcherThread = Thread(this::startWatcher)
    private val watcherStarted = AtomicBoolean()
    private val testerThread = Thread(this::startTester)
    private val testerStarted = AtomicBoolean()
    private val threadJoinTimeout = Duration.ofSeconds(30)

    private var idleTimeout = conf.getDuration(PROXY_INTERNAL_SERVER_IDLE_TIMEOUT, Duration.ofMinutes(5))
    private var idleCount = 0

    private val numRunningTasks = AtomicInteger()
    private var numFailedPages = 0
    private var numFailedTests = 0

    private var lastActiveTime = Instant.now()
    private var idleTime = Duration.ZERO
    private val closed = AtomicBoolean()
    private val isClosed get() = closed.get()

    val isEnabled get() = ProxyPool.isProxyEnabled() && conf.getBoolean(PROXY_ENABLE_FORWARD_SERVER, true)
    val isDisabled get() = !isEnabled
    val enableTester = true
    val port get() = connector.port
    var report: String = ""
        private set
    var verbose = false
    var autoRefresh = true

    val isWatcherStarted get() = watcherStarted.get()
    val currentProxyEntry: ProxyEntry? get() = connector.proxyEntry.get()

    @Synchronized
    fun start() {
        if (isDisabled) {
            log.warn("Proxy manager is disabled")
            return
        }

        if (watcherStarted.compareAndSet(false, true)) {
            watcherThread.isDaemon = true
            watcherThread.name = "pWatcher"
            watcherThread.start()
        }

        if (enableTester && testerStarted.compareAndSet(false, true)) {
            testerThread.isDaemon = true
            testerThread.name = "pTester"
            testerThread.start()
        }
    }

    /**
     * Run the task despite the proxy manager is disabled, it it's disabled, call the innovation directly
     * */
    fun <R> runAnyway(task: () -> R): R {
        return if (isDisabled) {
            task()
        } else {
            run(task)
        }
    }

    /**
     * Run the task in the proxy manager
     * */
    fun <R> run(task: () -> R): R {
        if (isClosed || isDisabled) {
            throw ProxyException("Proxy manager is " + if (isClosed) "closed" else "disabled")
        }

        idleTime = Duration.ZERO

        if (!ensureOnline()) {
            throw ProxyException("Failed to wait for a online proxy")
        }

        return try {
            numRunningTasks.incrementAndGet()
            task()
        } catch (e: Exception) {
            throw e
        } finally {
            lastActiveTime = Instant.now()
            numRunningTasks.decrementAndGet()
        }
    }

    fun ensureOnline(): Boolean {
        if (isDisabled || isClosed) {
            return false
        }

        return connector.ensureOnline()
    }

    fun changeProxyIfOnline(excludedProxy: ProxyEntry) {
        if (isDisabled || isClosed) {
            return
        }

        if (!ensureOnline()) {
            return
        }

        if (excludedProxy == this.currentProxyEntry) {
            excludedProxy.retire()
        }
    }

    private fun connectNext() {
        if (!isEnabled || isClosed) {
            return
        }

        connector.disconnect()?.let {
            proxyPool.retire(it)
            metricsSystem.reportRetiredProxies(it.toString())
        }
        proxyPool.poll()?.let { connector.connect(it) }
    }

    override fun close() {
        if (isEnabled && closed.compareAndSet(false, true)) {
            log.info("Closing proxy manager ... | {}", report)

            try {
                if (testerStarted.get()) {
                    testerThread.interrupt()
                    testerThread.join(threadJoinTimeout.toMillis())
                }

                if (watcherStarted.get()) {
                    watcherThread.interrupt()
                    watcherThread.join(threadJoinTimeout.toMillis())
                }

                connector.use { it.close() }
            } catch (e: Throwable) {
                log.error("Unexpected exception is caught when closing proxy manager", e)
            }
        }
    }

    private fun watch(tick: Int) {
        // watch every 5 seconds
        if (tick % 5 != 0) {
            return
        }

        val proxy = currentProxyEntry
        val avail = checkAvailability()
        when {
            avail == Availability.WILL_DISCONNECT -> {
                connector.disconnect()
            }
            avail == Availability.IDLE -> {
                log.info("Proxy manager is idle, disconnect online proxy")
                connector.disconnect()
                proxyPool.clear()
            }
            avail.isNotOK -> {
                connectNext()
            }
            else -> {}
        }

        idleCount = if (avail.isIdle) idleCount++ else 0

        if (proxy != null && tick % 20 == 0) {
            val proxyUpdated = when {
                numFailedPages != proxy.numFailedPages.get() -> true
                numFailedTests != proxy.numFailedTests.get() -> true
                else -> false
            }

            // something has been changed, we must report it
            if (proxyUpdated) {
                numFailedPages = proxy.numFailedPages.get()
                numFailedTests = proxy.numFailedTests.get()

                report = formatReport(avail.isIdle, avail, proxy)
                if (verbose) {
                    log.info(report)
                }
            }
        }
    }

    private fun checkAvailability(): Availability {
        val proxy = currentProxyEntry
        return when {
            proxy == null -> Availability.NO_PROXY
            willDisconnectOnCommand() -> Availability.WILL_DISCONNECT
            isIdle() -> Availability.IDLE
            !autoRefresh -> Availability.OK
            proxy.isTestIp -> Availability.TEST_IP
            proxy.isGone -> Availability.GONE
            proxy.willExpireAfter(Duration.ofMinutes(1)) -> Availability.WILL_EXPIRE
            else -> Availability.OK
        }
    }

    private fun willDisconnectOnCommand(): Boolean {
        if (RuntimeUtils.hasLocalFileCommand(CMD_INTERNAL_PROXY_SERVER_DISCONNECT, Duration.ofSeconds(30))) {
            log.info("Find fcmd $CMD_INTERNAL_PROXY_SERVER_DISCONNECT, disconnect the online proxy")
            return true
        }

        return false
    }

    /**
     * Check if the system is idle, we will disconnect the online proxy and clear the proxy pool is it's idle
     * */
    private fun isIdle(): Boolean {
        var isIdle = false
        if (numRunningTasks.get() == 0) {
            idleTime = Duration.between(lastActiveTime, Instant.now())
            if (idleTime > idleTimeout) {
                isIdle = true
            } else if (RuntimeUtils.hasLocalFileCommand(CMD_INTERNAL_PROXY_SERVER_FORCE_IDLE, Duration.ofSeconds(15))) {
                isIdle = true
            }
        }

        return isIdle
    }

    private fun startWatcher() {
        var tick = 0
        while (!isClosed && tick++ < Int.MAX_VALUE && !Thread.currentThread().isInterrupted) {
            try {
                watch(tick)
            } catch (e: Throwable) {
                log.error("Unexpected error", e)
            }

            try {
                TimeUnit.SECONDS.sleep(1)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        log.info("Quit proxy watcher loop after {} rounds", tick)
    }

    private fun startTester() {
        var tick = 0
        var seconds = 1L
        while (!isClosed && tick++ < Int.MAX_VALUE && !Thread.currentThread().isInterrupted) {
            val proxy = currentProxyEntry

            when {
                proxy == null -> seconds += 10
                connector.isOnline -> seconds = if (proxy.test()) 10 + seconds else 1
                else -> seconds += 10
            }

            if (seconds < 1) seconds = 1
            if (seconds > 60) seconds = 60

            try {
                TimeUnit.SECONDS.sleep(seconds)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        log.info("Quit proxy tester loop after {} rounds", tick)
    }

    private fun formatReport(isIdle: Boolean, avail: Availability, lastProxy: ProxyEntry? = null): String {
        if (isIdle) {
            return "Proxy is idle for $idleTime | $proxyPool"
        }

        if (lastProxy == null) {
            return "Proxy <none> is serving $numRunningTasks tasks | $proxyPool"
        }

        return "Proxy <${lastProxy.display}> is serving $numRunningTasks tasks ($avail)" +
                " | ${lastProxy.metadata} | $proxyPool"
    }
}

fun main() {
    val conf = ImmutableConfig()
    val proxyManager = ProxyManager(ProxyPool(conf), MetricsSystem(conf), conf)
    proxyManager.use {
        it.start()
        Thread.sleep(10000)
    }
}
