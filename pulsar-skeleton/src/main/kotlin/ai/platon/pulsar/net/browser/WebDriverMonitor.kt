package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.DateTimeUtil
import ai.platon.pulsar.common.RuntimeUtils
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.PulsarConstants
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.proxy.InternalProxyServer
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

/**
 * TODO: merge all monitor threads
 * */
class WebDriverMonitor(
        val webDriverPool: WebDriverPool,
        val proxyPool: ProxyPool,
        val internalProxyServer: InternalProxyServer,
        private val conf: ImmutableConfig
): AutoCloseable {
    private val log = LoggerFactory.getLogger(WebDriverMonitor::class.java)

    private var monitorThread = Thread(this::update)
    private var round = 0
    private val isIdle get() = webDriverPool.isIdle
    private var idleCount = 0
    private val closed = AtomicBoolean()
    val isClosed get() = closed.get()

    fun start() {
        // TODO: move to a better place to start proxy relative threads
        proxyPool.updateProxies(asap = true)
        if (internalProxyServer.isEnabled) {
            internalProxyServer.start()
        }

        monitorThread.isDaemon = true
        monitorThread.start()
    }

    override fun close() {
        if (closed.getAndSet(true)) {
            return
        }

        report()

        if (internalProxyServer.isEnabled) {
            internalProxyServer.use { it.close() }
        }

        monitorThread.interrupt()
        monitorThread.join()

        log.info("WDM is closed after $round rounds")
    }

    private fun update() {
        while (!isClosed && !Thread.currentThread().isInterrupted) {
            updateAndReport(round++)

            try {
                TimeUnit.SECONDS.sleep(1)
            } catch (e: InterruptedException) {
                log.warn("WDM loop interrupted after $round rounds")
                Thread.currentThread().interrupt()
            }
        }
    }

    private fun updateAndReport(tick: Int) {
        idleCount = if (isIdle) { 1 + idleCount } else 0
        val interval = min(20 + idleCount / 10 * 10, 200) // generates 20, 30, 40, 50, ..., 200
        if (tick % interval == 0) {
            report()
        }

        // check to close web drivers every minute
        if (tick % 60 == 0) {
            if (isIdle && !webDriverPool.isAllEmpty) {
                log.info("The web driver pool is idle, closing all drivers ...")
                webDriverPool.closeAll(maxWait = 0)
            }
        }

        // check local file command
        if (tick % 20 == 0) {
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

    private fun report() {
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
        if (ipsReport.isNotBlank()) {
            log.info(ipsReport)
        }
    }
}
