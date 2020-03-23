package ai.platon.pulsar

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyManager
import ai.platon.pulsar.net.browser.WebDriverManager
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * TODO: metrics system is a better choice
 * */
class AppMonitor(
        val driverManager: WebDriverManager,
        val proxyManager: ProxyManager,
        private val conf: ImmutableConfig
): AutoCloseable {
    private val log = LoggerFactory.getLogger(AppMonitor::class.java)
    private var lastIPSReport = ""

    private val driverPool = driverManager.driverPool
    private var monitorThread = Thread(this::update)
    private val loopStarted = AtomicBoolean()
    private val isIdle get() = driverManager.isIdle
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
            if (isIdle && !driverPool.isAllEmpty) {
                log.info("The web driver pool is idle, closing all drivers ...")
                driverPool.closeAll(incognito = true)
            }
        }
    }

    private fun report(round: Int) {
        val p = driverPool
        val idleTime = DateTimes.readableDuration(driverManager.idleTime)
        log.info("WDM - {}free: {}, working: {}, total: {}, crashed: {}, retired: {}, quit: {}, pageViews: {} rounds: {}",
                if (isIdle) "[Idle($idleTime)] " else "",
                p.numFree, p.numWorking, p.numActive,
                driverPool.numCrashed, driverPool.numRetired, driverPool.numQuit,
                driverManager.pageViews,
                round
        )

        val ipsReport = proxyManager.report
        if (ipsReport.isNotBlank() && ipsReport != lastIPSReport) {
            log.info(ipsReport)
        }
    }
}
