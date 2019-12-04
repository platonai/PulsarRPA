package ai.platon.pulsar

import ai.platon.pulsar.common.DateTimeUtil
import ai.platon.pulsar.common.RuntimeUtils
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.PulsarConstants
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.net.browser.WebDriverPool
import ai.platon.pulsar.proxy.InternalProxyServer
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * TODO: merge all monitor threads
 * */
class AppMonitor(
        val webDriverPool: WebDriverPool,
        val proxyPool: ProxyPool,
        val internalProxyServer: InternalProxyServer,
        private val conf: ImmutableConfig
): AutoCloseable {
    private val log = LoggerFactory.getLogger(AppMonitor::class.java)
    private var lastIPSReport = ""

    private val env = PulsarEnv.getOrCreate()
    private var monitorThread = Thread(this::update)
    private val loopStarted = AtomicBoolean()
    private val isIdle get() = webDriverPool.isIdle
    private val closed = AtomicBoolean()
    private val isClosed get() = closed.get()

    fun start() {
        if (loopStarted.compareAndSet(false, true)) {
            monitorThread.isDaemon = true
            monitorThread.start()
        }
    }

    override fun close() {
        if (closed.getAndSet(true)) {
            return
        }

        if (internalProxyServer.isEnabled) {
            internalProxyServer.use { it.close() }
        }

        monitorThread.interrupt()
        monitorThread.join()
    }

    private fun update() {
        var tick = 0
        while (!isClosed && !Thread.currentThread().isInterrupted) {
            updateAndReport(tick++)

            try {
                TimeUnit.SECONDS.sleep(1)
            } catch (e: InterruptedException) {
                log.warn("WDM loop interrupted after $tick rounds")
                Thread.currentThread().interrupt()
            }
        }

        // report for the last time
        report(tick)

        if (isClosed) {
            log.info("Quit WDM loop on close after {} rounds", tick)
        } else {
            log.error("Quit WDM loop abnormally after {} rounds", tick)
        }
    }

    private fun updateAndReport(tick: Int) {
        val interval = if (isIdle) 300 else 30
        if (tick % interval == 0) {
            report(tick)
        }

        // check to close web drivers every minute
        if (tick % 60 == 0) {
            if (isIdle && !webDriverPool.isAllEmpty) {
                log.info("The web driver pool is idle, closing all drivers ...")
                webDriverPool.closeAll(maxWait = 0)
            }
        }

        if (env.useProxy) {
            monitorProxySystem(tick)
        }
    }

    private fun monitorProxySystem(tick: Int) {
        if (tick % 20 == 0) {
            // proxy system can be started and shutdown at runtime
            if (!internalProxyServer.isLoopStarted) {
                if (internalProxyServer.isEnabled) {
                    proxyPool.updateProxies(asap = true)
                    internalProxyServer.start()
                }
            }

            // check local file command
            if (RuntimeUtils.hasLocalFileCommand(PulsarConstants.CMD_PROXY_POOL_DUMP)) {
                proxyPool.dump()
            }
        }

        if (!proxyPool.isIdle) {
            when {
                proxyPool.numFreeProxies == 0 -> proxyPool.updateProxies(asap = true)
                proxyPool.numFreeProxies < 3 -> proxyPool.updateProxies()
            }
        }
    }

    private fun report(round: Int) {
        val p = webDriverPool
        val idleTime = DateTimeUtil.readableDuration(p.idleTime)
        log.info("WDM - {}free: {}, working: {}, total: {}, crashed: {}, retired: {}, quit: {}, pageViews: {} rounds: {}",
                if (isIdle) "[Idle($idleTime)] " else "",
                p.freeSize, p.workingSize, p.aliveSize,
                WebDriverPool.numCrashed, WebDriverPool.numRetired, WebDriverPool.numQuit,
                WebDriverPool.pageViews,
                round
        )

        val ipsReport = internalProxyServer.report
        if (ipsReport.isNotBlank() && ipsReport != lastIPSReport) {
            log.info(ipsReport)
        }
    }
}
