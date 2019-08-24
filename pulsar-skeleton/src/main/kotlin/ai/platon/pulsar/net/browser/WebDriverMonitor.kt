package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.RuntimeUtils
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.PulsarConstants
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.proxy.InternalProxyServer
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

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
    private val closed = AtomicBoolean()
    val isClosed get() = closed.get()

    fun start() {
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
    }

    private fun update() {
        var tick = 0
        while (!isClosed && !Thread.currentThread().isInterrupted) {
            updateAndReport(tick++)

            try {
                TimeUnit.SECONDS.sleep(1)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    private fun report() {
        val p = webDriverPool
        log.info("Web driver pool status - free: {}, working: {}, total: {}, crashed: {}, retired: {}, quit: {}, pageViews: {}",
                p.freeSize, p.workingSize, p.aliveSize,
                WebDriverPool.numCrashed, WebDriverPool.numRetired, WebDriverPool.numQuit,
                WebDriverPool.pageViews
        )
    }

    private fun updateAndReport(tick: Int) {
        if (tick % 10 == 0) {
            report()

            if (webDriverPool.isIdle) {
                log.info("The web driver pool is idle, closing all drivers ...")
                webDriverPool.closeAll(maxWait = 0)
            }
        }

        if (!proxyPool.isIdle) {
            when {
                proxyPool.numFreeProxies == 0 -> proxyPool.updateProxies(asap = true)
                proxyPool.numFreeProxies < 3 -> proxyPool.updateProxies()
            }
        }

        if (tick % 20 == 0) {
            if (RuntimeUtils.hasLocalFileCommand(PulsarConstants.CMD_PROXY_POOL_DUMP)) {
                proxyPool.dump()
            }
        }
    }
}
